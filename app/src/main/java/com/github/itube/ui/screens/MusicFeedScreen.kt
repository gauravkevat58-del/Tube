package com.github.itube

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.itube.ui.components.HomeScreenSkeleton
import com.github.itube.ui.components.MaterialLoader
import com.github.itube.ui.tv.LocalIsTv
import com.github.itube.ui.tv.tvContentEntry
import com.github.itube.ui.tv.tvGridColumns

/**
 * YouTube Music feed tab: filters real music videos and music mixes directly from
 * the signed-in user's YouTube Home feed and plays items in the standard video player.
 */
@Composable
fun MusicFeedScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onPlayCollection: (PublicCollection) -> Unit,
    onExploreHome: () -> Unit,
    onSignIn: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val c = MaterialTheme.colorScheme
    val signedIn = Account.signedIn
    val isTv = LocalIsTv.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val gridCols = if (isTv) tvGridColumns(screenWidth, minColumnWidth = 280.dp, maxColumns = 3) else 1

    LaunchedEffect(signedIn) {
        if (signedIn) lib.home.ensureLoaded()
    }

    val musicVideos by remember {
        derivedStateOf { MusicFeedFilter.filterMusicVideos(lib.home.videos) }
    }
    val musicCollections by remember {
        derivedStateOf { MusicFeedFilter.filterMusicCollections(lib.home.collections) }
    }

    var autoPagesFetched by remember(lib.home.generation) { mutableIntStateOf(0) }
    LaunchedEffect(signedIn, lib.home.videos.size, lib.home.loading, lib.home.hasMore) {
        if (signedIn &&
            lib.home.videos.isNotEmpty() &&
            musicVideos.isEmpty() &&
            musicCollections.isEmpty() &&
            lib.home.hasMore &&
            !lib.home.loading &&
            autoPagesFetched < 2
        ) {
            autoPagesFetched++
            lib.home.loadMore()
        }
    }

    CollapsingHeaderLayout(
        resetKey = scrollToTop to lib.home.generation,
        header = {
            ScreenHeader(title = "Music", route = "tab:Music") {
                if (isTv && signedIn) CircleAction(Icons.Rounded.Refresh, "Refresh") { lib.home.refresh() }
                AccountAction(onAccount)
            }
        },
    ) { top ->
        if (!signedIn) {
            LazyColumn(contentPadding = contentPadding.withTop(top)) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.MusicNote,
                        title = "Music from your YouTube feed",
                        body = "Sign in with Google to browse music videos and mixes from your feed.",
                        action = {
                            PillButton(
                                "Sign in with Google",
                                onSignIn,
                                modifier = Modifier.tvContentEntry("tab:Music", centeredEmptyState = true),
                            )
                        },
                    )
                }
            }
            return@CollapsingHeaderLayout
        }

        val list = rememberScrollToTop(scrollToTop)
        LaunchedEffect(lib.home.generation) {
            if (lib.home.generation > 0 && list.firstVisibleItemIndex > 0) {
                list.scrollToItem(0)
            }
        }
        val nearEnd by remember {
            derivedStateOf {
                val last = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= list.layoutInfo.totalItemsCount - 3
            }
        }
        LaunchedEffect(nearEnd, lib.home.videos.size) {
            if (nearEnd && lib.home.videos.isNotEmpty() && musicVideos.isNotEmpty()) {
                lib.home.loadMore()
            }
        }

        PullToRefresh(
            enabled = true,
            refreshing = lib.home.refreshing,
            onRefresh = lib.home::refresh,
            topPadding = top,
        ) {
            LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                if (musicCollections.isNotEmpty()) {
                    item(key = "musicCollections") {
                        MusicCollectionsShelf(
                            collections = musicCollections,
                            entryPriorityOffset = if (musicVideos.isEmpty()) 0 else 6,
                            onPlay = onPlayCollection,
                        )
                    }
                }

                if (musicVideos.isNotEmpty()) {
                    item(key = "musicVideosTitle") {
                        SectionTitle("Music videos")
                    }
                    if (gridCols > 1) {
                        val rows = musicVideos.chunked(gridCols)
                        itemsIndexed(rows, key = { _, row -> "music-row-${row.first().id}" }) { rowIndex, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEachIndexed { colIndex, video ->
                                    val flatIndex = rowIndex * gridCols + colIndex
                                    Box(Modifier.weight(1f)) {
                                        VideoCard(
                                            video = video,
                                            onClick = { onOpen(video) },
                                            modifier = if (flatIndex < 6) Modifier.tvContentEntry("tab:Music", priority = flatIndex) else Modifier,
                                        )
                                    }
                                }
                                repeat(gridCols - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        itemsIndexed(musicVideos, key = { _, video -> "music-video-${video.id}" }) { index, video ->
                            VideoCard(
                                video = video,
                                onClick = { onOpen(video) },
                                modifier = if (index < 6) Modifier.tvContentEntry("tab:Music", priority = index) else Modifier,
                            )
                        }
                    }
                }

                when {
                    lib.home.error != null && musicVideos.isEmpty() && musicCollections.isEmpty() -> item {
                        EmptyState(
                            icon = Icons.Rounded.CloudOff,
                            title = "Couldn't load Music feed",
                            body = lib.home.error.orEmpty(),
                            action = {
                                PillButton(
                                    "Try again",
                                    { lib.home.refresh() },
                                    modifier = Modifier.tvContentEntry("tab:Music", centeredEmptyState = true, priority = 20),
                                )
                            },
                        )
                    }
                    lib.home.loading && musicVideos.isEmpty() && musicCollections.isEmpty() -> item {
                        HomeScreenSkeleton()
                    }
                    musicVideos.isEmpty() && musicCollections.isEmpty() -> item {
                        EmptyState(
                            icon = Icons.Rounded.MusicNote,
                            title = "No music videos in your feed yet",
                            body = "Refresh your feed to check for music videos, or browse Home.",
                            action = {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PillButton(
                                        "Refresh feed",
                                        { lib.home.refresh() },
                                        icon = Icons.Rounded.Refresh,
                                        modifier = Modifier.tvContentEntry("tab:Music", centeredEmptyState = true),
                                    )
                                    PillButton(
                                        "Explore Home",
                                        onExploreHome,
                                    )
                                }
                            },
                        )
                    }
                    lib.home.loading || lib.home.hasMore -> item {
                        Box(Modifier.fillMaxWidth().padding(28.dp), Alignment.Center) {
                            MaterialLoader(color = c.primary, size = 32.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicCollectionsShelf(
    collections: List<PublicCollection>,
    entryPriorityOffset: Int,
    onPlay: (PublicCollection) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 4.dp, bottom = 10.dp)) {
        SectionTitle("Mixes & music playlists")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(collections, key = { _, collection -> "music-collection-${collection.url}" }) { idx, collection ->
                val isMix = collection.subtitle.contains("mix", ignoreCase = true) ||
                    collection.url.substringAfter("list=", "").startsWith("RD")
                val cardShape = RoundedCornerShape(18.dp)
                Column(
                    Modifier
                        .then(if (idx < 3) Modifier.tvContentEntry("tab:Music", priority = entryPriorityOffset + idx) else Modifier)
                        .width(204.dp)
                        .pressable(pressedScale = 0.98f, focusScale = 1.03f, focusShape = cardShape) { onPlay(collection) },
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(122.dp)
                            .clip(cardShape)
                            .border(0.75.dp, c.outlineVariant.copy(alpha = 0.3f), cardShape),
                    ) {
                        Thumbnail(collection.thumbnail, Modifier.fillMaxSize(), radius = 0)
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(0.25f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)),
                            ),
                        )
                        Text(
                            if (isMix) "MIX" else "PLAYLIST",
                            Modifier.align(Alignment.TopStart).padding(9.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 9.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Icon(
                            Icons.Rounded.PlayArrow,
                            "Play ${collection.title}",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(9.dp).size(30.dp)
                                .clip(CircleShape).background(c.primary).padding(5.dp),
                        )
                    }
                    Text(
                        collection.title,
                        Modifier.padding(top = 7.dp),
                        color = c.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (isMix) "Play mix · all tracks" else "Play playlist · all videos",
                        color = c.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
