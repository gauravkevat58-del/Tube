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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalIsTv = compositionLocalOf { false }

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
    focusScale: Float = 1.05f,
    borderWidth: Dp = 2.5.dp,
    borderColor: Color? = null,
    addFocusable: Boolean = false,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusScale else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 1200f),
        label = "tvFocusScale",
    )
    val c = MaterialTheme.colorScheme
    val strokeColor = borderColor ?: c.primary

    this
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            if (focused) shadowElevation = 16f
        }
        .drawWithContent {
            drawContent()
            if (focused) {
                val outline = shape.createOutline(size, layoutDirection, this)
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

