package com.github.itube.ui.tv

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalIsTv = compositionLocalOf { false }
val LocalTvFocusBridge = compositionLocalOf<TvFocusBridge?> { null }

@Stable
class TvFocusBridge {
    internal data class EntrySpec(
        val key: Any,
        val requester: FocusRequester,
        val priority: Int,
        val centeredEmptyState: Boolean,
        val isAttached: () -> Boolean,
        val sequence: Long,
    )

    private data class HeaderSpec(
        val requester: FocusRequester,
        val isAttached: () -> Boolean,
    )

    val railRequester = FocusRequester()
    var activeRoute by mutableStateOf("tab:Home")

    private var seq = 0L
    private val routeEntries = mutableStateMapOf<String, MutableList<EntrySpec>>()
    private val routeHeaders = mutableStateMapOf<String, HeaderSpec>()

    fun registerEntry(
        route: String,
        requester: FocusRequester,
        centeredEmptyState: Boolean,
        priority: Int = 0,
        key: Any = requester,
        isAttached: () -> Boolean = { true },
    ) {
        val list = routeEntries.getOrPut(route) { mutableListOf() }
        list.removeAll { it.key == key || it.requester === requester }
        list.add(
            EntrySpec(
                key = key,
                requester = requester,
                priority = priority,
                centeredEmptyState = centeredEmptyState,
                isAttached = isAttached,
                sequence = ++seq,
            ),
        )
    }

    fun unregisterEntry(route: String, requester: FocusRequester, key: Any = requester) {
        val list = routeEntries[route] ?: return
        list.removeAll { it.key == key || it.requester === requester }
        if (list.isEmpty()) routeEntries.remove(route)
    }

    fun registerHeader(
        route: String,
        requester: FocusRequester,
        isAttached: () -> Boolean = { true },
    ) {
        routeHeaders[route] = HeaderSpec(requester, isAttached)
    }

    fun unregisterHeader(route: String, requester: FocusRequester) {
        val existing = routeHeaders[route]
        if (existing != null && existing.requester === requester) {
            routeHeaders.remove(route)
        }
    }

    internal fun orderedAttachedEntries(route: String = activeRoute): List<EntrySpec> =
        routeEntries[route]
            .orEmpty()
            .filter { it.isAttached() }
            .sortedWith(compareBy<EntrySpec> { it.priority }.thenBy { it.sequence })

    val isActiveCenteredEmptyState: Boolean
        get() {
            val attached = orderedAttachedEntries(activeRoute)
            return attached.firstOrNull()?.centeredEmptyState == true
        }

    fun currentEntryRequester(): FocusRequester? =
        orderedAttachedEntries(activeRoute).firstOrNull()?.requester

    fun currentHeaderRequester(): FocusRequester? =
        routeHeaders[activeRoute]?.takeIf { it.isAttached() }?.requester

    /**
     * Returns a verified attached target when exiting the TV nav rail rightwards, or null so
     * Compose's geometric 2D focus search lands on the closest scrolled content item directly.
     */
    fun exitToContentRequester(): FocusRequester? {
        val attached = orderedAttachedEntries(activeRoute)
        val first = attached.firstOrNull()
        if (first != null && (first.centeredEmptyState || first.priority <= 10)) {
            return first.requester
        }
        return if (attached.isEmpty()) currentHeaderRequester() else null
    }

    fun focusRail(): Boolean = runCatching {
        railRequester.requestFocus()
        true
    }.getOrDefault(false)

    fun focusContent(): Boolean {
        for (entry in orderedAttachedEntries(activeRoute)) {
            if (runCatching { entry.requester.requestFocus(); true }.getOrDefault(false)) {
                return true
            }
        }
        return false
    }

    fun focusHeader(): Boolean {
        val req = currentHeaderRequester() ?: return false
        return runCatching {
            req.requestFocus()
            true
        }.getOrDefault(false)
    }

    fun focusContentOrHeaderOrRail(): Boolean = focusContent() || focusHeader() || focusRail()
}

@Composable
fun Modifier.tvContentEntry(
    route: String,
    centeredEmptyState: Boolean = false,
    priority: Int = 0,
): Modifier {
    val bridge = LocalTvFocusBridge.current ?: return this
    val isTv = LocalIsTv.current
    if (!isTv) return this
    val key = remember(route) { Any() }
    val requester = remember(route) { FocusRequester() }
    var attached by remember(route) { mutableStateOf(false) }
    DisposableEffect(bridge, route, key, requester, centeredEmptyState, priority) {
        bridge.registerEntry(
            route = route,
            requester = requester,
            centeredEmptyState = centeredEmptyState,
            priority = priority,
            key = key,
            isAttached = { attached },
        )
        onDispose {
            attached = false
            bridge.unregisterEntry(route, requester, key)
        }
    }
    return this
        .onGloballyPositioned { attached = it.isAttached }
        .focusRequester(requester)
}

object TvHelper {
    /** True if running on an Android TV device (Leanback / TV ui mode). */
    fun isTvDevice(context: Context): Boolean {
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        ) {
            return true
        }
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}

/**
 * Returns true if running on Android TV or in landscape mode with TV optimization enabled.
 */
@Composable
fun rememberIsTv(): Boolean {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    return remember(config.orientation, config.screenWidthDp) {
        TvHelper.isTvDevice(context) ||
            config.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            config.screenWidthDp >= 600
    }
}

/**
 * Computes responsive grid column count for wide landscape / TV screens.
 */
fun tvGridColumns(maxWidth: Dp, minColumnWidth: Dp = 270.dp, maxColumns: Int = 4): Int {
    if (maxWidth < 540.dp) return 1
    return (maxWidth / minColumnWidth).toInt().coerceIn(2, maxColumns)
}

/**
 * Adds high-visibility D-pad focus feedback (scale up, elevation, luminous focus border drawn
 * above content/backgrounds so opaque fills never cover the ring).
 * Works automatically on Material3 buttons, chips, IconButtons, Surfaces, and custom focusables.
 */
fun Modifier.tvFocusRing(
    shape: Shape = RoundedCornerShape(16.dp),
    focusScale: Float = 1.04f,
    borderWidth: Dp = 2.75.dp,
    borderColor: Color? = null,
    addFocusable: Boolean = false,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusScale else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 1350f),
        label = "tvFocusScale",
    )
    val c = MaterialTheme.colorScheme
    val isTv = LocalIsTv.current
    val strokeColor = borderColor ?: if (isTv) Color.White else c.primary
    val outerHalo = if (strokeColor.luminance() > 0.45f) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.88f)

    this
        .onFocusChanged { focused = it.hasFocus || it.isFocused }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            if (focused) shadowElevation = 12f
        }
        .drawWithContent {
            drawContent()
            if (focused) {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    color = outerHalo,
                    style = Stroke(width = (borderWidth + 1.5.dp).toPx()),
                )
                drawOutline(
                    outline = outline,
                    color = strokeColor,
                    style = Stroke(width = borderWidth.toPx()),
                )
            }
        }
        .then(if (addFocusable) Modifier.focusable() else Modifier)
}

/**
 * Allows D-pad Left/Right keys to step a slider value cleanly on TV remotes.
 */
fun Modifier.tvSliderKeys(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
): Modifier = this.onPreviewKeyEvent { ev ->
    if (!enabled || ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (ev.key) {
        Key.DirectionLeft -> {
            val next = (value - step).coerceIn(valueRange.start, valueRange.endInclusive)
            onValueChange(next)
            onValueChangeFinished?.invoke()
            true
        }
        Key.DirectionRight -> {
            val next = (value + step).coerceIn(valueRange.start, valueRange.endInclusive)
            onValueChange(next)
            onValueChangeFinished?.invoke()
            true
        }
        else -> false
    }
}

