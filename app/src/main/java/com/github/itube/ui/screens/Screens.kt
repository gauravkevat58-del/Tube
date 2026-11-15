package com.github.itube

import androidx.compose.foundation.focusGroup
import androidx.compose.material3.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import com.github.itube.ui.components.MaterialLoader
import com.github.itube.ui.components.TrackBlurPopup
import com.github.itube.ui.components.popupContainerColor
import com.github.itube.ui.components.popupScrimColor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.github.itube.ui.tv.LocalIsTv
import com.github.itube.ui.tv.LocalTvFocusBridge
import com.github.itube.ui.tv.tvContentEntry
import com.github.itube.ui.tv.tvFocusRing
import com.github.itube.ui.tv.tvGridColumns
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.window.DialogProperties
import com.github.itube.ui.components.HomeScreenSkeleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeoutOrNull

// ── Home / Subscriptions / Library ─────────────────────────────────────────────
// Signed in, these show the account's real feeds. Signed out, they explain what signing
// in brings rather than showing sample content.

val LocalPlaylistMenu = staticCompositionLocalOf<(PlaylistRef) -> Unit> { {} }

enum class TvHomeChip(val label: String) {
    All("All"),
    Music("Music"),
    News("News"),
    Shorts("Shorts"),
    Mixes("Mixes"),
}

@Composable
private fun YouTubeTvFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val bg = when {
        focused -> Color.White
        selected -> Color(0xFFF1F1F1)
        else -> Color(0xFF272727)
    }
    val ink = if (focused || selected) Color(0xFF0F0F0F) else Color(0xFFF1F1F1)
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.hasFocus || it.isFocused }
            .pressable(
                pressedScale = 0.96f,
                focusScale = 1.04f,
                focusShape = shape,
                showFocusBorder = false,
                onClick = onClick,
            )
            .clip(shape)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = ink,
            fontFamily = FontFamily.SansSerif,
            fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun HomeScreen(
    lib: LibraryData,
    shorts: ShortsState,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit,
    onPlayCollection: (PublicCollection) -> Unit,
    onSearch: () -> Unit,
    onSignIn: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val signedIn = Account.signedIn
    LaunchedEffect(signedIn) { if (signedIn) lib.home.ensureLoaded() }
    LaunchedEffect(Unit) { shorts.ensureFeed() }
    val isTv = LocalIsTv.current
    val c = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val homeGridCols = if (isTv) tvGridColumns(screenWidth, minColumnWidth = 280.dp, maxColumns = 3) else 1
    var selectedChip by remember { mutableStateOf(TvHomeChip.All) }
    var newsVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var newsLoading by remember { mutableStateOf(false) }
    var newsError by remember { mutableStateOf<String?>(null) }
    var newsReloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(isTv, signedIn, selectedChip, newsReloadKey) {
        if (isTv && signedIn && selectedChip == TvHomeChip.News && (newsVideos.isEmpty() || newsReloadKey > 0)) {
            newsLoading = true
            newsError = null
            runCatching { YouTube.search("latest news", SearchFilter.News) }
                .onSuccess { results ->
                    newsVideos = results.mapNotNull { it.video }.filterNot { it.isShortVideo }.distinctBy { it.id }
                }
                .onFailure {
                    newsError = YouTube.friendlyError(it)
                }
            newsLoading = false
        }
    }

    val musicVideos by remember {
        derivedStateOf { MusicFeedFilter.filterMusicVideos(lib.home.videos) }
    }
    val musicCollections by remember {
        derivedStateOf { MusicFeedFilter.filterMusicCollections(lib.home.collections) }
    }
    var musicAutoPages by remember(lib.home.generation) { mutableIntStateOf(0) }
    LaunchedEffect(isTv, signedIn, selectedChip, lib.home.videos.size, lib.home.loading, lib.home.hasMore) {
        if (isTv && signedIn && selectedChip == TvHomeChip.Music &&
            lib.home.videos.isNotEmpty() && musicVideos.isEmpty() && musicCollections.isEmpty() &&
            lib.home.hasMore && !lib.home.loading && musicAutoPages < 2
        ) {
            musicAutoPages++
            lib.home.loadMore()
        }
    }

    CollapsingHeaderLayout(
        resetKey = listOf(scrollToTop, lib.home.generation, selectedChip),
        header = {
            Column {
                ScreenHeader(title = "iTube TV", brandMark = true, route = "tab:Home") {
                    if (isTv && signedIn) CircleAction(Icons.Rounded.Refresh, "Refresh") {
                        if (selectedChip == TvHomeChip.News) newsReloadKey++
                        else lib.home.refresh()
                    }
                    UpdateAction()
                    AccountAction(onAccount)
                }
                if (isTv && signedIn) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    ) {
                        itemsIndexed(TvHomeChip.entries, key = { _, chip -> "tv-home-chip-${chip.name}" }) { idx, chip ->
                            YouTubeTvFilterChip(
                                label = chip.label,
                                selected = selectedChip == chip,
                                modifier = Modifier.tvContentEntry("tab:Home", priority = 10 + idx),
                            ) {
                                selectedChip = chip
                            }
                        }
                    }
                }
            }
        },
    ) { top ->
        if (!signedIn) {
            LazyColumn(contentPadding = contentPadding.withTop(top)) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Subscriptions,
                        title = "Your recommendations, minus the ads",
                        body = "Sign in with Google to see your own YouTube home feed, or just search.",
                        action = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PillButton(
                                    "Sign in with Google",
                                    onSignIn,
                                    modifier = Modifier.tvContentEntry("tab:Home", centeredEmptyState = true),
                                )
                                Spacer(Modifier.height(12.dp))
                                TextLink("Search instead", onSearch)
                            }
                        },
                    )
                }
            }
        } else if (isTv && selectedChip != TvHomeChip.All) {
            when (selectedChip) {
                TvHomeChip.Music -> {
                    val list = rememberScrollToTop(scrollToTop)
                    LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                        if (musicCollections.isNotEmpty()) {
                            item(key = "tv-chip-music-collections") {
                                HomeCollectionsShelf(musicCollections, onPlayCollection)
                            }
                        }
                        if (musicVideos.isNotEmpty()) {
                            item(key = "tv-chip-music-title") { SectionTitle("Music videos") }
                            val rows = musicVideos.chunked(homeGridCols)
                            itemsIndexed(rows, key = { _, r -> "tv-chip-music-${r.first().id}" }) { rowIdx, row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    row.forEachIndexed { colIdx, video ->
                                        val flatIdx = rowIdx * homeGridCols + colIdx
                                        Box(Modifier.weight(1f)) {
                                            VideoCard(
                                                video,
                                                onClick = { onOpen(video) },
                                                modifier = if (flatIdx < 6) Modifier.tvContentEntry("tab:Home", priority = flatIdx) else Modifier,
                                            )
                                        }
                                    }
                                    repeat(homeGridCols - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        } else if (lib.home.loading) {
                            item { HomeScreenSkeleton() }
                        } else {
                            item {
                                EmptyState(
                                    icon = Icons.Rounded.Refresh,
                                    title = "No music videos found yet",
                                    body = "Refresh your feed or return to All recommendations.",
                                    action = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            PillButton(
                                                "Refresh",
                                                { lib.home.refresh() },
                                                modifier = Modifier.tvContentEntry("tab:Home", centeredEmptyState = true, priority = 0),
                                            )
                                            PillButton("Show all", { selectedChip = TvHomeChip.All })
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                TvHomeChip.News -> {
                    val list = rememberScrollToTop(scrollToTop)
                    LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                        when {
                            newsLoading && newsVideos.isEmpty() -> item { HomeScreenSkeleton() }
                            newsError != null && newsVideos.isEmpty() -> item {
                                EmptyState(
                                    icon = Icons.Rounded.CloudOff,
                                    title = "Couldn't load News",
                                    body = newsError.orEmpty(),
                                    action = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            PillButton(
                                                "Try again",
                                                { newsReloadKey++ },
                                                modifier = Modifier.tvContentEntry("tab:Home", centeredEmptyState = true, priority = 0),
                                            )
                                            PillButton("Show all", { selectedChip = TvHomeChip.All })
                                        }
                                    },
                                )
                            }
                            newsVideos.isEmpty() -> item {
                                EmptyState(
                                    icon = Icons.Rounded.Refresh,
                                    title = "No news videos right now",
                                    body = "Try refreshing or switch back to All recommendations.",
                                    action = {
                                        PillButton(
                                            "Show all",
                                            { selectedChip = TvHomeChip.All },
                                            modifier = Modifier.tvContentEntry("tab:Home", centeredEmptyState = true, priority = 0),
                                        )
                                    },
                                )
                            }
                            else -> {
                                item(key = "tv-chip-news-title") { SectionTitle("Top news") }
                                val rows = newsVideos.chunked(homeGridCols)
                                itemsIndexed(rows, key = { _, r -> "tv-chip-news-${r.first().id}" }) { rowIdx, row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        row.forEachIndexed { colIdx, video ->
                                            val flatIdx = rowIdx * homeGridCols + colIdx
                                            Box(Modifier.weight(1f)) {
                                                VideoCard(
                                                    video,
                                                    onClick = { onOpen(video) },
                                                    modifier = if (flatIdx < 6) Modifier.tvContentEntry("tab:Home", priority = flatIdx) else Modifier,
                                                )
                                            }
                                        }
                                        repeat(homeGridCols - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
                TvHomeChip.Shorts -> {
                    val list = rememberScrollToTop(scrollToTop)
                    val shortCols = tvGridColumns(screenWidth, 160.dp, 5)
                    LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                        if (shorts.items.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Rounded.Refresh,
                                    title = "No Shorts loaded yet",
                                    body = "Try again or return to All recommendations.",
                                    action = {
                                        PillButton(
                                            "Show all",
                                            { selectedChip = TvHomeChip.All },
                                            modifier = Modifier.tvContentEntry("tab:Home", centeredEmptyState = true, priority = 0),
                                        )
                                    },
                                )
                            }
                        } else {
                            item(key = "tv-chip-shorts-title") { SectionTitle("Shorts") }
                            val rows = shorts.items.chunked(shortCols)
                            itemsIndexed(rows, key = { _, r -> "tv-chip-shorts-${r.first().id}" }) { rowIdx, row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    row.forEachIndexed { colIdx, short ->
                                        val flatIdx = rowIdx * shortCols + colIdx
                                        Box(Modifier.weight(1f)) {
                                            TvShortCard(
                                                short = short,
                                                modifier = Modifier.fillMaxWidth().then(
                                                    if (flatIdx < 5) Modifier.tvContentEntry("tab:Home", priority = flatIdx) else Modifier
                                                ),
                                                onClick = { onOpenShort(short) },
                                            )
                                        }
                                    }
                                    repeat(shortCols - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
                TvHomeChip.Mixes -> {
                    val list = rememberScrollToTop(scrollToTop)
                    val colCols = tvGridColumns(screenWidth, 230.dp, 4)
                    LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                        if (lib.home.collections.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Rounded.Refresh,
                                    title = "No mixes or playlists yet",
                                    body = "Refresh your feed or switch back to All recommendations.",
                                    action = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            PillButton(
                                                "Refresh",
                                                { lib.home.refresh() },
                                                modifier = Modifier.tvContentEntry("tab:Home", centeredEmptyState = true, priority = 0),
                                            )
                                            PillButton("Show all", { selectedChip = TvHomeChip.All })
                                        }
                                    },
                                )
                            }
                        } else {
                            item(key = "tv-chip-mixes-title") { SectionTitle("Mixes & playlists") }
                            val rows = lib.home.collections.chunked(colCols)
                            itemsIndexed(rows, key = { _, r -> "tv-chip-mix-${r.first().url}" }) { rowIdx, row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    row.forEachIndexed { colIdx, collection ->
                                        val flatIdx = rowIdx * colCols + colIdx
                                        Box(Modifier.weight(1f)) {
                                            TvCollectionCard(
                                                collection = collection,
                                                modifier = Modifier.fillMaxWidth().then(
                                                    if (flatIdx < 4) Modifier.tvContentEntry("tab:Home", priority = flatIdx) else Modifier
                                                ),
                                                onClick = { onPlayCollection(collection) },
                                            )
                                        }
                                    }
                                    repeat(colCols - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
                TvHomeChip.All -> Unit
            }
        } else if (isTv) {
            val showTvShelves = lib.home.videos.size > 6
            val recommended = if (showTvShelves) lib.home.videos.take(6) else emptyList()
            val gridVideos = if (showTvShelves) lib.home.videos.drop(6) else lib.home.videos
            FeedList(
                feed = lib.home,
                contentPadding = contentPadding.withTop(top),
                onOpen = onOpen,
                videosOverride = if (showTvShelves) gridVideos else null,
                header = {
                    if (showTvShelves) {
                        item(key = "tv-recommended-title") { SectionTitle("Recommended") }
                        item(key = "tv-recommended-shelf") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                itemsIndexed(recommended, key = { _, v -> "tv-rec-${v.id}" }) { idx, video ->
                                    VideoCard(
                                        video = video,
                                        onClick = { onOpen(video) },
                                        modifier = Modifier
                                            .width(292.dp)
                                            .tvContentEntry("tab:Home", priority = idx),
                                    )
                                }
                            }
                        }
                        if (shorts.items.isNotEmpty()) {
                            item(key = "tv-home-shorts") {
                                HomeShortsShelf(shorts.items, 0, onOpenShort)
                            }
                        }
                        if (lib.home.collections.isNotEmpty()) {
                            item(key = "tv-home-mixes") {
                                HomeCollectionsShelf(lib.home.collections, onPlayCollection)
                            }
                        }
                        if (gridVideos.isNotEmpty()) {
                            item(key = "tv-more-videos-title") { SectionTitle("More videos") }
                        }
                    }
                },
                scrollToTop = scrollToTop,
                indicatorTop = top,
                entryRoute = if (showTvShelves) null else "tab:Home",
            )
        } else {
            FeedList(
                lib.home,
                contentPadding.withTop(top),
                onOpen,
                videoBatchSizes = listOf(3, 4),
                afterVideoBatch = { list, batch ->
                    list.item(key = "homeShorts-$batch") {
                        HomeShortsShelf(shorts.items, batch, onOpenShort)
                    }
                    if (batch == 0 && lib.home.collections.isNotEmpty()) {
                        list.item(key = "homeMixesAndPlaylists") {
                            HomeCollectionsShelf(lib.home.collections, onPlayCollection)
                        }
                    }
                },
                scrollToTop = scrollToTop,
                indicatorTop = top,
                entryRoute = "tab:Home",
            )
        }
    }
}

@Composable
private fun TvShortCard(
    short: VideoItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val menu = LocalVideoMenu.current
    var focused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .aspectRatio(9f / 16f)
            .prefetchOnPress(short.id)
            .onFocusChanged { focused = it.hasFocus || it.isFocused }
            .pressable(
                pressedScale = 0.98f,
                focusScale = 1.04f,
                showFocusBorder = false,
                onLongClick = { menu(short) },
                onClick = onClick,
            )
            .clip(cardShape)
            .drawWithContent {
                drawContent()
                if (focused) {
                    val outline = cardShape.createOutline(size, layoutDirection, this)
                    drawOutline(outline, Color.Black.copy(alpha = 0.78f), style = Stroke(4.2.dp.toPx()))
                    drawOutline(outline, Color.White, style = Stroke(2.6.dp.toPx()))
                }
            },
    ) {
        Thumbnail(
            url = short.shortThumbnail,
            modifier = Modifier.fillMaxSize(),
            radius = 0,
            fallbackUrl = short.thumbnail,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.84f)),
            ),
        )
        Text(
            short.title,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun TvCollectionCard(
    collection: PublicCollection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val isMix = collection.subtitle.contains("mix", ignoreCase = true) ||
        collection.url.substringAfter("list=", "").startsWith("RD")
    var focused by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .onFocusChanged { focused = it.hasFocus || it.isFocused }
            .pressable(
                pressedScale = 0.98f,
                focusScale = 1.03f,
                showFocusBorder = false,
                onClick = onClick,
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.88f)
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(if (focused) Color.White.copy(alpha = 0.72f) else Color(0xFF3A3A3A)),
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(thumbShape)
                .drawWithContent {
                    drawContent()
                    if (focused) {
                        val outline = thumbShape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, Color.Black.copy(alpha = 0.78f), style = Stroke(4.2.dp.toPx()))
                        drawOutline(outline, Color.White, style = Stroke(2.6.dp.toPx()))
                    }
                },
        ) {
            Thumbnail(collection.thumbnail, Modifier.fillMaxSize(), radius = 0)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
            Text(
                if (isMix) "MIX" else "PLAYLIST",
                Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.78f)).padding(horizontal = 7.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (focused) Color.White else Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    "Play ${collection.title}",
                    tint = if (focused) Color(0xFF0F0F0F) else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            collection.title,
            Modifier.padding(top = 8.dp),
            color = if (focused) Color.White else c.onSurface,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (isMix) "Play mix · all tracks" else "Play playlist · all videos",
            color = c.onSurfaceVariant,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A compact real Shorts rail, sourced from the same InnerTube feed as the Shorts tab. */
@Composable
private fun HomeShortsShelf(shorts: List<VideoItem>, shelfIndex: Int, onOpenShort: (VideoItem) -> Unit) {
    val isTv = LocalIsTv.current
    val shelfShorts = if (isTv) shorts.drop(shelfIndex * 8).take(10) else shorts.drop(shelfIndex * 4).take(4)
    if (shelfShorts.isEmpty()) return
    val menu = LocalVideoMenu.current
    val cardShape = RoundedCornerShape(16.dp)
    Column(Modifier.padding(top = 6.dp, bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isTv) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF0000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Shorts",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text("i", color = Color(0xFFFF0000), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Shorts", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (isTv) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shelfShorts, key = { "home-short-${it.id}" }) { short ->
                    TvShortCard(
                        short = short,
                        modifier = Modifier.width(156.dp),
                        onClick = { onOpenShort(short) },
                    )
                }
            }
        } else {
        shelfShorts.chunked(2).forEachIndexed { rowIndex, rowShorts ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowShorts.forEach { short ->
                    Box(
                        Modifier.weight(1f)
                            .aspectRatio(9f / 16f)
                            .prefetchOnPress(short.id)
                            .pressable(pressedScale = 0.98f, focusScale = 1.03f, focusShape = cardShape, onLongClick = { menu(short) }, onClick = { onOpenShort(short) })
                            .clip(cardShape),
                    ) {
                        Thumbnail(
                            url = short.shortThumbnail,
                            modifier = Modifier.fillMaxSize(),
                            radius = 0,
                            fallbackUrl = short.thumbnail,
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(0.42f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)),
                            ),
                        )
                        if (rowIndex == 0) {
                            Box(
                                Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp))
                                    .background(Color.White).padding(horizontal = 8.dp, vertical = 3.dp),
                            ) { Text("New", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        }
                        IconButton(
                            onClick = { menu(short) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(34.dp).tvFocusRing(CircleShape),
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Short options", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                        Text(
                            short.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 11.dp),
                        )
                    }
                }
                if (rowShorts.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        }
    }
}

/** Real YouTube Home Mixes and playlists; selecting a card queues its actual playlist tracks. */
@Composable
private fun HomeCollectionsShelf(collections: List<PublicCollection>, onPlay: (PublicCollection) -> Unit) {
    if (collections.isEmpty()) return
    val c = MaterialTheme.colorScheme
    val isTv = LocalIsTv.current
    Column(Modifier.padding(top = 4.dp, bottom = 10.dp)) {
        SectionTitle("Mixes & playlists")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 14.dp else 12.dp),
        ) {
            items(collections, key = { "home-collection-${it.url}" }) { collection ->
                if (isTv) {
                    TvCollectionCard(
                        collection = collection,
                        modifier = Modifier.width(248.dp),
                        onClick = { onPlay(collection) },
                    )
                } else {
                    val isMix = collection.subtitle.contains("mix", ignoreCase = true) ||
                        collection.url.substringAfter("list=", "").startsWith("RD")
                    Column(Modifier.width(190.dp).pressable(pressedScale = 0.98f, focusScale = 1.03f, focusShape = RoundedCornerShape(18.dp), onClick = { onPlay(collection) })) {
                        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(18.dp))) {
                            Thumbnail(collection.thumbnail, Modifier.fillMaxSize(), radius = 0)
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.25f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
                            Text(
                                if (isMix) "MIX" else "PLAYLIST",
                                Modifier.align(Alignment.TopStart).padding(9.dp).clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.58f)).padding(horizontal = 9.dp, vertical = 4.dp),
                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            )
                            Icon(Icons.Rounded.PlayArrow, "Play ${collection.title}", tint = Color.White,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(9.dp).size(30.dp)
                                    .clip(CircleShape).background(c.primary).padding(5.dp))
                        }
                        Text(collection.title, Modifier.padding(top = 7.dp), color = c.onSurface, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(if (isMix) "Play mix · all tracks" else "Play playlist · all videos", color = c.onSurfaceVariant,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onSignIn: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val signedIn = Account.signedIn
    LaunchedEffect(signedIn) {
        if (signedIn) {
            lib.subscriptions.ensureLoaded()
            if (lib.channels.isEmpty()) lib.loadChannels()
        }
    }
    val isTv = LocalIsTv.current
    CollapsingHeaderLayout(
        resetKey = scrollToTop to lib.subscriptions.generation,
        header = {
            ScreenHeader(title = "Subscriptions", route = "tab:Subscriptions") {
                if (isTv && signedIn) CircleAction(Icons.Rounded.Refresh, "Refresh") { lib.subscriptions.refresh(); lib.loadChannels() }
                AccountAction(onAccount)
            }
        },
    ) { top ->
        if (signedIn) {
            FeedList(
                lib.subscriptions,
                contentPadding.withTop(top),
                onOpen,
                header = {
                    // Always present (empty until loaded) so its arrival never shifts the list.
                    item(key = "channels") { if (lib.channels.isNotEmpty()) ChannelRail(lib.channels, entryRoute = "tab:Subscriptions") }
                },
                scrollToTop = scrollToTop,
                indicatorTop = top,
                entryRoute = if (lib.channels.isEmpty()) "tab:Subscriptions" else null,
            )
        } else {
            LazyColumn(contentPadding = contentPadding.withTop(top)) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Subscriptions,
                        title = "Your subscriptions",
                        body = "Sign in with Google to see new videos from the channels you follow.",
                        action = {
                            PillButton(
                                "Sign in with Google",
                                onSignIn,
                                modifier = Modifier.tvContentEntry("tab:Subscriptions", centeredEmptyState = true),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRail(channels: List<ChannelRef>, entryRoute: String? = null) {
    val c = MaterialTheme.colorScheme
    val openChannel = LocalChannelOpener.current
    val firstId = channels.firstOrNull()?.id
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
    ) {
        items(channels, key = { it.id }) { ch ->
            Column(
                Modifier
                    .then(if (entryRoute != null && ch.id == firstId) Modifier.tvContentEntry(entryRoute) else Modifier)
                    .width(64.dp)
                    .pressable(pressedScale = 0.94f, focusScale = 1.04f, focusShape = RoundedCornerShape(12.dp)) { openChannel(ch.id) }
                    .clip(RoundedCornerShape(12.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ChannelAvatar(ch.avatar, ch.name, size = 56)
                Spacer(Modifier.height(6.dp))
                Text(ch.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun LibraryScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit = onOpen,
    onOpenHistory: () -> Unit = {},
    onOpenPlaylist: (PlaylistRef) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenCollection: (PublicCollection) -> Unit,
    onSignIn: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val signedIn = Account.signedIn
    LaunchedEffect(signedIn) {
        if (signedIn) {
            lib.history.ensureLoaded()
            if (lib.playlists.isEmpty()) lib.loadPlaylists()
        }
    }
    val isTv = LocalIsTv.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    CollapsingHeaderLayout(
        resetKey = scrollToTop to lib.history.generation,
        header = {
            ScreenHeader(title = "Library", route = "tab:Library") {
                if (isTv && signedIn) CircleAction(Icons.Rounded.Refresh, "Refresh") { lib.history.refresh(); lib.loadPlaylists() }
                AccountAction(onAccount)
            }
        },
    ) { top ->
        PullToRefresh(
            enabled = signedIn,
            refreshing = lib.history.refreshing,
            onRefresh = { lib.history.refresh(); lib.loadPlaylists() },
            topPadding = top,
        ) {
        LazyColumn(state = rememberScrollToTop(scrollToTop), contentPadding = contentPadding.withTop(top)) {
            if (!signedIn) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.VideoLibrary,
                        title = "Your library",
                        body = "Sign in with Google for your history, Watch later, liked videos and playlists.",
                        action = {
                            PillButton(
                                "Sign in with Google",
                                onSignIn,
                                modifier = Modifier.tvContentEntry("tab:Library", centeredEmptyState = true),
                            )
                        },
                    )
                }
            }
            if (signedIn) {
                item(key = "historyTitle") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "History",
                            fontFamily = if (isTv) FontFamily.SansSerif else RoundedHeavy,
                            fontWeight = if (isTv) FontWeight.Bold else FontWeight.ExtraBold,
                            fontSize = if (isTv) 18.sp else 19.sp,
                            letterSpacing = if (isTv) (-0.2).sp else (-0.5).sp,
                        )
                        if (isTv) {
                            YouTubeTvFilterChip(
                                label = "View all",
                                selected = false,
                                modifier = Modifier.tvContentEntry("tab:Library", priority = 0),
                                onClick = onOpenHistory,
                            )
                        } else {
                            Text(
                                "View all",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.tvContentEntry("tab:Library", priority = 0).pressable(focusShape = CircleShape, onClick = onOpenHistory).padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                item(key = "history") {
                    val history = lib.history.videos
                    when {
                        history.isNotEmpty() -> LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (isTv) 14.dp else 10.dp),
                        ) {
                            items(history.take(20), key = { "h-" + it.id }) { video ->
                                HistoryCard(video) {
                                    if (video.isShortVideo) onOpenShort(video) else onOpen(video)
                                }
                            }
                        }
                        lib.history.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                            MaterialLoader(color = MaterialTheme.colorScheme.primary, size = 28.dp)
                        }
                        else -> Text(
                            lib.history.error ?: "Nothing watched yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
            item(key = "offlineMediaTitle") { SectionTitle("Downloads") }
            item(key = "offlineMedia") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PlaylistTile(
                        PlaylistRef("DL", "Downloads", null, "${DownloadLibrary.entries.size} videos"),
                        (if (isTv) Modifier.widthIn(max = 320.dp).fillMaxWidth() else Modifier.weight(1f))
                            .tvContentEntry("tab:Library", priority = 1),
                        onOpenDownloads,
                    )
                    if (!isTv) Spacer(Modifier.weight(1f))
                }
            }
            if (SavedCollections.items.isNotEmpty()) {
                item(key = "savedCollectionsTitle") { SectionTitle("Saved collections") }
                items(SavedCollections.items, key = { "saved-collection-${it.url}" }) { collection ->
                    PublicCollectionRow(collection, onOpenCollection)
                }
            }
            if (!signedIn) return@LazyColumn
            item(key = "playlistsTitle") { SectionTitle("Playlists") }
            val pinned = listOf(
                PlaylistRef("WL", "Watch later", null, null),
                PlaylistRef("LL", "Liked videos", null, null),
            )
            val plCols = if (isTv) tvGridColumns(screenWidth, 210.dp, 4) else 2
            itemsIndexed((pinned + lib.playlists).chunked(plCols), key = { _, row -> row.joinToString { it.id } }) { rowIdx, row ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { colIdx, pl ->
                        PlaylistTile(
                            pl,
                            Modifier.weight(1f).then(if (rowIdx == 0 && colIdx < 2) Modifier.tvContentEntry("tab:Library", priority = 2 + colIdx) else Modifier),
                        ) { onOpenPlaylist(pl) }
                    }
                    repeat(plCols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        }
    }
}

@Composable
private fun HistoryCard(video: VideoItem, onClick: () -> Unit) {
    val menu = LocalVideoMenu.current
    val isTv = LocalIsTv.current
    if (isTv) {
        var focused by remember { mutableStateOf(false) }
        val thumbShape = RoundedCornerShape(12.dp)
        Column(
            Modifier
                .width(220.dp)
                .prefetchOnPress(video.id)
                .onFocusChanged { focused = it.hasFocus || it.isFocused }
                .pressable(
                    pressedScale = 0.97f,
                    focusScale = 1.03f,
                    showFocusBorder = false,
                    onLongClick = { menu(video) },
                    onClick = onClick,
                ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(thumbShape)
                    .drawWithContent {
                        drawContent()
                        if (focused) {
                            val outline = thumbShape.createOutline(size, layoutDirection, this)
                            drawOutline(outline, Color.Black.copy(alpha = 0.78f), style = Stroke(4.2.dp.toPx()))
                            drawOutline(outline, Color.White, style = Stroke(2.6.dp.toPx()))
                        }
                    },
            ) {
                Thumbnail(video.thumbnail, Modifier.fillMaxSize(), radius = 0)
                DurationPill(video.durationSec, Modifier.align(Alignment.BottomEnd).padding(6.dp), small = true)
                ResumeBar(video.id, Modifier.align(Alignment.BottomStart))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                video.title,
                color = if (focused) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }
    Column(Modifier.width(168.dp).prefetchOnPress(video.id).pressable(pressedScale = 0.97f, focusScale = 1.03f, focusShape = RoundedCornerShape(20.dp), onLongClick = { menu(video) }, onClick = onClick)) {
        Box {
            Thumbnail(video.thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9f), radius = 20)
            DurationPill(video.durationSec, Modifier.align(Alignment.BottomEnd).padding(6.dp), small = true)
        }
        Spacer(Modifier.height(7.dp))
        Text(video.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistTile(playlist: PlaylistRef, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val isTv = LocalIsTv.current
    val openOptions = LocalPlaylistMenu.current
    val canManage = playlist.id !in setOf("WL", "LL", "DL")
    val (icon, tone) = when (playlist.id) {
        "WL" -> Icons.Rounded.WatchLater to c.primaryContainer
        "LL" -> Icons.Rounded.ThumbUp to c.tertiaryContainer
        "DL" -> Icons.Rounded.Download to c.primaryContainer
        else -> Icons.AutoMirrored.Rounded.PlaylistPlay to c.secondaryContainer
    }
    if (isTv) {
        var focused by remember { mutableStateOf(false) }
        val tvShape = RoundedCornerShape(12.dp)
        Column(
            modifier = modifier
                .height(128.dp)
                .onFocusChanged { focused = it.hasFocus || it.isFocused }
                .pressable(
                    pressedScale = 0.97f,
                    focusScale = 1.03f,
                    showFocusBorder = false,
                    onLongClick = { if (canManage) openOptions(playlist) },
                    onClick = onClick,
                )
                .clip(tvShape)
                .background(if (focused) Color(0xFF282828) else Color(0xFF181818))
                .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF2A2A2A), tvShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (playlist.thumbnail != null) {
                Thumbnail(playlist.thumbnail, Modifier.size(width = 76.dp, height = 44.dp), radius = 8)
            } else {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(if (focused) Color.White else Color(0xFF282828)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = if (focused) Color(0xFF0F0F0F) else Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Column {
                Text(
                    playlist.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                playlist.subtitle?.let {
                    Text(it, color = c.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
        return
    }
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .height(132.dp)
            .pressable(pressedScale = 0.97f, focusScale = 1.03f, focusShape = shape, onLongClick = { if (canManage) openOptions(playlist) }, onClick = onClick)
            .clip(shape)
            .background(c.surfaceContainerLow)
            .border(0.75.dp, c.outlineVariant.copy(alpha = 0.28f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (playlist.thumbnail != null) {
            Thumbnail(playlist.thumbnail, Modifier.size(width = 72.dp, height = 44.dp), radius = 12)
        } else {
            Box(Modifier.size(52.dp).clip(CookieShape).background(tone), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = contentColorFor(tone))
            }
        }
        Column {
            Text(playlist.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            playlist.subtitle?.let { Text(it, color = c.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsSheet(
    playlist: PlaylistRef,
    onDismiss: () -> Unit,
    onDelete: (PlaylistRef) -> Unit,
    onReload: (PlaylistRef) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmDelete by remember(playlist.id) { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Column(Modifier.fillMaxWidth().focusGroup()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(c.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                if (playlist.thumbnail != null) Thumbnail(playlist.thumbnail, Modifier.fillMaxSize(), radius = 0)
                else Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, tint = c.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                playlist.subtitle?.let { Text(it, color = c.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        PlaylistAction(Icons.Rounded.Refresh, "Refresh") { onReload(playlist); onDismiss() }
        if (playlist.id !in setOf("WL", "LL", "DL")) {
            PlaylistAction(Icons.Rounded.Share, "Share playlist") {
                shareLink(context, "https://www.youtube.com/playlist?list=${playlist.id}", playlist.title)
                onDismiss()
            }
            PlaylistAction(Icons.Rounded.Delete, "Delete playlist", c.error) { confirmDelete = true }
        }
        Spacer(Modifier.height(18.dp))
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("“${playlist.title}” will be deleted from your YouTube account.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(playlist) }, modifier = Modifier.tvFocusRing(CircleShape)) { Text("Delete", color = c.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, modifier = Modifier.tvFocusRing(CircleShape)) { Text("Cancel") } },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        )
    }
}

@Composable
private fun PlaylistAction(icon: ImageVector, title: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(pressedScale = 0.98f, focusScale = 1.015f, focusShape = RoundedCornerShape(14.dp), onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(title, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

/** Videos saved into Downloads/iTube; opening a row uses its stored file URI and stored metadata. */
@Composable
fun DownloadsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (DownloadedVideo) -> Unit,
) {
    val downloads = DownloadLibrary.entries
    val active = DownloadLibrary.activeDownloads
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    CollapsingHeaderLayout(
        resetKey = downloads.size + active.size,
        header = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.tvContentEntry("overlay:downloads"), onBack)
                Spacer(Modifier.width(12.dp))
                Text("Downloads", fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
        },
    ) { top ->
        if (downloads.isEmpty() && active.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = top), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Rounded.Download, "No downloads yet", "Videos you download will appear here and play offline.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding.withTop(top)) {
                if (active.isNotEmpty()) {
                    item { SectionTitle("Downloading") }
                    items(active, key = { "active-download-${it.id}" }) { task ->
                        ActiveDownloadRow(task) { VideoDownloader.cancel(context, task.id) }
                    }
                }
                if (downloads.isNotEmpty()) {
                    item { SectionTitle("Downloaded videos") }
                    items(downloads, key = { "download-" + it.item.id }) { download ->
                        DownloadedVideoRow(download) { onOpen(download) }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ActiveDownloadRow(download: ActiveDownload, onCancel: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(112.dp).aspectRatio(16f / 9f)) {
            Thumbnail(download.item.thumbnail, Modifier.fillMaxSize(), radius = 14)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp)) {
                if (download.progress == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(progress = { download.progress.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(download.item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(download.status, color = c.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCancel, modifier = Modifier.tvFocusRing(CircleShape)) { Icon(Icons.Rounded.Cancel, contentDescription = "Cancel download", tint = c.error) }
    }
}

@Composable
private fun DownloadedVideoRow(download: DownloadedVideo, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().pressable(pressedScale = 0.98f, focusScale = 1.015f, focusShape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(132.dp).aspectRatio(16f / 9f)) {
            Thumbnail(download.item.thumbnail, Modifier.fillMaxSize(), radius = 16)
            DurationPill(download.item.durationSec, Modifier.align(Alignment.BottomEnd).padding(6.dp), small = true)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(download.item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(download.item.channel, color = c.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    download.item.views.takeIf { it >= 0 }?.let { "${formatCount(it)} views" },
                    download.item.uploaded,
                ).joinDots().ifBlank { "Downloaded" },
                color = c.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Dedicated full history screen with pagination, Shorts shelf, and smooth navigation. */
@Composable
fun HistoryScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit = onOpen,
) {
    val feed = lib.history
    val videoMenu = LocalVideoMenu.current
    LaunchedEffect(Unit) { feed.ensureLoaded() }
    val historyShorts = remember(feed.videos) { feed.videos.filter { it.isShortVideo } }
    val regularVideos = remember(feed.videos, historyShorts) {
        if (historyShorts.isNotEmpty()) feed.videos.filterNot { it.isShortVideo } else feed.videos
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        CollapsingHeaderLayout(
            resetKey = feed.generation,
            header = {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.tvContentEntry("overlay:history"), onBack)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "History",
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.8).sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        ) { top ->
            if (feed.videos.isEmpty() && !feed.loading && feed.error == null) {
                Box(Modifier.fillMaxSize().padding(top = top), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Rounded.History, "No watch history", "Videos and shorts you watch will appear here.")
                }
            } else {
                FeedList(
                    feed = feed,
                    contentPadding = contentPadding.withTop(top),
                    videosOverride = if (historyShorts.isNotEmpty()) regularVideos else null,
                    onOpen = { video -> if (video.isShortVideo) onOpenShort(video) else onOpen(video) },
                    header = {
                        if (historyShorts.isNotEmpty()) {
                            item(key = "history-shorts-shelf") {
                                PlaylistShortsShelf(shorts = historyShorts, onOpenShort = onOpenShort, onVideoLongPress = videoMenu)
                            }
                        }
                    },
                    indicatorTop = top,
                )
            }
        }
    }
}

/** A playlist's tracks stay separate from YouTube's recommendation shelf. */
@Composable
fun PlaylistScreen(
    playlist: PlaylistRef,
    feed: FeedState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit = onOpen,
    onPlayPlaylist: (List<VideoItem>, Int) -> Unit = { videos, index -> videos.getOrNull(index)?.let(onOpen) },
    onVideoLongPress: ((VideoItem) -> Unit)? = null,
) {
    val parentVideoMenu = LocalVideoMenu.current
    val videoMenu = LocalVideoMenu.current
    val playlistMenu = LocalPlaylistMenu.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val savedShorts = playlist.title.contains("short", ignoreCase = true)
    val isLikedVideos = playlist.id == "LL" || playlist.title.contains("Liked", ignoreCase = true)
    val likedShorts = remember(feed.videos, isLikedVideos) {
        if (isLikedVideos) feed.videos.filter { it.isShortVideo } else emptyList()
    }
    val regularVideos = remember(feed.videos, isLikedVideos, likedShorts) {
        if (isLikedVideos && likedShorts.isNotEmpty()) feed.videos.filterNot { it.isShortVideo } else feed.videos
    }
    fun playPlaylistItem(video: VideoItem) {
        scope.launch {
            val all = feed.loadAllVideos()
            val playlistVideos = if (isLikedVideos && likedShorts.isNotEmpty()) all.filterNot { it.isShortVideo } else all
            val index = playlistVideos.indexOfFirst { it.id == video.id }
            if (index >= 0) onPlayPlaylist(playlistVideos, index) else onOpen(video)
        }
    }
    LaunchedEffect(playlist.id) { feed.ensureLoaded() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    CompositionLocalProvider(LocalVideoMenu provides { video -> onVideoLongPress?.invoke(video) ?: parentVideoMenu(video) }) {
    CollapsingHeaderLayout(
        resetKey = feed.generation,
        header = {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.tvContentEntry("overlay:playlist", priority = 5), onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                playlist.title,
                fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.8).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { playlistMenu(playlist) }, modifier = Modifier.tvFocusRing(CircleShape)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        },
    ) { top ->
        val isTv = LocalIsTv.current
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val showShortsGrid = savedShorts || (isLikedVideos && regularVideos.isEmpty() && likedShorts.isNotEmpty())
        if (feed.videos.isEmpty() && feed.recommendations.isEmpty() && !feed.loading && feed.error == null && !feed.hasMore) {
            Box(Modifier.padding(top = top)) { EmptyState(Icons.Rounded.VideoLibrary, "Nothing here yet", "Videos you add to this playlist will show up here.") }
        } else if (showShortsGrid) {
            val shortsList = if (savedShorts) feed.videos else likedShorts
            val listState = rememberLazyListState()
            val nearEnd by remember(listState, shortsList.size) {
                derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= listState.layoutInfo.totalItemsCount - 3 } == true }
            }
            LaunchedEffect(nearEnd, shortsList.size) { if (nearEnd && feed.hasMore && !feed.loading) feed.loadMore() }
            val shortCols = if (isTv) tvGridColumns(screenWidth, 160.dp, 5) else 2
            val shortShape = RoundedCornerShape(18.dp)
            LazyColumn(state = listState, contentPadding = contentPadding.withTop(top)) {
                item(key = "playlist-actions") {
                    PlaylistPlaybackActions(
                        count = shortsList.size,
                        entryRoute = "overlay:playlist",
                        onPlay = { shortsList.firstOrNull()?.let(onOpenShort) },
                        onShuffle = { shortsList.shuffled().firstOrNull()?.let(onOpenShort) },
                    )
                }
                items(shortsList.chunked(shortCols), key = { row -> row.joinToString(prefix = "shorts-") { it.id } }) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { short ->
                            Box(
                                Modifier.weight(1f)
                                    .aspectRatio(9f / 16f)
                                    .prefetchOnPress(short.id)
                                    .pressable(pressedScale = 0.98f, focusScale = 1.03f, focusShape = shortShape, onLongClick = { videoMenu(short) }, onClick = { onOpenShort(short) })
                                    .clip(shortShape),
                            ) {
                                Thumbnail(
                                    url = short.shortThumbnail,
                                    modifier = Modifier.fillMaxSize(),
                                    radius = 0,
                                    fallbackUrl = short.thumbnail,
                                )
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.74f))))
                                Text(short.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                            }
                        }
                        repeat(shortCols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (feed.loading) item { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) { MaterialLoader(color = MaterialTheme.colorScheme.primary, size = 26.dp) } }
            }
        } else {
            val displayVideos = if (likedShorts.isNotEmpty()) regularVideos else feed.videos
            FeedList(
                feed = feed,
                contentPadding = contentPadding.withTop(top),
                videosOverride = if (likedShorts.isNotEmpty()) regularVideos else null,
                onOpen = { video ->
                    if (video.isShortVideo) {
                        onOpenShort(video)
                    } else if (feed.videos.any { it.id == video.id }) {
                        playPlaylistItem(video)
                    } else {
                        onOpen(video)
                    }
                },
                entryRoute = "overlay:playlist",
                header = {
                    item(key = "playlist-actions") {
                        PlaylistPlaybackActions(
                            count = displayVideos.size,
                            entryRoute = "overlay:playlist",
                            onPlay = {
                                scope.launch {
                                    val all = if (likedShorts.isNotEmpty()) feed.loadAllVideos().filterNot { it.isShortVideo } else feed.loadAllVideos()
                                    onPlayPlaylist(all, 0)
                                }
                            },
                            onShuffle = {
                                scope.launch {
                                    val all = (if (likedShorts.isNotEmpty()) feed.loadAllVideos().filterNot { it.isShortVideo } else feed.loadAllVideos()).shuffled()
                                    onPlayPlaylist(all, 0)
                                }
                            },
                        )
                    }
                    if (likedShorts.isNotEmpty()) {
                        item(key = "playlist-shorts-shelf") {
                            PlaylistShortsShelf(
                                shorts = likedShorts,
                                onOpenShort = onOpenShort,
                                onVideoLongPress = { video -> onVideoLongPress?.invoke(video) ?: parentVideoMenu(video) },
                            )
                        }
                    }
                    if (displayVideos.isEmpty() && feed.recommendations.isNotEmpty()) {
                        item(key = "playlist-empty-tip") {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Rounded.VideoLibrary, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No videos in this playlist yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("Add some from the recommendations below to get started", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                },
                indicatorTop = top,
                footer = {
                    if ((!feed.hasMore || displayVideos.isEmpty()) && feed.recommendations.isNotEmpty()) {
                        item(key = "playlist-recommended-title") {
                            PlaylistRecommendationsHeader(count = feed.recommendations.size)
                        }
                        items(feed.recommendations, key = { "playlist-recommendation-${it.id}" }) { video ->
                            RecommendedVideoRow(
                                video = video,
                                onOpen = {
                                    if (video.isShortVideo) onOpenShort(video) else onOpen(video)
                                },
                                onAdd = {
                                    feed.addPlaylistVideo(playlist.id, video)
                                },
                                onMenu = { videoMenu(video) },
                            )
                        }
                    }
                },
            )
        }
    }
    }
    }
}

@Composable
private fun PlaylistShortsShelf(
    shorts: List<VideoItem>,
    onOpenShort: (VideoItem) -> Unit,
    onVideoLongPress: (VideoItem) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF0000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Shorts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                )
            }
            Text(
                "${shorts.size} shorts",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shorts, key = { "playlist-short-${it.id}" }) { short ->
                Box(
                    Modifier
                        .width(140.dp)
                        .aspectRatio(9f / 16f)
                        .prefetchOnPress(short.id)
                        .pressable(
                            pressedScale = 0.96f,
                            focusScale = 1.03f,
                            focusShape = RoundedCornerShape(16.dp),
                            onLongClick = { onVideoLongPress(short) },
                            onClick = { onOpenShort(short) },
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surfaceContainerHigh),
                ) {
                    Thumbnail(
                        url = short.shortThumbnail,
                        modifier = Modifier.fillMaxSize(),
                        radius = 0,
                        fallbackUrl = short.thumbnail,
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0.4f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    )
                    val isTv = LocalIsTv.current
                    if (!isTv) {
                        IconButton(
                            onClick = { onVideoLongPress(short) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(30.dp)
                                .tvFocusRing(CircleShape),
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        short.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRecommendationsHeader(count: Int) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp)
            .padding(horizontal = 16.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = c.outlineVariant.copy(alpha = 0.4f),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = c.onSecondaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Recommended",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(c.tertiaryContainer)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "SUGGESTIONS",
                                color = c.onTertiaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                    Text(
                        "Based on videos in this playlist",
                        fontSize = 12.sp,
                        color = c.onSurfaceVariant,
                    )
                }
            }
            if (count > 0) {
                Text(
                    "$count",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecommendedVideoRow(
    video: VideoItem,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onMenu: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val rowShape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .prefetchOnPress(video.id)
            .pressable(pressedScale = 0.98f, focusScale = 1.015f, focusShape = rowShape, onLongClick = onMenu, onClick = onOpen)
            .clip(rowShape)
            .background(c.surfaceContainerLow)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            Thumbnail(video.thumbnail, Modifier.fillMaxSize(), radius = 0)
            DurationPill(video.durationSec, Modifier.align(Alignment.BottomEnd).padding(4.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.tertiaryContainer.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    "Recommendation",
                    color = c.onTertiaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                video.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = c.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                metaLine(video.channel, video.views, video.uploaded),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = c.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        IconButton(
            onClick = onAdd,
            modifier = Modifier
                .size(36.dp)
                .tvFocusRing(CircleShape)
                .clip(CircleShape)
                .background(c.primaryContainer),
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add to playlist",
                tint = c.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onMenu,
            modifier = Modifier.size(34.dp).tvFocusRing(CircleShape),
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = c.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PlaylistPlaybackActions(
    count: Int,
    entryRoute: String? = null,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PlaylistActionButton(
            "Play all",
            Icons.Rounded.PlayArrow,
            Modifier.weight(1f).then(if (entryRoute != null) Modifier.tvContentEntry(entryRoute, priority = 0) else Modifier),
            onPlay,
        )
        PlaylistActionButton("Shuffle", Icons.Rounded.Shuffle, Modifier.weight(1f), onShuffle)
        if (count > 0) Text("$count", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp), fontSize = 13.sp)
    }
}

@Composable
private fun PlaylistActionButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.pressable(pressedScale = 0.97f, focusShape = CircleShape, onClick = onClick)
            .clip(CircleShape).background(c.secondaryContainer)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = c.onSecondaryContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = c.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        modifier = Modifier.pressable(focusShape = CircleShape, focusScale = 1.06f, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ── Search ─────────────────────────────────────────────────────────────────────

/** Lives above the tab switcher so results survive switching tabs. */
@Stable
class SearchState(private val scope: CoroutineScope) {
    var query by mutableStateOf("")
    var submitted by mutableStateOf("")
        private set
    var results by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    /** All is the default and uses YouTube's real ALL result filter. */
    var filter by mutableStateOf(SearchFilter.All)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var generation by mutableStateOf(0)
        private set
    private var job: Job? = null

    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    private var suggestionsJob: Job? = null

    fun updateQuery(value: String) {
        query = value
        suggestionsJob?.cancel()
        if (value.trim().length < 2) {
            suggestions = emptyList()
            return
        }
        suggestionsJob = scope.launch {
            kotlinx.coroutines.delay(180)
            suggestions = runCatching { YouTube.suggestions(value.trim()) }.getOrDefault(emptyList())
        }
    }

    /** Selecting a row in the search filter sheet immediately refreshes the active query. */
    fun selectFilter(newFilter: SearchFilter) {
        if (filter == newFilter) return
        filter = newFilter
        if (query.trim().isNotEmpty()) submit()
    }

    fun submit() {
        val q = query.trim()
        if (q.isEmpty()) return
        SearchHistory.add(q)
        submitted = q
        loading = true
        error = null
        job?.cancel()
        job = scope.launch {
            runCatching { YouTube.search(q, filter) }
                .onSuccess { results = it; generation++ }
                .onFailure { error = YouTube.friendlyError(it) }
            loading = false
        }
    }
}

private sealed interface SearchDisplayItem {
    data class Normal(val result: SearchResult) : SearchDisplayItem
    data class ShortsShelf(val key: String, val shorts: List<VideoItem>) : SearchDisplayItem
}

private fun groupSearchResults(results: List<SearchResult>): List<SearchDisplayItem> {
    if (results.isEmpty()) return emptyList()
    val displayItems = ArrayList<SearchDisplayItem>()
    val currentShorts = ArrayList<VideoItem>()
    var shelfCounter = 0

    fun flush() {
        if (currentShorts.isNotEmpty()) {
            displayItems.add(
                SearchDisplayItem.ShortsShelf(
                    key = "shorts_shelf_${shelfCounter++}_${currentShorts.first().id}",
                    shorts = ArrayList(currentShorts)
                )
            )
            currentShorts.clear()
        }
    }

    for (res in results) {
        val video = res.video
        if (video != null && video.isShortVideo) {
            currentShorts.add(video)
        } else {
            flush()
            displayItems.add(SearchDisplayItem.Normal(res))
        }
    }
    flush()
    return displayItems
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: SearchState,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit = {},
    onOpenResult: (SearchResult) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: Int = 0,
) {
    val c = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val view = LocalView.current
    val bridge = LocalTvFocusBridge.current
    val isTv = LocalIsTv.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val searchCols = if (isTv) tvGridColumns(screenWidth, 280.dp, 3) else 1
    // Search and Enter both dismiss the keyboard — every field, every time. A hide issued
    // while the key event is still being dispatched can be undone by the IME, so the
    // window-level hide is posted to run after it.
    fun submit() {
        if (!isTv) {
            focus.clearFocus(force = true)
        } else {
            focus.moveFocus(FocusDirection.Down)
        }
        keyboard?.hide()
        view.postDelayed({
            view.context.getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(view.windowToken, 0)
        }, 120)
        state.submit()
    }
    var focused by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
    // On touch phones, closing the soft keyboard drops field focus so results return cleanly.
    // On Android TV, only clear if an on-screen IME was previously visible and just dismissed.
    var imeWasVisible by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible, isTv) {
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible && !isTv && focused) {
            imeWasVisible = false
            focus.clearFocus()
        } else {
            imeWasVisible = false
        }
    }

    // Past searches, filtered by what's typed: while typing, and on the empty page.
    val history = SearchHistory.matching(state.query)
    val showHistory = history.isNotEmpty() && (focused || state.submitted.isEmpty())
    val showSuggestions = focused && state.query.trim().length >= 2 && state.suggestions.isNotEmpty()

    // The search bar rides with the header: both slide away together while scrolling results.
    CollapsingHeaderLayout(
        resetKey = listOf(scrollToTop, state.generation, showHistory),
        header = { Column {
        ScreenHeader(
            title = "Search",
            route = "tab:Search",
        ) {
            CircleAction(Icons.Rounded.Tune, "Search filters") { filtersOpen = true }
        }
        val barShape = if (isTv) RoundedCornerShape(10.dp) else CircleShape
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(if (isTv) 52.dp else 60.dp)
                .border(
                    width = if (focused) 2.2.dp else if (isTv) 1.dp else 0.75.dp,
                    color = if (focused && isTv) Color.White else if (focused) c.primary else if (isTv) Color(0xFF383838) else c.outlineVariant.copy(alpha = 0.25f),
                    shape = barShape,
                )
                .clip(barShape)
                .background(if (isTv) Color(0xFF212121) else c.surfaceContainerHigh)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = if (isTv && focused) Color.White else c.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text(
                        "Search iTube TV",
                        color = c.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = if (isTv) FontWeight.Medium else FontWeight.SemiBold
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = state::updateQuery,
                    singleLine = true,
                    textStyle = OwnDirectionText.merge(TextStyle(color = c.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)),
                    cursorBrush = SolidColor(if (isTv) Color.White else c.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }, onDone = { submit() }),
                    // Hardware Enter doesn't go through the IME action; treat it the same.
                    modifier = Modifier.tvContentEntry("tab:Search", priority = 8).fillMaxWidth().onFocusChanged { focused = it.isFocused }.onPreviewKeyEvent { e ->
                        if (e.key == Key.Enter || e.key == Key.NumPadEnter) {
                            if (e.type == KeyEventType.KeyDown) submit()
                            true
                        } else if (e.key == Key.DirectionDown && e.type == KeyEventType.KeyDown) {
                            focus.moveFocus(FocusDirection.Down)
                            true
                        } else if (isTv && e.key == Key.DirectionLeft && state.query.isEmpty() && e.type == KeyEventType.KeyDown) {
                            bridge?.focusRail() == true
                        } else false
                    },
                )
            }
            if (state.query.isNotEmpty()) {
                Box(
                    modifier = Modifier.size(44.dp).pressable(focusShape = CircleShape) { state.updateQuery("") },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Clear", tint = c.onSurfaceVariant) }
            }
        }
        if (isTv) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(SearchFilter.entries, key = { "tv-search-filter-${it.name}" }) { filter ->
                    YouTubeTvFilterChip(
                        label = filter.label,
                        selected = state.filter == filter,
                    ) {
                        state.selectFilter(filter)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }
        } },
    ) { top ->
        Box(Modifier.fillMaxSize()) {
            // Suggestions are already a scrolling list with the header inset. Including
            // them here avoids adding that inset a second time (which left a huge blank gap).
            val listShown = showSuggestions || showHistory || (state.results.isNotEmpty() && state.error == null)
            Box(Modifier.padding(top = if (listShown) 0.dp else top)) {
                when {
                    showSuggestions -> SearchSuggestionsList(
                        suggestions = state.suggestions,
                        contentPadding = contentPadding.withTop(top),
                        onSearch = { state.updateQuery(it); submit() },
                    )
                    showHistory -> SearchHistoryList(
                        history = history,
                        contentPadding = contentPadding.withTop(top),
                        onSearch = { state.query = it; submit() },
                    )
                    state.loading && state.results.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                        MaterialLoader(color = c.primary, size = 36.dp)
                    }
                    state.error != null -> EmptyState(
                        icon = Icons.Rounded.CloudOff,
                        title = "Search didn't load",
                        body = state.error.orEmpty(),
                        action = {
                            PillButton(
                                "Try again",
                                { state.submit() },
                                modifier = Modifier.tvContentEntry("tab:Search", centeredEmptyState = true, priority = 0),
                            )
                        },
                    )
                    state.submitted.isEmpty() -> EmptyState(
                        icon = Icons.Rounded.TravelExplore,
                        title = "Search without the ads",
                        body = "Results play straight away, with no ads and no sign-in.",
                    )
                    state.results.isEmpty() -> EmptyState(
                        icon = Icons.Rounded.History,
                        title = "Nothing found",
                        body = "No videos matched “${state.submitted}”.",
                    )
                    else -> {
                        val list = rememberScrollToTop(scrollToTop)
                        LaunchedEffect(state.generation) { list.scrollToItem(0) }
                        val displayItems = remember(state.results) { groupSearchResults(state.results) }
                        if (isTv) {
                            val rows = remember(displayItems, searchCols) {
                                buildList<List<SearchDisplayItem>> {
                                    val buf = ArrayList<SearchDisplayItem>(searchCols)
                                    for (d in displayItems) {
                                        if (d is SearchDisplayItem.ShortsShelf) {
                                            if (buf.isNotEmpty()) { add(buf.toList()); buf.clear() }
                                            add(listOf(d))
                                        } else {
                                            buf.add(d)
                                            if (buf.size == searchCols) { add(buf.toList()); buf.clear() }
                                        }
                                    }
                                    if (buf.isNotEmpty()) add(buf.toList())
                                }
                            }
                            LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                                itemsIndexed(
                                    items = rows,
                                    key = { _, row ->
                                        when (val first = row.first()) {
                                            is SearchDisplayItem.Normal -> "tv-row-${first.result.id}"
                                            is SearchDisplayItem.ShortsShelf -> first.key
                                        }
                                    }
                                ) { rowIdx, row ->
                                    val first = row.first()
                                    if (first is SearchDisplayItem.ShortsShelf) {
                                        SearchShortsShelf(first.shorts, onOpenShort)
                                    } else {
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            row.forEachIndexed { colIdx, cell ->
                                                if (cell is SearchDisplayItem.Normal) {
                                                    val flatIdx = rowIdx * searchCols + colIdx
                                                    Box(Modifier.weight(1f)) {
                                                        SearchResultCard(
                                                            cell.result,
                                                            onOpen,
                                                            onOpenShort,
                                                            onOpenResult,
                                                            modifier = if (flatIdx < 6) Modifier.tvContentEntry("tab:Search", priority = flatIdx) else Modifier,
                                                        )
                                                    }
                                                }
                                            }
                                            repeat(searchCols - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                                items(
                                    items = displayItems,
                                    key = { item ->
                                        when (item) {
                                            is SearchDisplayItem.Normal -> item.result.id
                                            is SearchDisplayItem.ShortsShelf -> item.key
                                        }
                                    }
                                ) { item ->
                                    when (item) {
                                        is SearchDisplayItem.Normal -> SearchResultCard(item.result, onOpen, onOpenShort, onOpenResult)
                                        is SearchDisplayItem.ShortsShelf -> SearchShortsShelf(item.shorts, onOpenShort)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (filtersOpen) SearchFiltersSheet(
        selected = state.filter,
        onSelected = { filter ->
            state.selectFilter(filter)
            filtersOpen = false
        },
        onDismiss = { filtersOpen = false },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SearchFiltersSheet(
    selected: SearchFilter,
    onSelected: (SearchFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackBlurPopup()
    val c = MaterialTheme.colorScheme
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = popupContainerColor(c.surfaceContainerLow),
        scrimColor = popupScrimColor(),
    ) {
        Column(Modifier.fillMaxWidth().focusGroup().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Search filters", fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text("Choose what to search for", color = c.onSurfaceVariant, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            SearchFilter.entries.forEach { filter ->
                val active = selected == filter
                Row(
                    Modifier.fillMaxWidth().pressable(focusScale = 1.015f, focusShape = RoundedCornerShape(12.dp)) { onSelected(filter) }.padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(filter.label, modifier = Modifier.weight(1f), fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = 16.sp)
                    if (active) Icon(Icons.Rounded.Check, "Selected", tint = c.primary)
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onOpen: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit,
    onOpenResult: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    result.video?.let { video ->
        if (video.isShortVideo) {
            SearchShortsShelf(shorts = listOf(video), onOpenShort = onOpenShort)
        } else {
            VideoCard(video = video, onClick = { onOpen(video) }, modifier = modifier)
        }
        return
    }
    val c = MaterialTheme.colorScheme
    val isTv = LocalIsTv.current
    val cardShape = if (isTv) RoundedCornerShape(12.dp) else RoundedCornerShape(20.dp)
    var cardFocused by remember { mutableStateOf(false) }
    val cardBg = if (isTv && cardFocused) Color(0xFF282828) else if (isTv) Color(0xFF181818) else c.surfaceContainerLow
    val tvCardModifier = if (isTv) {
        Modifier
            .onFocusChanged { cardFocused = it.hasFocus || it.isFocused }
            .pressable(focusScale = 1.02f, focusShape = cardShape, showFocusBorder = false) { onOpenResult(result) }
            .clip(cardShape)
            .background(cardBg)
            .border(if (cardFocused) 2.dp else 1.dp, if (cardFocused) Color.White else Color(0xFF2A2A2A), cardShape)
    } else {
        Modifier
            .pressable(focusScale = 1.02f, focusShape = cardShape) { onOpenResult(result) }
            .clip(cardShape)
            .background(cardBg)
    }
    when (result.kind) {
        SearchResultKind.Channel -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTv) 8.dp else 16.dp, vertical = 6.dp)
                    .then(tvCardModifier)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                ) {
                    Thumbnail(result.thumbnail, Modifier.fillMaxSize(), radius = 0)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isTv && cardFocused) Color.White else c.onSurface,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = result.subtitle.ifBlank { "Channel" },
                        color = c.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (isTv) 8.dp else 16.dp))
                        .background(if (isTv && cardFocused) Color.White else if (isTv) Color(0xFF272727) else c.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "View",
                        color = if (isTv && cardFocused) Color(0xFF0F0F0F) else if (isTv) Color(0xFFF1F1F1) else c.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        SearchResultKind.Collection -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTv) 8.dp else 16.dp, vertical = 6.dp)
                    .then(tvCardModifier)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(if (isTv) 8.dp else 14.dp))
                ) {
                    Thumbnail(result.thumbnail, Modifier.fillMaxSize(), radius = 0)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (result.isAlbum) "ALBUM" else "PLAYLIST",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isTv && cardFocused) Color.White else c.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result.subtitle.ifBlank { if (result.isAlbum) "Album" else "Playlist" },
                        color = c.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = if (isTv && cardFocused) Color.White else c.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        else -> {
            Row(
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTv) 8.dp else 16.dp, vertical = 6.dp)
                    .then(tvCardModifier)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Thumbnail(result.thumbnail, Modifier.size(68.dp), radius = if (isTv) 8 else 16)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(result.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(result.subtitle, color = c.onSurfaceVariant, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun SearchShortsShelf(shorts: List<VideoItem>, onOpenShort: (VideoItem) -> Unit) {
    if (shorts.isEmpty()) return
    val menu = LocalVideoMenu.current
    val isTv = LocalIsTv.current
    val cardShape = RoundedCornerShape(16.dp)
    Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTv) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF0000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Shorts", fontFamily = FontFamily.SansSerif, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("i", color = Color(0xFFFF0000), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Shorts", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (isTv) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shorts, key = { "search-short-${it.id}" }) { short ->
                    TvShortCard(
                        short = short,
                        modifier = Modifier.width(154.dp),
                        onClick = { onOpenShort(short) },
                    )
                }
            }
        } else {
        shorts.chunked(2).forEach { rowShorts ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowShorts.forEach { short ->
                    Box(
                        Modifier.weight(1f)
                            .aspectRatio(9f / 16f)
                            .prefetchOnPress(short.id)
                            .pressable(pressedScale = 0.98f, focusScale = 1.03f, focusShape = cardShape, onLongClick = { menu(short) }, onClick = { onOpenShort(short) })
                            .clip(cardShape),
                    ) {
                        Thumbnail(
                            url = short.shortThumbnail,
                            modifier = Modifier.fillMaxSize(),
                            radius = 0,
                            fallbackUrl = short.thumbnail,
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(0.42f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)),
                            ),
                        )
                        IconButton(
                            onClick = { menu(short) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(34.dp).tvFocusRing(CircleShape),
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Short options", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                        Text(
                            short.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 11.dp),
                        )
                    }
                }
                if (rowShorts.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        }
    }
}

@Composable
private fun SearchSuggestionsList(suggestions: List<String>, contentPadding: PaddingValues, onSearch: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    LazyColumn(contentPadding = contentPadding) {
        items(suggestions, key = { it }) { suggestion ->
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).pressable(focusScale = 1.015f, focusShape = RoundedCornerShape(12.dp)) { onSearch(suggestion) }.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.onSurfaceVariant, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(16.dp))
                Text(suggestion, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SearchHistoryList(history: List<String>, contentPadding: PaddingValues, onSearch: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    LazyColumn(contentPadding = contentPadding) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            ) {
                SectionTitle("Recent searches", Modifier.weight(1f).alignByBaseline())
                Box(Modifier.alignByBaseline()) { TextLink("Clear all") { SearchHistory.clear() } }
            }
        }
        items(history, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .pressable(focusScale = 1.015f, focusShape = RoundedCornerShape(12.dp)) { onSearch(query) }
                    .height(52.dp)
                    .padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.History, null, tint = c.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    query,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier.size(44.dp).pressable(focusShape = CircleShape) { SearchHistory.remove(query) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Remove", tint = c.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
