package com.github.itube

/**
 * Pure deterministic filters that identify real music videos and music mixes/playlists
 * inside the user's YouTube feeds without introducing hardcoded or sample data.
 */
object MusicFeedFilter {

    private val TITLE_PHRASES = listOf(
        "official music video",
        "music video",
        "official audio",
        "official visualizer",
        "official lyric video",
        "lyric video",
        "lyrics video",
        "audio track",
        "full album",
        "full song",
        "tiny desk",
        "live session",
        "live performance",
        "acoustic session",
        "dj mix",
        "cover song",
        "jukebox",
        "visualizer",
        "unplugged",
        "lo-fi",
        "lofi",
        "mashup",
        "m/v",
    )

    private val TITLE_TAG_REGEX = Regex(
        """[\(\[]\s*(?:lyrics?|audio|official(?:\s+(?:music\s+)?video|\s+audio)?|mv|m/v|live|remix|acoustic|visualizer)\s*[\)\]]""",
        RegexOption.IGNORE_CASE,
    )

    private val TITLE_WORD_REGEX = Regex(
        """\b(?:song|songs|music|mv|ost|album|remix|acoustic|karaoke|instrumental|soundtrack|synthwave)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val CHANNEL_PHRASES = listOf(
        "vevo",
        " - topic",
    )

    private val CHANNEL_WORD_REGEX = Regex(
        """\b(?:music|records|recordings|audio|sounds)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val COLLECTION_WORD_REGEX = Regex(
        """\b(?:mix|music|song|songs|hits|album|albums|lofi|lo-fi|chill|beats|jukebox|acoustic|soundtrack|playlist|pop|rock|rap|hip\s*hop|jazz|classical)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun isMusicVideo(video: VideoItem): Boolean {
        if (video.isShortVideo) return false
        val title = video.title.trim()
        if (title.isEmpty()) return false
        val titleLower = title.lowercase()

        if (TITLE_PHRASES.any { titleLower.contains(it) }) return true
        if (TITLE_TAG_REGEX.containsMatchIn(title)) return true
        if (TITLE_WORD_REGEX.containsMatchIn(title)) return true

        val channel = video.channel.trim()
        if (channel.isNotEmpty()) {
            val channelLower = channel.lowercase()
            if (CHANNEL_PHRASES.any { channelLower.contains(it) }) return true
            if (CHANNEL_WORD_REGEX.containsMatchIn(channel)) return true
        }
        return false
    }

    fun isMusicCollection(collection: PublicCollection): Boolean {
        if (collection.isAlbum) return true
        val listId = collection.url.substringAfter("list=", "")
        if (listId.startsWith("RD", ignoreCase = true)) return true
        val text = "${collection.title} ${collection.subtitle}"
        return COLLECTION_WORD_REGEX.containsMatchIn(text)
    }

    fun filterMusicVideos(videos: List<VideoItem>): List<VideoItem> {
        if (videos.isEmpty()) return emptyList()
        val seen = HashSet<String>(videos.size)
        return videos.filter { video ->
            seen.add(video.id) && isMusicVideo(video)
        }
    }

    fun filterMusicCollections(collections: List<PublicCollection>): List<PublicCollection> {
        if (collections.isEmpty()) return emptyList()
        val seen = HashSet<String>(collections.size)
        return collections.filter { collection ->
            seen.add(collection.url) && isMusicCollection(collection)
        }
    }
}
