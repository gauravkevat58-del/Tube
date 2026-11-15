package com.github.itube

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import java.lang.ref.WeakReference
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.blur
import com.github.itube.ui.components.BlurPopupState
import com.github.itube.ui.components.BlurSetting
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.github.itube.ui.components.EqualizerSheet
import com.github.itube.ui.player.SleepTimerSheet
import com.github.itube.ui.tv.TvHelper
import com.github.itube.ui.tv.TvNavRail
import com.github.itube.ui.tv.tvFocusRing

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var openDownloadsRequest by mutableStateOf(0)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // One window at a time (one player, one video texture): a newer one closes the one before.
        // That's a bubble opened while the app is open, or the app opened from Home while it sits
        // in a bubble (the bubble then goes away). Its task goes too, so Recents keeps no stale card;
        // PlaybackService.onTaskRemoved sees this window still open and plays on.
        current?.get()?.takeIf { it !== this && !it.isFinishing }?.finishAndRemoveTask()
        current = WeakReference(this)
        enableEdgeToEdge()
        // The media notification (and its lock-screen controls) needs this on Android 13+.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        NewVideos.takeVideo(intent)?.let { NewVideos.opening = it }
        handleDownloadsIntent(intent)
        setContent { AirStreamTheme { AirStreamApp() } }
    }

    /** A new-video notification tapped while this window is open (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NewVideos.takeVideo(intent)?.let { NewVideos.opening = it }
        handleDownloadsIntent(intent)
    }

    private fun handleDownloadsIntent(intent: Intent) {
        if (intent.action == ACTION_OPEN_DOWNLOADS) {
            openDownloadsRequest++
            setIntent(Intent(intent).setAction(null))
        }
    }

    override fun onStart() {
        super.onStart()
        AppUpdate.checkIfDue()
    }

    override fun onResume() {
        super.onResume()
        AppUpdate.onWindowResumed(this)
    }

    override fun onDestroy() {
        if (current?.get() === this) current = null
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                android.view.KeyEvent.KEYCODE_HEADSETHOOK -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.togglePlay()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.controller?.play()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.controller?.pause()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.seekBy(10_000)
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.seekBy(-10_000)
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_MEDIA_STOP -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.controller?.pause()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.playNextUp()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    val p = PlayerState.activeInstance
                    if (p?.current != null) {
                        p.playPrevious()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        const val ACTION_OPEN_DOWNLOADS = "com.github.itube.action.OPEN_DOWNLOADS"
        private var current: WeakReference<MainActivity>? = null

        /** The task of the app's open window; null when there's none (closed, or on its way out). */
        fun openTaskId(): Int? = current?.get()?.takeUnless { it.isFinishing || it.isDestroyed }?.taskId
    }
}

enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Music("Music", Icons.Outlined.MusicNote, Icons.Rounded.MusicNote),
    Subscriptions("Subs", Icons.Outlined.Subscriptions, Icons.Rounded.Subscriptions),
    Shorts("Shorts", Icons.Outlined.SlowMotionVideo, Icons.Rounded.SlowMotionVideo),
    Search("Search", Icons.Rounded.Search, Icons.Rounded.Search),
    Library("Library", Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary),
}

// Floating mini video: a rounded landscape tile at the lower trailing corner.
private val MiniWidth = 184.dp
private val MiniGap = 8.dp

@Composable
fun AirStreamApp() {
    val c = MaterialTheme.colorScheme
    val hostActivity = LocalContext.current as? MainActivity
    val openDownloadsRequest = hostActivity?.openDownloadsRequest ?: 0
    val player = rememberPlayerState()
    val scope = rememberCoroutineScope()
    val search = remember { SearchState(scope) }
    val lib = remember { LibraryData(scope) }
    var signingIn by remember { mutableStateOf(false) }
    var accountSheet by remember { mutableStateOf(false) }
    // A release can arrive after the first composition because the launch check is asynchronous.
    // Show its GitHub notes once per app launch, then keep the same sheet available in Settings.
    var updateNotesShownFor by rememberSaveable { mutableStateOf<String?>(null) }
    var automaticUpdateNotes by remember { mutableStateOf(false) }
    val availableUpdate = AppUpdate.available
    LaunchedEffect(availableUpdate?.version) {
        val version = availableUpdate?.version
        if (version != null && updateNotesShownFor != version) {
            updateNotesShownFor = version
            automaticUpdateNotes = true
        }
    }
    var openPlaylist by remember { mutableStateOf<PlaylistRef?>(null) }
    var downloadsOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var openChannel by remember { mutableStateOf<String?>(null) }
    var openPublicCollection by remember { mutableStateOf<PublicCollection?>(null) }
    // Kept after closing so the page can slide out with its content.
    var channelState by remember { mutableStateOf<ChannelState?>(null) }
    var publicCollectionState by remember { mutableStateOf<PublicCollectionState?>(null) }
    val playlistFeed = remember(openPlaylist?.id) { openPlaylist?.let { lib.playlistFeed(it.id) } }
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    LaunchedEffect(openDownloadsRequest) {
        if (openDownloadsRequest > 0) {
            openPlaylist = null
            tab = Tab.Library
            downloadsOpen = true
        }
    }
    // Shorts: its own feed and players; ‹ (and Back) return to the tab it was opened from.
    // One per process (AirStreamApplication), so its bank keeps filling with no window open.
    val shorts = (LocalContext.current.applicationContext as AirStreamApplication).shorts
    DisposableEffect(shorts) { onDispose { shorts.leave() } }
    var shortsFrom by rememberSaveable { mutableStateOf(Tab.Home) }
    // A new-video notification's tap: that video opens in the player (leaving Shorts, as a channel page's video does).
    LaunchedEffect(NewVideos.opening) {
        val video = NewVideos.opening ?: return@LaunchedEffect
        NewVideos.opening = null
        if (tab == Tab.Shorts) tab = shortsFrom
        player.open(video)
    }

    LaunchedEffect(player) {
        SleepTimerState.onTimerExpired = {
            player.controller?.pause()
            player.actionMessage = "Sleep timer: playback paused"
        }
    }

    // Bumped when the already-selected tab is tapped; that tab's list scrolls to the top.
    val scrollToTop = remember { mutableStateMapOf<Tab, Int>() }
    val reduced = rememberReducedMotion()
    val morph = remember { PlayerMorph(scope) { expanded -> player.expanded = expanded } }

    // The screen stays on while a video plays (full player, mini-player or fullscreen);
    // paused, the normal timeout applies again. Only affects the visible window.
    val rootView = LocalView.current
    DisposableEffect(player.isPlaying, shorts.playing) {
        rootView.keepScreenOn = player.isPlaying || shorts.playing
        onDispose { rootView.keepScreenOn = false }
    }

    val isTv = com.github.itube.ui.tv.rememberIsTv()
    // Opening the player drops any text focus (the search box behind it) on touch screens,
    // while TV keeps focus inside the active player hierarchy.
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(player.expanded, isTv) {
        if (player.expanded) {
            if (!isTv) focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    // Off screen (app in background, screen locked): audio only.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> { player.setBackground(true); shorts.setForeground(false) }
                androidx.lifecycle.Lifecycle.Event.ON_START -> { player.setBackground(false); shorts.setForeground(true) }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // As in Castii: at least 8 dp off the bottom, and extra clearance above a three-button bar.
    val stackBottom = navBottom.coerceAtLeast(8.dp) +
        if (WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding() > 0.dp) 10.dp else 0.dp
    var optionsFor by remember { mutableStateOf<VideoItem?>(null) }
    var optionsPlaylistFor by remember { mutableStateOf<PlaylistRef?>(null) }
    var playlistOptionsFor by remember { mutableStateOf<PlaylistRef?>(null) }
    var collectionOptionsFor by remember { mutableStateOf<PublicCollection?>(null) }
    val playerOpen = player.current != null && player.expanded
    // On Shorts the app's video waits, paused, out of sight (no mini-player).
    val hasMini = player.current != null && tab != Tab.Shorts
    // Lists scroll fully clear of the floating nav (and mini-player when present).
    val contentPadding = if (isTv) {
        PaddingValues(
            start = 12.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = stackBottom + (if (hasMini) MiniHeight + MiniGap else 0.dp) + 20.dp,
        )
    } else {
        PaddingValues(
            bottom = stackBottom + NavHeight + (if (hasMini) MiniHeight + MiniGap else 0.dp) + 16.dp,
        )
    }

    // Fullscreen is whenever the open player is shown in landscape (rotation or button).
    val activity = LocalContext.current as Activity
    val isTvDevice = remember(activity) { TvHelper.isTvDevice(activity) }
    val orientation = rememberOrientationController(activity as ComponentActivity)
    val tvFocusBridge = remember { com.github.itube.ui.tv.TvFocusBridge() }
    var showExitDialog by remember { mutableStateOf(false) }

    fun openVideo(v: VideoItem) {
        player.open(v)
        if (isTv) {
            player.expanded = true
            orientation.enterFullscreen(vertical = false)
        }
    }
    // In split screen this is the pane's shape, not the phone's.
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Audio only has no picture to go fullscreen with: the open player stays portrait.
    orientation.Bind(playerOpen && !AudioOnly.enabled, landscape)
    val pip = remember(activity) { PipController(activity) }
    // Closing the picture-in-picture window pauses (expanding it just comes back).
    pip.Bind(onClosed = { player.controller?.pause() })
    // The PiP window can be any shape: hold whatever was showing when it opened.
    val fullscreenBeforePip = remember { mutableStateOf(false) }
    val showFullscreen = if (pip.active) fullscreenBeforePip.value else orientation.showsFullscreen(playerOpen, landscape)
    SideEffect { if (!pip.active) fullscreenBeforePip.value = showFullscreen }
    LaunchedEffect(showFullscreen) { player.fullscreen = showFullscreen }
    SystemBarsHidden(showFullscreen)
    // A vertical video's fullscreen stays upright (Fullscreen.kt).
    fun toggleFullscreen() = if (showFullscreen) orientation.exitFullscreen() else orientation.enterFullscreen(vertical = player.videoAspect < 1f)
    // Pulling down from the details area always enters Flow-style portrait fullscreen.
    fun enterPortraitFullscreen() = orientation.enterFullscreen(vertical = true)

    BackHandler(enabled = player.expanded && player.sheet == null && player.playlistTarget == null) {
        player.close()
        if (isTv) tvFocusBridge.focusContent()
    }
    BackHandler(enabled = !player.expanded && tab != Tab.Home && openPlaylist == null) { tab = if (tab == Tab.Shorts) shortsFrom else Tab.Home }
    BackHandler(enabled = !player.expanded && openPlaylist != null) { openPlaylist = null }
    BackHandler(enabled = !player.expanded && downloadsOpen) { downloadsOpen = false }
    BackHandler(enabled = !player.expanded && historyOpen) { historyOpen = false }
    BackHandler(enabled = !player.expanded && openPublicCollection != null) { openPublicCollection = null }
    BackHandler(enabled = signingIn) { signingIn = false }
    BackHandler(enabled = !player.expanded && openChannel != null) { openChannel = null }
    BackHandler(enabled = tab == Tab.Shorts && shorts.commentsOpen) { shorts.commentsOpen = false }
    // Registered last so it wins: in fullscreen on phone, Back returns to portrait first (which then
    // stops and closes on the next Back); on Android TV, leaving the video player stops playback and closes.
    BackHandler(enabled = showFullscreen && player.sheet == null && player.playlistTarget == null) {
        orientation.exitFullscreen()
        if (isTvDevice) {
            player.close()
            tvFocusBridge.focusContent()
        }
    }

    // The open player's bar (PlayerBar.kt): its tools card / quality list close on Back, on a
    // tap anywhere else, and whenever the player stops being the open portrait page.
    val playerBar = remember { PlayerBarState(scope) }
    BackHandler(enabled = playerBar.isOpen && playerOpen && !showFullscreen) { playerBar.closeAll() }
    LaunchedEffect(playerOpen, showFullscreen, player.sheet) {
        if (!playerOpen || showFullscreen || player.sheet != null) playerBar.closeAll()
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current

    // Shorts (Shorts.kt) play on their own players: the app's video pauses on the way in and
    // carries on, from the same spot in the mini-player, on the way out (Derek, 2026-10-28).
    // One progress turns the tab bar into the glass bar: 620 ms in, 460 ms out, both moving at once.
    val shortsMorph = remember { androidx.compose.animation.core.Animatable(if (tab == Tab.Shorts) 1f else 0f) }
    var resumeMain by remember { mutableStateOf(false) }
    LaunchedEffect(tab) {
        if (tab == Tab.Shorts) {
            resumeMain = resumeMain || player.isPlaying
            player.controller?.pause()
            player.setVideoAside(true)
            shorts.enter()
        } else if (shorts.active) {
            shorts.leave()
            player.setVideoAside(false)
            if (resumeMain) player.controller?.play()
            resumeMain = false
        }
    }
    // Shorts turned off (account sheet) while it was the tab: back to where it was opened from.
    LaunchedEffect(ShortsSetting.enabled) { if (!ShortsSetting.enabled && tab == Tab.Shorts) tab = shortsFrom }
    LaunchedEffect(tab == Tab.Shorts) {
        if (tab == Tab.Shorts) shortsMorph.animateTo(1f, if (reduced) snap() else tween(ShortsEnterMs, easing = androidx.compose.animation.core.LinearEasing))
        else shortsMorph.animateTo(0f, if (reduced) snap() else tween(ShortsExitMs, easing = ShortsExitEasing))
    }
    val shortsShown by remember { derivedStateOf { shortsMorph.value > 0f } }
    SideEffect {
        StatusBarOverVideo.shorts = tab == Tab.Shorts && openChannel == null
        shorts.onMessage = { player.actionMessage = it }
        shorts.onSavedShortsChanged = { lib.loadPlaylists() }
        shorts.mainBusy = { player.isBuffering }
    }

    // Taps, back and opening a video set `expanded`; this turns that into the morph.
    // Gesture releases settle themselves and are left alone (see PlayerMorph.animateTo).
    LaunchedEffect(player.expanded, player.current?.id) {
        if (player.current == null) morph.progress.snapTo(0f) else morph.animateTo(player.expanded, reduced)
    }
    val showPlayer by remember {
        // Held until the morph has completely finished, so the video changes hands once.
        derivedStateOf { player.current != null && (player.expanded || morph.dragging || morph.progress.isRunning || morph.progress.value > 0f) }
    }

    fun showChannel(url: String) {
        // From the player: minimize it so the channel page is visible.
        player.expanded = false
        if (channelState?.url != url) channelState = ChannelState(url, scope)
        openChannel = url
    }
    fun showPublicCollection(collection: PublicCollection) {
        player.expanded = false
        if (publicCollectionState?.collection?.url != collection.url) publicCollectionState = PublicCollectionState(collection, scope)
        openPublicCollection = collection
    }
    // A channel opened from Shorts sits over the short, which waits paused.
    SideEffect { shorts.cover(tab == Tab.Shorts && openChannel != null) }
    BackHandler(enabled = tab == Tab.Shorts && shorts.channelMenu) { shorts.channelMenu = false }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.surface)) {
        val rootConstraints = constraints
        val rootMaxWidth = maxWidth
        val rootMaxHeight = maxHeight
        // The floating player can be moved freely; its position is also used as the mini end
        // of the player morph, so expanding/collapsing always lands on the visible tile.
        var miniOffset by remember { mutableStateOf(Offset.Zero) }
        var tuckedEdge by remember { mutableStateOf<Int?>(null) } // -1 = left, 1 = right
        val stageFloor = maxOf(StageMinAspect, constraints.maxWidth / (constraints.maxHeight * StageMaxHeightShare))
        SideEffect { player.stageFloor = stageFloor }
        // Both ends of the morph, from the same constants the mini-player and player use.
        val stageAspect = player.stageAspect
        val geometry = remember(constraints.maxWidth, constraints.maxHeight, stackBottom, statusTop, density, stageAspect, miniOffset) {
            with(density) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val miniBottom = h - (stackBottom + NavHeight + MiniGap).toPx() + miniOffset.y
                val miniW = minOf(w - 32.dp.toPx(), MiniWidth.toPx())
                val miniH = miniW * 9f / 16f
                val miniLeft = w - miniW - 16.dp.toPx() + miniOffset.x
                val miniCard = Rect(miniLeft, miniBottom - miniH, miniLeft + miniW, miniBottom)
                val thumb = miniCard
                val side = StageSideGap.toPx()
                val stageTop = (statusTop + StageTopGap).toPx()
                val stage = Rect(side, stageTop, w - side, stageTop + (w - 2 * side) / stageAspect)
                MorphGeometry(
                    screen = Size(w, h),
                    miniCard = miniCard,
                    miniThumb = thumb,
                    stage = stage,
                    miniRadius = MiniRadius.toPx(),
                    thumbRadius = MiniThumbRadius.toPx(),
                    stageRadius = StageRadius.toPx(),
                )
            }
        }
        SideEffect { morph.geometry = geometry }

        // One video surface that moves between player, fullscreen and mini-player (never two
        // at once), so the picture is never rebuilt and never blinks back to the thumbnail.
        val sharedSurface = remember(player) {
            androidx.compose.runtime.movableContentOf { m: Modifier -> VideoSurface(player.controller, m) }
        }
        val isAtRootHome = tab == Tab.Home &&
            !player.expanded &&
            !showFullscreen &&
            openPlaylist == null &&
            !downloadsOpen &&
            !historyOpen &&
            openChannel == null &&
            openPublicCollection == null &&
            !signingIn &&
            !accountSheet &&
            optionsFor == null &&
            playlistOptionsFor == null &&
            collectionOptionsFor == null &&
            player.playlistTarget == null &&
            player.sheet == null &&
            !playerBar.isOpen &&
            CrashReports.pendingReport == null &&
            !(automaticUpdateNotes && availableUpdate != null) &&
            !showExitDialog
        BackHandler(enabled = isAtRootHome) { showExitDialog = true }

        val activeTvRoute = when {
            openPublicCollection != null -> "overlay:collection"
            openChannel != null -> "overlay:channel"
            historyOpen -> "overlay:history"
            downloadsOpen -> "overlay:downloads"
            openPlaylist != null -> "overlay:playlist"
            else -> "tab:${tab.name}"
        }
        SideEffect { tvFocusBridge.activeRoute = activeTvRoute }
        val backgroundInteractive = !playerOpen &&
            !showFullscreen &&
            !showExitDialog &&
            !accountSheet &&
            !signingIn &&
            optionsFor == null &&
            playlistOptionsFor == null &&
            collectionOptionsFor == null &&
            player.sheet == null &&
            player.playlistTarget == null &&
            CrashReports.pendingReport == null &&
            !(automaticUpdateNotes && availableUpdate != null)

        var mainWindowHasFocus by remember { mutableStateOf(false) }
        var previousTvRoute by remember { mutableStateOf<String?>(null) }
        var wasBackgroundInteractive by remember { mutableStateOf(backgroundInteractive) }
        LaunchedEffect(isTv, activeTvRoute, backgroundInteractive) {
            val prevRoute = previousTvRoute
            val prevInteractive = wasBackgroundInteractive
            previousTvRoute = activeTvRoute
            wasBackgroundInteractive = backgroundInteractive
            if (!isTv || !backgroundInteractive) return@LaunchedEffect
            val routeTransition = prevRoute == null ||
                (prevRoute != activeTvRoute && (activeTvRoute.startsWith("overlay:") || prevRoute.startsWith("overlay:")))
            val recoveredFromModal = !prevInteractive
            if (routeTransition || recoveredFromModal) {
                kotlinx.coroutines.delay(55)
                tvFocusBridge.focusContentOrHeaderOrRail()
            }
        }
        LaunchedEffect(isTv, mainWindowHasFocus, backgroundInteractive, activeTvRoute, playerOpen, showFullscreen) {
            if (!isTv || !backgroundInteractive || playerOpen || showFullscreen || mainWindowHasFocus) {
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(70)
            if (!mainWindowHasFocus && backgroundInteractive && !playerOpen && !showFullscreen) {
                tvFocusBridge.focusContentOrHeaderOrRail()
            }
        }

        CompositionLocalProvider(
            com.github.itube.ui.tv.LocalIsTv provides isTv,
            com.github.itube.ui.tv.LocalTvFocusBridge provides (if (isTv) tvFocusBridge else null),
            // In picture-in-picture only the PiP view below shows the video.
            LocalVideoSurface provides if (pip.active) NoVideoSurface else sharedSurface,
            LocalPictureInPicture provides pip,
            LocalVideoMenu provides { optionsFor = it },
            LocalPlaylistMenu provides { playlistOptionsFor = it },
            LocalChannelOpener provides { url -> showChannel(url) },
            LocalCollectionOpener provides { collection -> showPublicCollection(collection) },
            LocalCollectionMenu provides { collection -> collectionOptionsFor = collection },
        ) {
        val popupBlurRadius by animateDpAsState(
            targetValue = if (BlurPopupState.isBlurActive) 22.dp else 0.dp,
            animationSpec = tween(260, easing = Motion.Emphasized),
            label = "popupBlurRadius",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { mainWindowHasFocus = it.hasFocus }
                .then(if (popupBlurRadius > 0.dp) Modifier.blur(popupBlurRadius) else Modifier),
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = backgroundInteractive }
        ) {
        fun playCollection(collection: PublicCollection) {
            scope.launch {
                player.actionMessage = "Loading ${collection.title}…"
                runCatching { YouTube.publicCollection(collection.url, collection.isAlbum).videos }
                    .onSuccess { videos ->
                        if (videos.isEmpty()) player.actionMessage = "This playlist has no playable videos."
                        else player.openPlaylist(videos, 0)
                    }
                    .onFailure { player.actionMessage = "Couldn't load ${collection.title}." }
            }
        }
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.padding(
                start = if (isTv && tab != Tab.Shorts && !playerOpen && !showFullscreen) com.github.itube.ui.tv.TvRailWidth else 0.dp,
            ),
            transitionSpec = {
                if (reduced) fadeIn(snap()) togetherWith fadeOut(snap())
                else (fadeIn(tween(Motion.PageEnterMs, easing = Motion.Emphasized)) +
                    slideInVertically(tween(Motion.PageRiseMs, easing = Motion.Emphasized)) { (it / 35).coerceIn(8, 24) }) togetherWith
                    fadeOut(tween(Motion.PageExitMs))
            },
            label = "tabs",
        ) { t ->
            when (t) {
                Tab.Home -> HomeScreen(
                    lib, shorts, contentPadding, onOpen = ::openVideo,
                    onOpenShort = { short ->
                        shorts.openFromHome(short.id)
                        tab = Tab.Shorts
                    },
                    onPlayCollection = ::playCollection,
                    onSearch = { tab = Tab.Search },
                    onSignIn = { signingIn = true }, onAccount = { accountSheet = true },
                    scrollToTop = scrollToTop[Tab.Home] ?: 0,
                )
                Tab.Music -> MusicFeedScreen(
                    lib = lib,
                    contentPadding = contentPadding,
                    onOpen = ::openVideo,
                    onPlayCollection = ::playCollection,
                    onExploreHome = { tab = Tab.Home },
                    onSignIn = { signingIn = true },
                    onAccount = { accountSheet = true },
                    scrollToTop = scrollToTop[Tab.Music] ?: 0,
                )
                Tab.Subscriptions -> SubscriptionsScreen(
                    lib, contentPadding, onOpen = ::openVideo,
                    onSignIn = { signingIn = true }, onAccount = { accountSheet = true },
                    scrollToTop = scrollToTop[Tab.Subscriptions] ?: 0,
                )
                Tab.Shorts -> ShortsScreen(shorts, { shortsMorph.value }, overlayBottom = stackBottom + (if (isTv) 16.dp else GlassHeight + 16.dp))
                Tab.Search -> SearchScreen(
                    state = search,
                    contentPadding = contentPadding,
                    onOpen = { video ->
                        if (video.isShortVideo) {
                            if (tab != Tab.Shorts) shortsFrom = tab
                            shorts.openExternal(video)
                            tab = Tab.Shorts
                        } else {
                            player.setAudioOnly(false)
                            openVideo(video)
                        }
                    },
                    onOpenShort = { short ->
                        if (tab != Tab.Shorts) shortsFrom = tab
                        shorts.openExternal(short)
                        tab = Tab.Shorts
                    },
                    onOpenResult = { result ->
                        when (result.kind) {
                            SearchResultKind.Video -> result.video?.let { video ->
                                if (video.isShortVideo) {
                                    if (tab != Tab.Shorts) shortsFrom = tab
                                    shorts.openExternal(video)
                                    tab = Tab.Shorts
                                } else {
                                    player.setAudioOnly(false)
                                    openVideo(video)
                                }
                            }
                            SearchResultKind.Channel -> showChannel(result.url)
                            SearchResultKind.Collection -> showPublicCollection(
                                PublicCollection(result.url, result.title, result.subtitle, result.thumbnail, result.isAlbum)
                            )
                        }
                    },
                    onSettings = { accountSheet = true },
                    scrollToTop = scrollToTop[t] ?: 0,
                )
                Tab.Library -> LibraryScreen(
                    lib = lib,
                    contentPadding = contentPadding,
                    onOpen = ::openVideo,
                    onOpenShort = { short ->
                        if (tab != Tab.Shorts) shortsFrom = tab
                        shorts.openExternal(short)
                        tab = Tab.Shorts
                    },
                    onOpenHistory = { historyOpen = true },
                    onOpenPlaylist = { openPlaylist = it },
                    onOpenDownloads = { downloadsOpen = true },
                    onOpenCollection = ::showPublicCollection,
                    onSignIn = { signingIn = true },
                    onAccount = { accountSheet = true },
                    scrollToTop = scrollToTop[Tab.Library] ?: 0,
                )
            }
        }

        // A playlist opens as a page over the tabs (slides in from the right).
        AnimatedVisibility(
            visible = openPlaylist != null,
            enter = if (reduced) fadeIn(snap()) else slideInHorizontally(tween(340, easing = Motion.Emphasized)) { it } + fadeIn(tween(120)),
            exit = if (reduced) fadeOut(snap()) else slideOutHorizontally(tween(300, easing = Motion.Emphasized)) { it } + fadeOut(tween(220)),
        ) {
            val pl = openPlaylist
            val feed = playlistFeed
            if (pl != null && feed != null) {
                Box(Modifier.fillMaxSize().padding(start = if (isTv && !playerOpen && !showFullscreen) com.github.itube.ui.tv.TvRailWidth else 0.dp)) {
                PlaylistScreen(
                    playlist = pl,
                    feed = feed,
                    contentPadding = contentPadding,
                    onBack = { openPlaylist = null },
                    onOpen = ::openVideo,
                    onOpenShort = { short ->
                        if (tab != Tab.Shorts) shortsFrom = tab
                        shorts.openExternal(short)
                        openPlaylist = null
                        tab = Tab.Shorts
                    },
                    onPlayPlaylist = { videos, index -> player.openPlaylist(videos, index) },
                    onVideoLongPress = { video ->
                        optionsFor = video
                        optionsPlaylistFor = pl.takeIf { selected -> feed.videos.any { it.id == video.id } }
                    },
                )
                }
            }
        }

        AnimatedVisibility(
            visible = downloadsOpen,
            enter = if (reduced) fadeIn(snap()) else slideInHorizontally(tween(340, easing = Motion.Emphasized)) { it } + fadeIn(tween(120)),
            exit = if (reduced) fadeOut(snap()) else slideOutHorizontally(tween(300, easing = Motion.Emphasized)) { it } + fadeOut(tween(220)),
        ) {
            Box(Modifier.fillMaxSize().padding(start = if (isTv && !playerOpen && !showFullscreen) com.github.itube.ui.tv.TvRailWidth else 0.dp)) {
            DownloadsScreen(
                contentPadding = contentPadding,
                onBack = { downloadsOpen = false },
                onOpen = { download ->
                    downloadsOpen = false
                    player.openOffline(download)
                },
            )
            }
        }

        AnimatedVisibility(
            visible = historyOpen,
            enter = if (reduced) fadeIn(snap()) else slideInHorizontally(tween(340, easing = Motion.Emphasized)) { it } + fadeIn(tween(120)),
            exit = if (reduced) fadeOut(snap()) else slideOutHorizontally(tween(300, easing = Motion.Emphasized)) { it } + fadeOut(tween(220)),
        ) {
            Box(Modifier.fillMaxSize().padding(start = if (isTv && !playerOpen && !showFullscreen) com.github.itube.ui.tv.TvRailWidth else 0.dp)) {
            HistoryScreen(
                lib = lib,
                contentPadding = contentPadding,
                onBack = { historyOpen = false },
                onOpen = ::openVideo,
                onOpenShort = { short ->
                    if (tab != Tab.Shorts) shortsFrom = tab
                    shorts.openExternal(short)
                    historyOpen = false
                    tab = Tab.Shorts
                },
            )
            }
        }

        // A channel opens the same way, over everything below the mini-player.
        AnimatedVisibility(
            visible = openChannel != null,
            enter = if (reduced) fadeIn(snap()) else slideInHorizontally(tween(340, easing = Motion.Emphasized)) { it } + fadeIn(tween(120)),
            exit = if (reduced) fadeOut(snap()) else slideOutHorizontally(tween(300, easing = Motion.Emphasized)) { it } + fadeOut(tween(220)),
        ) {
            channelState?.let { ch ->
                Box(Modifier.fillMaxSize().padding(start = if (isTv && !playerOpen && !showFullscreen) com.github.itube.ui.tv.TvRailWidth else 0.dp)) {
                ChannelScreen(
                    ch, contentPadding, onBack = { openChannel = null },
                    // A video from a channel opened in Shorts plays in the app's player: Shorts closes.
                    onOpen = { v -> if (tab == Tab.Shorts) tab = shortsFrom; openVideo(v) },
                    onOpenShort = { short ->
                        if (tab != Tab.Shorts) shortsFrom = tab
                        shorts.openExternal(short)
                        openChannel = null
                        tab = Tab.Shorts
                    },
                    onOpenCollection = ::showPublicCollection,
                    onMessage = { player.actionMessage = it },
                )
                }
            }
        }

        // Public playlists and albums from search/channel pages are separate from the signed-in Library.
        AnimatedVisibility(
            visible = openPublicCollection != null,
            enter = if (reduced) fadeIn(snap()) else slideInHorizontally(tween(340, easing = Motion.Emphasized)) { it } + fadeIn(tween(120)),
            exit = if (reduced) fadeOut(snap()) else slideOutHorizontally(tween(300, easing = Motion.Emphasized)) { it } + fadeOut(tween(220)),
        ) {
            publicCollectionState?.let { collection ->
                Box(Modifier.fillMaxSize().padding(start = if (isTv && !playerOpen && !showFullscreen) com.github.itube.ui.tv.TvRailWidth else 0.dp)) {
                PublicCollectionScreen(
                    state = collection,
                    contentPadding = contentPadding,
                    onBack = { openPublicCollection = null },
                    onOpen = { v ->
                        player.setAudioOnly(false)
                        openVideo(v)
                    },
                    onOpenPlaylist = { videos, index ->
                        player.setAudioOnly(false)
                        player.openPlaylist(videos, index)
                    },
                    onOpenChannel = ::showChannel,
                )
                }
            }
        }

        // Ledgii's bottom vignette: the page color fades in over the last 130 dp (plus the
        // system bar), so content dissolves behind the mini-player and tab bar instead of
        // running hard into them. Drawn only — it never intercepts touches.
        val pageColor = c.surface
        val vignette = remember(pageColor) { vignetteBrush(pageColor, opaqueAtTop = false) }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(navBottom + 130.dp)
                // Not over a short: the video runs to the screen's edge.
                .graphicsLayer { alpha = 1f - shortsMorph.value }
                .drawBehind { drawRect(vignette) },
        )
        // Swiping home from here never scrolls the page (the tab bar above still wins its taps).
        BottomGestureGuard(Modifier.align(Alignment.BottomCenter))

        if (hasMini) {
            val miniBottomPad = stackBottom + (if (isTv) 12.dp else NavHeight + MiniGap)
            val widthPx = rootConstraints.maxWidth.toFloat()
            val heightPx = rootConstraints.maxHeight.toFloat()
            val miniWidthPx = with(density) { minOf(rootMaxWidth - 32.dp, MiniWidth).toPx() }
            val miniHeightPx = miniWidthPx * 9f / 16f
            val baseLeft = widthPx - miniWidthPx - with(density) { 16.dp.toPx() }
            val baseTop = heightPx - with(density) { miniBottomPad.toPx() } - miniHeightPx
            val visibleHandlePx = with(density) { 30.dp.toPx() }
            val topLimitPx = with(density) { statusTop.toPx() }
            val bottomLimitPx = heightPx - with(density) { miniBottomPad.toPx() } - miniHeightPx
            MiniPlayer(
                player,
                onExpand = { player.expanded = true },
                showVideo = !showPlayer && !showFullscreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = miniBottomPad)
                    .widthIn(max = MiniWidth).aspectRatio(16f / 9f)
                    .offset { IntOffset(miniOffset.x.roundToInt(), miniOffset.y.roundToInt()) }
                    // A drag moves the tile. A tap remains a separate action that opens the
                    // full player; dragging to either edge tucks the tile behind an arrow handle.
                    .graphicsLayer {
                        // Hidden for the whole morph: the shrinking page draws the mini-player
                        // itself (it stays touchable underneath, so taps still work mid-collapse).
                        alpha = if (showPlayer) 0f else 1f
                    }
                    .pointerInput(player.current?.id, widthPx, heightPx) {
                        detectDragGestures(
                            onDragStart = { tuckedEdge = null },
                            onDrag = { change, drag ->
                                change.consume()
                                val minX = -baseLeft + visibleHandlePx
                                val maxX = widthPx - baseLeft - visibleHandlePx
                                val minY = topLimitPx - baseTop
                                val maxY = bottomLimitPx - baseTop
                                miniOffset = Offset(
                                    (miniOffset.x + drag.x).coerceIn(minX, maxX),
                                    (miniOffset.y + drag.y).coerceIn(minY, maxY),
                                )
                            },
                            onDragEnd = {
                                val tileLeft = baseLeft + miniOffset.x
                                val leftTuck = tileLeft < visibleHandlePx
                                val rightTuck = tileLeft + miniWidthPx > widthPx - visibleHandlePx
                                tuckedEdge = when {
                                    leftTuck -> -1
                                    rightTuck -> 1
                                    else -> null
                                }
                                if (tuckedEdge == -1) miniOffset = miniOffset.copy(x = -baseLeft - miniWidthPx + visibleHandlePx)
                                if (tuckedEdge == 1) miniOffset = miniOffset.copy(x = widthPx - visibleHandlePx - baseLeft)
                            },
                        )
                    },
            )
            tuckedEdge?.let { edge ->
                val handleLeft = if (edge < 0) 0f else widthPx - visibleHandlePx
                val handleTop = (baseTop + miniOffset.y).coerceIn(topLimitPx, bottomLimitPx)
                val handleShape = if (edge < 0) RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp) else RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                Box(
                    Modifier
                        .offset { IntOffset(handleLeft.roundToInt(), handleTop.roundToInt()) }
                        .size(width = with(density) { visibleHandlePx.toDp() }, height = with(density) { miniHeightPx.toDp() })
                        .pressable(focusShape = handleShape) {
                            miniOffset = Offset.Zero
                            tuckedEdge = null
                        }
                        .clip(handleShape)
                        .background(Color.Black.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (edge < 0) Icons.Rounded.ArrowForward else Icons.Rounded.ArrowBack,
                        contentDescription = "Restore mini player",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        }

        // The player morphs out of (and back into) the mini-player; see PlayerMorph.
        if (showPlayer && !showFullscreen) {
            PlayerScreen(
                    player, morph,
                    onOpen = player::open,
                    onCollapse = { player.expanded = false },
                    onFullscreen = { toggleFullscreen() },
                    onPortraitFullscreen = { enterPortraitFullscreen() },
                    // With the controls on the video there's no bar over the player (it steps down below).
                    bottomClearance = if (PlayerControls.inBar && !isTv) stackBottom + NavHeight + PlayerBarRaise + 16.dp else stackBottom + 16.dp,
                )
        }

        // Over the player: while it's open the nav pill is the player's own bar (NavBar.kt).
        // It steps down out of the way of the comments panel (its composer sits there).
        val barAside by animateFloatAsState(
            if (player.sheet.isPanel && playerOpen) 1f else 0f,
            if (reduced) snap() else tween(280, easing = Motion.Emphasized), label = "barAside",
        )
        // A tap anywhere outside the open card / list closes it (and goes no further).
        if (playerBar.isOpen && playerOpen) {
            Box(
                Modifier.fillMaxSize().pointerInput(playerBar) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        playerBar.closeAll()
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                },
            )
        }
        if (!isTv) {
            NavPill(
                selected = tab,
                onSelect = { selected ->
                    when {
                        // Re-tapping the current tab: back out of a playlist, else scroll to top.
                        selected == tab && openChannel != null -> openChannel = null
                        selected == tab && openPlaylist != null -> openPlaylist = null
                        selected == tab -> scrollToTop[selected] = (scrollToTop[selected] ?: 0) + 1
                        else -> {
                            if (selected == Tab.Shorts) shortsFrom = tab
                            openPlaylist = null; openChannel = null; tab = selected
                        }
                    }
                },
                player = player,
                morph = morph,
                controlsActive = false,
                bar = playerBar,
                onFullscreen = { toggleFullscreen() },
                shorts = { shortsMorph.value },
                tabsEnabled = !shortsShown,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = stackBottom)
                    .graphicsLayer {
                        val videoAway = if (playerOpen) 1f else ramp(morph.progress.value, 0f, 0.5f)
                        val away = maxOf(barAside, videoAway)
                        translationY = away * (NavHeight + PlayerBarRaise + stackBottom + 24.dp).toPx()
                        alpha = 1f - away
                    },
            )
        } else {
            if (!playerOpen && !showFullscreen) {
                TvNavRail(
                    selectedTab = tab,
                    onSelectTab = { selected ->
                        when {
                            selected == tab && openChannel != null -> openChannel = null
                            selected == tab && openPlaylist != null -> openPlaylist = null
                            selected == tab -> scrollToTop[selected] = (scrollToTop[selected] ?: 0) + 1
                            else -> {
                                if (selected == Tab.Shorts) shortsFrom = tab
                                openPlaylist = null; openChannel = null; tab = selected
                            }
                        }
                    },
                    onAccount = { accountSheet = true },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .focusProperties { canFocus = backgroundInteractive },
                )
            }
        }

        // The tab bar's Shorts form: ‹ and the glass pill, out of the tab bar's own spot. It steps
        // down out of the way of the comments panel (its composer sits there), as the player's bar does.
        if (shortsShown) {
            val shortsAside by animateFloatAsState(
                if (shorts.commentsOpen || openChannel != null) 1f else 0f,
                if (reduced) snap() else tween(280, easing = Motion.Emphasized), label = "shortsAside",
            )
            ShortsBar(
                shorts,
                progress = { shortsMorph.value },
                onHome = { tab = Tab.Home },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = if (isTv) com.github.itube.ui.tv.TvRailWidth + 16.dp else 16.dp,
                        end = 16.dp,
                        bottom = stackBottom + (if (isTv) 12.dp else 0.dp),
                    )
                    .widthIn(max = NavWidth)
                    .fillMaxWidth()
                    .height(NavHeight)
                    .graphicsLayer {
                        translationY = shortsAside * (NavHeight + stackBottom + 24.dp).toPx()
                        alpha = 1f - shortsAside
                    }
                    .focusProperties {
                        canFocus = backgroundInteractive && !playerOpen && !showFullscreen && !shorts.commentsOpen && openChannel == null
                    },
            )
        }

        if (shortsShown) ShortsChannelMenu(shorts, bottom = stackBottom + GlassHeight + 10.dp, onChannel = ::showChannel)

        if (showFullscreen) FullscreenPlayer(player, onExit = { orientation.exitFullscreen() })
        }

        if (accountSheet) {
            AccountSheet(
                onSignIn = { signingIn = true },
                onSignedOut = { lib.reset(); player.refreshAccountState() },
                onDismiss = { accountSheet = false },
            )
        }
        if (signingIn) {
            SignInScreen(
                onDone = { signingIn = false; lib.reset(); player.refreshAccountState() },
                onCancel = { signingIn = false },
            )
        }
        if (automaticUpdateNotes && availableUpdate != null) {
            UpdateNotesSheet(availableUpdate, onDismiss = { automaticUpdateNotes = false })
        }

        when (player.sheet) {
            PlayerSheet.Quality -> QualitySheet(player, onDismiss = { player.sheet = null })
            PlayerSheet.Speed -> SpeedSheet(player, onDismiss = { player.sheet = null })
            PlayerSheet.Comments, PlayerSheet.Transcript, PlayerSheet.Description, PlayerSheet.OnTheGo -> Unit // a panel inside the player (PlayerScreen)
            PlayerSheet.Captions -> CaptionsSheet(player, onDismiss = { player.sheet = null }, onTranscript = { player.sheet = PlayerSheet.Transcript })
            PlayerSheet.Queue -> QueueSheet(player, onDismiss = { player.sheet = null })
            PlayerSheet.SleepTimer -> SleepTimerSheet(
                isVideo = true,
                onDismiss = { player.sheet = null },
                onSetMessage = { player.actionMessage = it }
            )
            PlayerSheet.Equalizer -> EqualizerSheet(onDismiss = { player.sheet = null })
            null -> Unit
        }
        optionsFor?.let { video ->
            val playlist = optionsPlaylistFor
            VideoOptionsSheet(
                video,
                player,
                onDismiss = { optionsFor = null; optionsPlaylistFor = null },
                onOpenChannel = { url -> showChannel(url) },
                onRemoveFromPlaylist = playlist?.let { currentPlaylist ->
                    {
                        playlistFeed?.removePlaylistVideo(currentPlaylist.id, video)
                        optionsFor = null
                        optionsPlaylistFor = null
                    }
                },
            )
        }
        playlistOptionsFor?.let { playlist ->
            PlaylistOptionsSheet(
                playlist = playlist,
                onDismiss = { playlistOptionsFor = null },
                onReload = { selected ->
                    if (selected.id == openPlaylist?.id) playlistFeed?.refresh()
                    else lib.loadPlaylists()
                },
                onDelete = { selected ->
                    scope.launch {
                        runCatching { InnerTube.deletePlaylist(selected.id) }
                            .onSuccess {
                                lib.forgetPlaylist(selected.id)
                                lib.loadPlaylists()
                                if (openPlaylist?.id == selected.id) openPlaylist = null
                                playlistOptionsFor = null
                                player.actionMessage = "Playlist deleted"
                            }
                            .onFailure { player.actionMessage = it.message ?: "Couldn't delete playlist" }
                    }
                },
            )
        }
        collectionOptionsFor?.let { collection ->
            CollectionOptionsSheet(collection, onDismiss = { collectionOptionsFor = null })
        }
        // No taller than the comments panel opens (Derek): its top rests just under the video.
        player.playlistTarget?.let { video -> SaveToPlaylistSheet(player, video, maxHeight = maxHeight - panelRestTop(player)) }
        // Confirmations ("Added to queue") when the full player isn't showing its own.
        if (!showPlayer) {
            QueueMessage(
                player,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = if (isTv) com.github.itube.ui.tv.TvRailWidth else 0.dp,
                        bottom = stackBottom + (if (isTv) 12.dp else NavHeight) + (if (hasMini) MiniHeight + MiniGap else 0.dp) + 12.dp,
                    ),
            )
        }
        // Picture-in-picture: the window is just the video, over everything (kept composed
        // underneath, so expanding returns to exactly where things were).
        if (pip.active) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Box(Modifier.fillMaxSize().aspectFit(player.videoAspect)) {
                    sharedSurface(Modifier.fillMaxSize())
                    CaptionLayer(player, bottom = 4.dp)
                }
            }
        }
        CrashReports.pendingReport?.let { report ->
            CrashReportDialog(report, onDismiss = CrashReports::dismiss)
        }
        if (showExitDialog) {
            ExitAppDialog(
                onCancel = { showExitDialog = false },
                onConfirmExit = {
                    showExitDialog = false
                    player.close()
                    shorts.leave()
                    activity.stopService(Intent(activity, PlaybackService::class.java))
                    activity.finishAndRemoveTask()
                },
            )
        }
        }
    }
}

@Composable
private fun ExitAppDialog(
    onCancel: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        runCatching { cancelFocusRequester.requestFocus() }
    }
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = c.surfaceContainerHigh,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, c.outlineVariant.copy(alpha = 0.55f)),
            modifier = Modifier
                .widthIn(min = 280.dp, max = 380.dp)
                .focusGroup(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = null,
                        tint = c.onErrorContainer,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = "Exit iTube TV?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = c.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Active playback will stop and the app will close.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("exit_dialog_cancel_button")
                            .focusRequester(cancelFocusRequester)
                            .tvFocusRing(RoundedCornerShape(14.dp), focusScale = 1.03f),
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirmExit,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.error,
                            contentColor = c.onError,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("exit_dialog_confirm_button")
                            .tvFocusRing(RoundedCornerShape(14.dp), focusScale = 1.03f),
                    ) {
                        Text("Exit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueMessage(player: PlayerState, modifier: Modifier) {
    val message = player.actionMessage ?: return
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(2_000)
        player.actionMessage = null
    }
    val c = MaterialTheme.colorScheme
    Box(modifier.clip(RoundedCornerShape(50)).background(c.inverseSurface).padding(horizontal = 18.dp, vertical = 11.dp)) {
        Text(message, color = c.inverseOnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
