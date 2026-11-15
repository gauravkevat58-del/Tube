package com.github.itube

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class ChannelOriginStatus {
    INDIAN,
    FOREIGN,
    UNKNOWN,
}

/**
 * Resolves a YouTube creator's declared channel country strictly from YouTube channel About
 * metadata (InnerTube channel About responses and /about ytInitialData), never inferring origin
 * from video language, titles, or description keywords.
 */
object ChannelOriginResolver {
    private const val PREFS_NAME = "channel_origin_cache"
    private const val KNOWN_TTL_MS = 7L * 24L * 60L * 60L * 1000L
    private const val UNKNOWN_TTL_MS = 6L * 60L * 60L * 1000L
    private const val VIDEO_MAP_PREFIX = "vid:"
    private const val MAX_CONCURRENT_LOOKUPS = 8

    // InnerTube params token for the channel About engagement/browse panel.
    private const val CHANNEL_ABOUT_PARAMS = "EgVhYm91dPIGBAoCEgA="

    private data class CacheEntry(
        val status: ChannelOriginStatus,
        val updatedAtMs: Long,
    ) {
        fun isFresh(nowMs: Long): Boolean {
            val ttl = if (status == ChannelOriginStatus.UNKNOWN) UNKNOWN_TTL_MS else KNOWN_TTL_MS
            return nowMs - updatedAtMs < ttl
        }
    }

    private var prefs: SharedPreferences? = null
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    private val videoToChannelCache = ConcurrentHashMap<String, String>()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<ChannelOriginStatus>>()
    private val networkGate = Semaphore(MAX_CONCURRENT_LOOKUPS)

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun clearMemoryCache() {
        memoryCache.clear()
        videoToChannelCache.clear()
    }

    // ── Pure normalization & parsing (unit-testable on JVM) ─────────────────

    internal fun canonicalChannelKey(channelUrl: String?): String? {
        val raw = channelUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val cleaned = raw.substringBefore('?').substringBefore('#').trimEnd('/')
        if (cleaned.startsWith("UC", ignoreCase = false) && !cleaned.contains('/')) {
            return cleaned
        }
        val channelMatch = Regex("/channel/(UC[A-Za-z0-9_-]+)").find(cleaned)?.groupValues?.getOrNull(1)
        if (!channelMatch.isNullOrBlank()) return channelMatch
        val handleMatch = Regex("/(@[^/]+)").find(cleaned)?.groupValues?.getOrNull(1)
        if (!handleMatch.isNullOrBlank()) return handleMatch.lowercase(Locale.US)
        if (cleaned.startsWith("@")) return cleaned.lowercase(Locale.US)
        val legacyUser = Regex("/(?:user|c)/([^/]+)").find(cleaned)?.groupValues?.getOrNull(1)
        if (!legacyUser.isNullOrBlank()) return "legacy:${legacyUser.lowercase(Locale.US)}"
        return cleaned.lowercase(Locale.US)
    }

    internal fun normalizeAboutUrl(channelUrl: String?): String? {
        val raw = channelUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val base = when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("/") -> "https://www.youtube.com$raw"
            raw.startsWith("@") -> "https://www.youtube.com/$raw"
            raw.startsWith("UC") -> "https://www.youtube.com/channel/$raw"
            else -> return null
        }.substringBefore('?').substringBefore('#').trimEnd('/')
        return if (base.endsWith("/about")) base else "$base/about"
    }

    internal fun extractUcId(channelUrl: String?): String? {
        val key = canonicalChannelKey(channelUrl) ?: return null
        return key.takeIf { it.startsWith("UC") && !it.contains('/') && !it.contains(':') }
    }

    internal fun isIndiaCountry(rawCountry: String?): Boolean {
        val normalized = rawCountry
            ?.trim()
            ?.lowercase(Locale.US)
            ?.replace(Regex("[._-]+"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?: return false
        return normalized == "india" ||
            normalized == "in" ||
            normalized == "ind" ||
            normalized == "bharat" ||
            normalized == "republic of india"
    }

    internal fun classifyCountryString(rawCountry: String?): ChannelOriginStatus {
        val cleaned = rawCountry?.trim()
        if (cleaned.isNullOrEmpty()) return ChannelOriginStatus.UNKNOWN
        return if (isIndiaCountry(cleaned)) ChannelOriginStatus.INDIAN else ChannelOriginStatus.FOREIGN
    }

    /**
     * Reads only explicit channel About country fields from a YouTube JSON payload.
     * Deliberately ignores video titles, descriptions, languages, and general metadata.
     */
    internal fun extractCountryFromChannelJson(root: JSONObject): String? {
        var found: String? = null
        fun inspectObject(key: String?, obj: JSONObject) {
            if (found != null) return
            when (key) {
                "aboutChannelViewModel" -> {
                    obj.optString("country").trim().takeIf { it.isNotEmpty() }?.let { found = it; return }
                }
                "channelAboutFullMetadataRenderer" -> {
                    obj.text("country")?.trim()?.takeIf { it.isNotEmpty() }?.let { found = it; return }
                }
                "channelMetadataRenderer" -> {
                    obj.optString("country").trim().takeIf { it.isNotEmpty() }?.let { found = it; return }
                }
            }
            if (obj.has("aboutChannelViewModel")) {
                obj.optJSONObject("aboutChannelViewModel")?.optString("country")?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    found = it
                    return
                }
            }
            if (obj.has("channelAboutFullMetadataRenderer")) {
                obj.optJSONObject("channelAboutFullMetadataRenderer")?.text("country")?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    found = it
                    return
                }
            }
        }

        fun traverse(key: String?, obj: JSONObject) {
            if (found != null) return
            inspectObject(key, obj)
            if (found != null) return
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                when (val child = obj.opt(k)) {
                    is JSONObject -> traverse(k, child)
                    is JSONArray -> {
                        for (i in 0 until child.length()) {
                            val elem = child.opt(i)
                            if (elem is JSONObject) traverse(null, elem)
                            if (found != null) return
                        }
                    }
                }
                if (found != null) return
            }
        }

        traverse(null, root)
        return found
    }

    internal fun extractCountryFromAboutHtml(html: String): String? {
        if (html.isBlank()) return null
        val jsonMarker = "var ytInitialData = "
        val start = html.indexOf(jsonMarker)
        if (start >= 0) {
            val from = start + jsonMarker.length
            val end = html.indexOf(";</script>", from)
            if (end > from) {
                val payload = runCatching { JSONObject(html.substring(from, end)) }.getOrNull()
                if (payload != null) {
                    extractCountryFromChannelJson(payload)?.let { return it }
                }
            }
        }
        // Fallback inside embedded JSON snippets strictly scoped to aboutChannelViewModel / channelAboutFullMetadataRenderer.
        val viewModelMatch = Regex("\"aboutChannelViewModel\"\\s*:\\s*\\{[^{}]*?\"country\"\\s*:\\s*\"([^\"]+)\"")
            .find(html)?.groupValues?.getOrNull(1)?.trim()
        if (!viewModelMatch.isNullOrEmpty()) return unescapeBasicJson(viewModelMatch)

        val rendererMatch = Regex("\"channelAboutFullMetadataRenderer\"\\s*:\\s*\\{.*?\"country\"\\s*:\\s*\\{\\s*\"simpleText\"\\s*:\\s*\"([^\"]+)\"")
            .find(html)?.groupValues?.getOrNull(1)?.trim()
        if (!rendererMatch.isNullOrEmpty()) return unescapeBasicJson(rendererMatch)

        return null
    }

    internal fun extractChannelUrlFromOEmbedJson(jsonText: String): String? {
        if (jsonText.isBlank()) return null
        val obj = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
        return obj.optString("author_url").trim().takeIf { it.startsWith("http") || it.startsWith("/") }
    }

    private fun unescapeBasicJson(raw: String): String =
        raw.replace("\\/", "/").replace("\\u0026", "&").trim()

    /**
     * Pure item-level filter: keeps verified [ChannelOriginStatus.INDIAN] creators and strictly
     * excludes [ChannelOriginStatus.FOREIGN] creators. [ChannelOriginStatus.UNKNOWN] creators
     * are excluded conservatively unless [includeUnknownIfEmpty] is true and no verified Indian
     * creators were found.
     */
    internal fun filterVideosByOrigin(
        videos: List<VideoItem>,
        statusByVideoOrChannelKey: Map<String, ChannelOriginStatus>,
        includeUnknownIfEmpty: Boolean = false,
    ): List<VideoItem> {
        if (videos.isEmpty()) return emptyList()
        val indian = ArrayList<VideoItem>(videos.size)
        val unknown = ArrayList<VideoItem>()
        for (video in videos) {
            val key = canonicalChannelKey(video.channelUrl)
            val status = statusByVideoOrChannelKey[video.id]
                ?: (key?.let { statusByVideoOrChannelKey[it] })
                ?: ChannelOriginStatus.UNKNOWN
            when (status) {
                ChannelOriginStatus.INDIAN -> indian += video
                ChannelOriginStatus.UNKNOWN -> unknown += video
                ChannelOriginStatus.FOREIGN -> Unit
            }
        }
        return if (indian.isNotEmpty() || !includeUnknownIfEmpty) indian else unknown
    }

    // ── Suspend lookup & feed filtering API ─────────────────────────────────

    suspend fun resolveOrigin(channelUrl: String?, videoIdFallback: String? = null): ChannelOriginStatus =
        withContext(Dispatchers.IO) {
            val effectiveUrl = channelUrl?.takeIf { it.isNotBlank() }
                ?: videoIdFallback?.let { resolveChannelUrlForVideo(it) }
            val key = canonicalChannelKey(effectiveUrl) ?: return@withContext ChannelOriginStatus.UNKNOWN

            readCached(key)?.let { return@withContext it }

            val mine = CompletableDeferred<ChannelOriginStatus>()
            val existing = inFlight.putIfAbsent(key, mine)
            if (existing != null) {
                return@withContext existing.await()
            }

            try {
                val resolved = networkGate.withPermit {
                    fetchChannelOrigin(effectiveUrl, key)
                }
                writeCached(key, resolved)
                mine.complete(resolved)
                resolved
            } catch (t: Throwable) {
                mine.complete(ChannelOriginStatus.UNKNOWN)
                ChannelOriginStatus.UNKNOWN
            } finally {
                inFlight.remove(key, mine)
            }
        }

    suspend fun filterIndiaOriginVideos(
        videos: List<VideoItem>,
        maxResolve: Int = 36,
    ): List<VideoItem> = coroutineScope {
        if (videos.isEmpty()) return@coroutineScope emptyList()
        val sample = videos.take(maxResolve)
        val statuses = sample.map { video ->
            async(Dispatchers.IO) {
                video.id to resolveOrigin(video.channelUrl, video.id)
            }
        }.awaitAll().toMap()
        filterVideosByOrigin(sample, statuses, includeUnknownIfEmpty = false)
    }

    suspend fun filterIndiaOriginSearchResults(
        results: List<SearchResult>,
        maxResolve: Int = 32,
    ): List<SearchResult> = coroutineScope {
        if (results.isEmpty()) return@coroutineScope emptyList()
        val sample = results.take(maxResolve)
        val resolved = sample.map { result ->
            async(Dispatchers.IO) {
                val status = when {
                    result.video != null -> resolveOrigin(result.video.channelUrl, result.video.id)
                    result.kind == SearchResultKind.Channel -> resolveOrigin(result.url, null)
                    else -> ChannelOriginStatus.UNKNOWN
                }
                result to status
            }
        }.awaitAll()
        resolved.mapNotNull { (res, status) ->
            if (status == ChannelOriginStatus.INDIAN) res else null
        }
    }

    private fun fetchChannelOrigin(channelUrl: String?, key: String): ChannelOriginStatus {
        val ucId = extractUcId(channelUrl) ?: key.takeIf { it.startsWith("UC") && !it.contains(':') }
        if (ucId != null) {
            runCatching {
                val body = JSONObject()
                    .put("browseId", ucId)
                    .put("params", CHANNEL_ABOUT_PARAMS)
                val root = InnerTube.post("browse", body, anonymous = true)
                extractCountryFromChannelJson(root)?.let { return classifyCountryString(it) }
            }
        }

        val aboutUrl = normalizeAboutUrl(channelUrl)
        if (aboutUrl != null) {
            val html = runCatching { fetchHtml(aboutUrl) }.getOrNull()
            if (!html.isNullOrBlank()) {
                extractCountryFromAboutHtml(html)?.let { return classifyCountryString(it) }
            }
        }

        return ChannelOriginStatus.UNKNOWN
    }

    private fun resolveChannelUrlForVideo(videoId: String): String? {
        if (videoId.isBlank()) return null
        videoToChannelCache[videoId]?.let { return it }
        prefs?.getString("$VIDEO_MAP_PREFIX$videoId", null)?.let { cached ->
            videoToChannelCache[videoId] = cached
            return cached
        }
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val encoded = URLEncoder.encode(watchUrl, StandardCharsets.UTF_8.name())
        val req = Request.Builder()
            .url("https://www.youtube.com/oembed?url=$encoded&format=json")
            .header("User-Agent", Net.USER_AGENT)
            .build()
        val resolved = runCatching {
            Net.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                extractChannelUrlFromOEmbedJson(resp.body?.string().orEmpty())
            }
        }.getOrNull()
        if (!resolved.isNullOrBlank()) {
            videoToChannelCache[videoId] = resolved
            prefs?.edit()?.putString("$VIDEO_MAP_PREFIX$videoId", resolved)?.apply()
        }
        return resolved
    }

    private fun fetchHtml(url: String): String? {
        val sep = if ('?' in url) '&' else '?'
        val finalUrl = if ("hl=" in url) url else "$url${sep}hl=en"
        val req = Request.Builder()
            .url(finalUrl)
            .header("User-Agent", Net.USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return Net.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    }

    private fun readCached(key: String): ChannelOriginStatus? {
        val now = System.currentTimeMillis()
        memoryCache[key]?.let { entry ->
            if (entry.isFresh(now)) return entry.status
        }
        val stored = prefs?.getString("status:$key", null) ?: return null
        val parts = stored.split('|')
        if (parts.size != 2) return null
        val status = runCatching { ChannelOriginStatus.valueOf(parts[0]) }.getOrNull() ?: return null
        val timestamp = parts[1].toLongOrNull() ?: return null
        val entry = CacheEntry(status, timestamp)
        if (!entry.isFresh(now)) return null
        memoryCache[key] = entry
        return status
    }

    private fun writeCached(key: String, status: ChannelOriginStatus) {
        val now = System.currentTimeMillis()
        val entry = CacheEntry(status, now)
        memoryCache[key] = entry
        prefs?.edit()?.putString("status:$key", "${status.name}|$now")?.apply()
    }
}
