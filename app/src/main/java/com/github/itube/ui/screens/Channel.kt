package com.github.itube

import android.content.Intent
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.ContentCopy
import com.github.itube.ui.components.MaterialLoader
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.itube.ui.tv.LocalIsTv
import com.github.itube.ui.tv.tvContentEntry
import com.github.itube.ui.tv.tvFocusRing
import com.github.itube.ui.tv.tvGridColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page

/** Opens a channel page from anywhere (player channel row, card avatars). Provided by the shell. */
val LocalChannelOpener = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Opens a public playlist or album page from anywhere in the app. */
val LocalCollectionOpener = staticCompositionLocalOf<(PublicCollection) -> Unit> { {} }

/** Opens the album or playlist action sheet. */
val LocalCollectionMenu = staticCompositionLocalOf<(PublicCollection) -> Unit> { {} }

@Stable
class ChannelState(val url: String, private val scope: CoroutineScope) {
    var details by mutableStateOf<ChannelDetails?>(null)
        private set
    var videos by mutableStateOf<List<VideoItem>>(emptyList())
        private set
    var shorts by mutableStateOf<List<VideoItem>>(emptyList())
        private set
    var releases by mutableStateOf<List<PublicCollection>>(emptyList())
        private set
    var playlists by mutableStateOf<List<PublicCollection>>(emptyList())
        private set
    var sectionsLoading by mutableStateOf(false)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var subscribed by mutableStateOf<Boolean?>(null)
        private set
    private var next: String? = null
    val hasMore get() = next != null

    init { load() }

    fun load() {
        loading = true
        error = null
        scope.launch {
            runCatching { YouTube.channel(url) }
                .onSuccess { d ->
                    details = d
                    if (Account.signedIn) launch { subscribed = runCatching { InnerTube.channelSubscribed(d.id, d.name, d.avatar) }.getOrNull() }
                    runCatching { YouTube.channelVideos(d, null) }
                        .onSuccess { p -> videos = p.videos; next = p.next }
                        .onFailure { error = YouTube.friendlyError(it) }
                    sectionsLoading = true
                    launch {
                        runCatching { YouTube.channelShorts(d) }.onSuccess { shorts = it }
                        runCatching { YouTube.channelCollections(d, albums = true) }.onSuccess { releases = it }
                        runCatching { YouTube.channelCollections(d, albums = false) }.onSuccess { playlists = it }
                        sectionsLoading = false
                    }
                }
                .onFailure { error = YouTube.friendlyError(it) }
            loading = false
        }
    }

    fun loadMore() {
        val d = details ?: return
        val page = next ?: return
        if (loading) return
        loading = true
        scope.launch {
            runCatching { YouTube.channelVideos(d, page) }
                .onSuccess { p ->
                    val seen = videos.mapTo(HashSet()) { it.id }
                    videos = videos + p.videos.filter { seen.add(it.id) }
                    next = p.next
                }
            loading = false
        }
    }

    fun toggleSubscribe(onError: (String) -> Unit) {
        val id = details?.id ?: return
        val before = subscribed ?: return
        subscribed = !before
        scope.launch {
            runCatching { InnerTube.setSubscribed(id, !before) }
                .onSuccess { if (before) Bells.forget(id) }
                .onFailure { subscribed = before; onError("Couldn't update your subscription.") }
        }
    }
}

@Composable
fun ChannelScreen(
    state: ChannelState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit,
    onOpenCollection: (PublicCollection) -> Unit,
    onMessage: (String) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val isTv = LocalIsTv.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val list = rememberLazyListState()
    var section by remember(state.url) { mutableStateOf(ChannelSection.Videos) }
    val nearEnd by remember {
        derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 4 }
    }
    LaunchedEffect(nearEnd, state.videos.size) { if (nearEnd && state.videos.isNotEmpty()) state.loadMore() }
    Box(Modifier.fillMaxSize().background(c.surface)) {
    CollapsingHeaderLayout(
        header = {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.tvContentEntry("overlay:channel", priority = 5), onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                state.details?.name.orEmpty(),
                fontFamily = if (isTv) FontFamily.SansSerif else RoundedHeavy,
                fontWeight = if (isTv) FontWeight.Bold else FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = if (isTv) (-0.3).sp else (-0.6).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        },
    ) { top ->
        val d = state.details
        when {
            d == null && state.error != null -> Box(Modifier.padding(top = top)) { EmptyState(
                Icons.Rounded.CloudOff, "Couldn't load this channel", state.error.orEmpty(),
                action = {
                    PillButton(
                        "Try again",
                        state::load,
                        modifier = Modifier.tvContentEntry("overlay:channel", centeredEmptyState = true, priority = 0),
                    )
                },
            ) }
            d == null -> Box(Modifier.fillMaxWidth().padding(top = top + 60.dp), Alignment.Center) {
                MaterialLoader(color = c.primary, size = 36.dp)
            }
            else -> LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                item(key = "header") {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        if (d.banner != null) {
                            Thumbnail(d.banner, Modifier.fillMaxWidth().aspectRatio(5.6f), radius = if (isTv) 12 else 22)
                            Spacer(Modifier.height(16.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChannelAvatar(d.avatar, d.name, size = 76)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    d.name,
                                    fontFamily = if (isTv) FontFamily.SansSerif else RoundedHeavy,
                                    fontWeight = if (isTv) FontWeight.Bold else FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (d.subscribers > 0) Text("${formatCount(d.subscribers)} subscribers", color = c.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        state.subscribed?.let { subscribed ->
                            Spacer(Modifier.height(14.dp))
                            SubscribeWithBell(
                                subscribed,
                                channel = BellChannel(d.id, d.name, d.avatar),
                                onSubscribe = { state.toggleSubscribe(onMessage) },
                                onMessage = onMessage,
                                modifier = Modifier.fillMaxWidth(),
                                wide = true,
                            )
                        }
                        if (d.description.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(d.description, color = c.onSurfaceVariant, fontSize = 13.5.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                item(key = "tabs") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isTv) 10.dp else 8.dp),
                    ) {
                        ChannelSection.entries.forEachIndexed { idx, tab ->
                            val active = section == tab
                            if (isTv) {
                                var tabFocused by remember { mutableStateOf(false) }
                                val tabShape = RoundedCornerShape(8.dp)
                                val bg = when {
                                    tabFocused -> Color.White
                                    active -> Color(0xFFF1F1F1)
                                    else -> Color(0xFF272727)
                                }
                                val ink = if (tabFocused || active) Color(0xFF0F0F0F) else Color(0xFFF1F1F1)
                                Text(
                                    tab.label,
                                    color = ink,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (tabFocused || active) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .then(if (idx == 0) Modifier.tvContentEntry("overlay:channel", priority = 0) else Modifier)
                                        .onFocusChanged { tabFocused = it.hasFocus || it.isFocused }
                                        .pressable(focusShape = tabShape, focusScale = 1.04f, showFocusBorder = false) { section = tab }
                                        .clip(tabShape)
                                        .background(bg)
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                )
                            } else {
                                Text(
                                    tab.label,
                                    color = if (active) c.onPrimary else c.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .then(if (idx == 0) Modifier.tvContentEntry("overlay:channel", priority = 0) else Modifier)
                                        .pressable(focusShape = CircleShape, focusScale = 1.04f) { section = tab }
                                        .clip(CircleShape)
                                        .background(if (active) c.primary else c.surfaceContainerHigh)
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }
                }
                when (section) {
                    ChannelSection.Videos -> {
                        item(key = "videosTitle") { SectionTitle("Videos") }
                        val videoCols = if (isTv) tvGridColumns(screenWidth, 280.dp, 3) else 1
                        if (videoCols > 1) {
                            items(state.videos.chunked(videoCols), key = { "vrow-${it.first().id}" }) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    row.forEach { video ->
                                        Box(Modifier.weight(1f)) { VideoCard(video, onClick = { onOpen(video) }) }
                                    }
                                    repeat(videoCols - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        } else {
                            items(state.videos, key = { it.id }) { video -> VideoCard(video, onClick = { onOpen(video) }) }
                        }
                    }
                    ChannelSection.Shorts -> {
                        item(key = "shortsTitle") { SectionTitle("Shorts") }
                        if (state.shorts.isEmpty() && !state.sectionsLoading) {
                            item(key = "emptyShorts") {
                                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                    Text("No shorts found on this channel", color = c.onSurfaceVariant, fontSize = 14.sp)
                                }
                            }
                        } else {
                            val shortCols = if (isTv) tvGridColumns(screenWidth, 160.dp, 5) else 3
                            items(state.shorts.chunked(shortCols), key = { chunk -> chunk.first().id }) { rowShorts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowShorts.forEach { short ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ChannelShortCard(short = short, onClick = { onOpenShort(short) })
                                        }
                                    }
                                    repeat(shortCols - rowShorts.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    ChannelSection.Releases -> {
                        item(key = "releasesTitle") { SectionTitle("Releases") }
                        if (state.releases.isEmpty() && !state.sectionsLoading) {
                            item(key = "emptyReleases") {
                                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                    Text("No albums or releases found", color = c.onSurfaceVariant, fontSize = 14.sp)
                                }
                            }
                        } else {
                            val relCols = if (isTv) tvGridColumns(screenWidth, 210.dp, 4) else 2
                            items(state.releases.chunked(relCols), key = { chunk -> chunk.first().url }) { rowReleases ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowReleases.forEach { col ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ChannelCollectionCard(col = col, onClick = { onOpenCollection(col) })
                                        }
                                    }
                                    repeat(relCols - rowReleases.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    ChannelSection.Playlists -> {
                        item(key = "playlistsTitle") { SectionTitle("Playlists") }
                        if (state.playlists.isEmpty() && !state.sectionsLoading) {
                            item(key = "emptyPlaylists") {
                                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                    Text("No playlists found", color = c.onSurfaceVariant, fontSize = 14.sp)
                                }
                            }
                        } else {
                            val plCols = if (isTv) tvGridColumns(screenWidth, 210.dp, 4) else 2
                            items(state.playlists.chunked(plCols), key = { chunk -> chunk.first().url }) { rowPlaylists ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowPlaylists.forEach { col ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ChannelCollectionCard(col = col, onClick = { onOpenCollection(col) })
                                        }
                                    }
                                    repeat(plCols - rowPlaylists.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                if (section == ChannelSection.Videos && (state.loading || state.hasMore)) item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), Alignment.Center) {
                        MaterialLoader(color = c.primary, size = 32.dp)
                    }
                }
                if (section != ChannelSection.Videos && state.sectionsLoading) item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), Alignment.Center) { MaterialLoader(color = c.primary, size = 28.dp) }
                }
            }
        }
    }
    }
}

@Composable
private fun ChannelShortCard(short: VideoItem, onClick: () -> Unit) {
    val menu = LocalVideoMenu.current
    val isTv = LocalIsTv.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (isTv) 12.dp else 14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .onFocusChanged { focused = it.hasFocus || it.isFocused }
            .pressable(
                pressedScale = 0.98f,
                focusScale = 1.03f,
                focusShape = shape,
                showFocusBorder = !isTv,
                onLongClick = { menu(short) },
                onClick = onClick,
            )
            .clip(shape)
            .drawWithContent {
                drawContent()
                if (isTv && focused) {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(outline, Color.Black.copy(alpha = 0.78f), style = Stroke(4.2.dp.toPx()))
                    drawOutline(outline, Color.White, style = Stroke(2.6.dp.toPx()))
                }
            }
    ) {
        Thumbnail(
            url = short.shortThumbnail,
            modifier = Modifier.fillMaxSize(),
            radius = 0,
            fallbackUrl = short.thumbnail,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = short.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
            )
            if (short.views > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${formatCount(short.views)} views",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ChannelCollectionCard(col: PublicCollection, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val menu = LocalCollectionMenu.current
    val isTv = LocalIsTv.current
    var focused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(if (isTv) 12.dp else 16.dp)
    val thumbShape = RoundedCornerShape(if (isTv) 12.dp else 16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.hasFocus || it.isFocused }
            .pressable(
                pressedScale = 0.98f,
                focusScale = 1.03f,
                focusShape = cardShape,
                showFocusBorder = !isTv,
                onLongClick = { menu(col) },
                onClick = onClick,
            )
            .clip(cardShape)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(thumbShape)
                .drawWithContent {
                    drawContent()
                    if (isTv && focused) {
                        val outline = thumbShape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, Color.Black.copy(alpha = 0.78f), style = Stroke(4.2.dp.toPx()))
                        drawOutline(outline, Color.White, style = Stroke(2.6.dp.toPx()))
                    }
                }
        ) {
            Thumbnail(col.thumbnail, Modifier.fillMaxSize(), radius = 0)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(if (isTv) 4.dp else 8.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (col.isAlbum) "Album" else "Playlist",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = col.title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isTv && focused) Color.White else c.onSurface,
        )
        Text(
            text = col.subtitle,
            color = c.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class ChannelSection(val label: String) { Videos("Videos"), Shorts("Shorts"), Releases("Releases"), Playlists("Playlists") }

@Composable
fun PublicCollectionRow(collection: PublicCollection, onOpen: (PublicCollection) -> Unit) {
    val c = MaterialTheme.colorScheme
    val menu = LocalCollectionMenu.current
    val rowShape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
            .pressable(pressedScale = 0.985f, focusScale = 1.02f, focusShape = rowShape, onLongClick = { menu(collection) }, onClick = { onOpen(collection) })
            .clip(rowShape)
            .background(c.surfaceContainerLow)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) {
            Thumbnail(collection.thumbnail, Modifier.fillMaxSize(), radius = 0)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (collection.isAlbum) "Album" else "Playlist",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(collection.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(collection.subtitle, color = c.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Stable
class PublicCollectionState(val collection: PublicCollection, private val scope: CoroutineScope) {
    var details by mutableStateOf<PublicCollectionDetails?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { load() }
    fun load() {
        loading = true; error = null
        scope.launch {
            runCatching { YouTube.publicCollection(collection.url, collection.isAlbum) }
                .onSuccess { details = it }
                .onFailure { error = YouTube.friendlyError(it) }
            loading = false
        }
    }
}

/**
 * Premium Flow-style dedicated Album & Playlist Screen.
 * Features hero artwork with elevation, type badge, Play All, Shuffle, Share,
 * and clean numbered compact track rows that support queuing playback.
 */
@Composable
fun PublicCollectionScreen(
    state: PublicCollectionState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    onOpenPlaylist: ((List<VideoItem>, Int) -> Unit)? = null,
    onOpenChannel: ((String) -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val openCollectionMenu = LocalCollectionMenu.current
    Box(Modifier.fillMaxSize().background(c.surface)) {
        CollapsingHeaderLayout(header = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.tvContentEntry("overlay:collection", priority = 5), onBack)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        state.collection.title,
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { openCollectionMenu(state.collection) },
                    modifier = Modifier.tvFocusRing(CircleShape),
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Collection options", tint = c.onSurface)
                }
            }
        }) { top ->
            val isTv = LocalIsTv.current
            when {
                state.loading && state.details == null -> Box(Modifier.fillMaxWidth().padding(top = top + 64.dp), Alignment.Center) { MaterialLoader(color = c.primary, size = 36.dp) }
                state.error != null -> Box(Modifier.padding(top = top)) {
                    EmptyState(
                        Icons.Rounded.CloudOff,
                        "Couldn't load this ${if (state.collection.isAlbum) "album" else "playlist"}",
                        state.error.orEmpty(),
                        action = {
                            PillButton(
                                "Try again",
                                state::load,
                                modifier = Modifier.tvContentEntry("overlay:collection", centeredEmptyState = true, priority = 0),
                            )
                        },
                    )
                }
                else -> {
                    val details = state.details ?: return@CollapsingHeaderLayout
                    LazyColumn(contentPadding = contentPadding.withTop(top)) {
                        // Hero Header Card
                        item(key = "hero") {
                            if (isTv) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(144.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                    ) {
                                        Thumbnail(details.collection.thumbnail, Modifier.fillMaxSize(), radius = 0)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(c.primaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (details.collection.isAlbum) "ALBUM" else "PLAYLIST",
                                                color = c.onPrimaryContainer,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp,
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            details.collection.title,
                                            fontFamily = RoundedHeavy,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = c.onSurface,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            details.collection.subtitle,
                                            color = c.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            PillButton(
                                                "Play all",
                                                onClick = {
                                                    if (details.videos.isNotEmpty()) {
                                                        onOpenPlaylist?.invoke(details.videos, 0) ?: onOpen(details.videos.first())
                                                    }
                                                },
                                                icon = Icons.Rounded.PlayArrow,
                                                modifier = Modifier.tvContentEntry("overlay:collection", priority = 0),
                                            )
                                            CircleAction(
                                                icon = Icons.Rounded.Shuffle,
                                                contentDescription = "Shuffle",
                                                onClick = {
                                                    if (details.videos.isNotEmpty()) {
                                                        val shuffled = details.videos.shuffled()
                                                        onOpenPlaylist?.invoke(shuffled, 0) ?: onOpen(shuffled.first())
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            } else {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                ) {
                                    Thumbnail(details.collection.thumbnail, Modifier.fillMaxSize(), radius = 0)
                                }
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(c.primaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (details.collection.isAlbum) "ALBUM" else "PLAYLIST",
                                        color = c.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    details.collection.title,
                                    fontFamily = RoundedHeavy,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = c.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    details.collection.subtitle,
                                    color = c.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.height(16.dp))

                                // Action Buttons (Play All, Shuffle)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PillButton(
                                        "Play all",
                                        onClick = {
                                            if (details.videos.isNotEmpty()) {
                                                onOpenPlaylist?.invoke(details.videos, 0) ?: onOpen(details.videos.first())
                                            }
                                        },
                                        icon = Icons.Rounded.PlayArrow,
                                    )
                                    CircleAction(
                                        icon = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        onClick = {
                                            if (details.videos.isNotEmpty()) {
                                                val shuffled = details.videos.shuffled()
                                                onOpenPlaylist?.invoke(shuffled, 0) ?: onOpen(shuffled.first())
                                            }
                                        },
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            }
                        }

                        // Track List Header
                        item(key = "trackCountHeader") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${details.videos.size} tracks",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = c.onSurfaceVariant,
                                )
                            }
                        }

                        // Compact Track Rows
                        itemsIndexed(details.videos, key = { index, video -> "${video.id}_$index" }) { index, video ->
                            CompactCollectionTrackRow(
                                index = index + 1,
                                video = video,
                                onClick = {
                                    onOpenPlaylist?.invoke(details.videos, index) ?: onOpen(video)
                                },
                            )
                        }
                        if (details.recommendations.isNotEmpty()) {
                            item(key = "collectionRecommendationsTitle") {
                                SectionTitle("Recommended")
                            }
                            itemsIndexed(details.recommendations, key = { index, video -> "recommended-${video.id}-$index" }) { index, video ->
                                CompactCollectionTrackRow(
                                    index = index + 1,
                                    video = video,
                                    onClick = { onOpen(video) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCollectionTrackRow(
    index: Int,
    video: VideoItem,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val menu = LocalVideoMenu.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.99f, focusScale = 1.015f, focusShape = RoundedCornerShape(14.dp), onLongClick = { menu(video) }, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$index",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = c.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.width(28.dp),
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Thumbnail(video.thumbnail, Modifier.fillMaxSize(), radius = 0)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = c.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = video.channel,
                color = c.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (video.durationSec > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatDuration(video.durationSec * 1000),
                color = c.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionOptionsSheet(collection: PublicCollection, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val saved = SavedCollections.contains(collection.url)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Column(Modifier.fillMaxWidth().focusGroup()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))) {
                Thumbnail(collection.thumbnail, Modifier.fillMaxSize(), radius = 0)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(collection.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (collection.isAlbum) "Album" else "Playlist", color = c.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        CollectionAction(Icons.Rounded.Download, "Download ${if (collection.isAlbum) "album" else "playlist"}") {
            VideoDownloader.downloadCollection(context, collection)
            Toast.makeText(context, "Downloading ${if (collection.isAlbum) "album" else "playlist"}", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
        CollectionAction(if (saved) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd, if (saved) "Remove from Library" else "Save to Library") {
            val added = SavedCollections.toggle(context, collection)
            Toast.makeText(context, if (added) "Saved to Library" else "Removed from Library", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
        CollectionAction(Icons.Rounded.Share, "Share") {
            shareLink(context, collection.url, collection.title)
            onDismiss()
        }
        CollectionAction(Icons.Rounded.ContentCopy, "Copy link") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(collection.title, collection.url))
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
        Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CollectionAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().pressable(pressedScale = 0.98f, focusScale = 1.015f, focusShape = RoundedCornerShape(12.dp), onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.onSurface, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(title, color = c.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
