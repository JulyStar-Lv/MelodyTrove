package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LocalTideBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val LocalTideBottomContentInset = staticCompositionLocalOf { 0.dp }

@Immutable
data class TideStickyHeaderState(
    val title: String,
    val subtitle: String?,
    val collapseFraction: Float,
)

val LocalTideStickyHeaderStateSink =
    staticCompositionLocalOf<((TideStickyHeaderState?) -> Unit)?> { null }

@Immutable
object TideLiquidGlassDefaults {
    const val contrast = 1.04f
    const val saturation = 1.10f
    val blurRadius = 18.dp
    val refractionHeight = 8.dp
    val refractionAmount = 14.dp
    const val depthEffect = true
    val highlightWidth = 0.25.dp
    val highlightBlurRadius = 0.5.dp
    const val highlightAlpha = 0.78f
    const val darkSurfaceAlpha = 0.24f
    const val lightSurfaceAlpha = 0.52f
    const val fallbackSurfaceAlpha = 0.90f
}

/**
 * Provides the safe, opaque fallback for glass components.
 *
 * A layer backdrop must exclude every component that samples it. This scene has a single content
 * slot, so recording it would include those components and create a recursive draw on iOS.
 */
@Composable
fun TideGlassScene(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalTideBackdrop provides null) {
        Box(
            modifier = modifier,
            content = content,
        )
    }
}

/**
 * Records [backdropContent] without any glass consumers, then lets [overlayContent] sample it.
 * Keeping the overlay outside the recorded layer avoids recursive backdrop rendering.
 */
@Composable
fun TideGlassOverlayScene(
    modifier: Modifier = Modifier,
    contentBottomInset: Dp = 0.dp,
    backdropContent: @Composable BoxScope.() -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()

    CompositionLocalProvider(LocalTideBottomContentInset provides contentBottomInset) {
        Box(modifier = modifier) {
            CompositionLocalProvider(LocalTideBackdrop provides null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(backdrop),
                    content = backdropContent,
                )
            }
            CompositionLocalProvider(LocalTideBackdrop provides backdrop) {
                overlayContent()
            }
        }
    }
}

/**
 * A compact ActionBar that progressively applies the shared liquid-glass treatment.
 */
@Composable
fun TideStickyGlassActionBar(
    title: String,
    subtitle: String? = null,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    statusBarInset: Dp = 0.dp,
) {
    val fraction = collapseFraction.coerceIn(0f, 1f)
    val stateSink = LocalTideStickyHeaderStateSink.current
    if (stateSink != null) {
        SideEffect {
            stateSink(
                TideStickyHeaderState(
                    title = title,
                    subtitle = subtitle,
                    collapseFraction = fraction,
                ),
            )
        }
        DisposableEffect(stateSink) {
            onDispose { stateSink(null) }
        }
        return
    }

    val adaptive = TideTunesTokens.adaptive
    val titleFraction = ((fraction - 0.72f) / 0.28f).coerceIn(0f, 1f)
    val backdrop = currentTideBackdrop()
    val glassModifier = if (backdrop != null && fraction > 0f) {
        Modifier.tideLiquidGlass(
            backdrop = backdrop,
            shape = RoundedCornerShape(0.dp),
            intensity = fraction,
        )
    } else if (backdrop == null) {
        Modifier
            .alpha(fraction)
            .background(MiuixTheme.colorScheme.background)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adaptive.compactHeaderHeight + statusBarInset)
            .then(glassModifier),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(adaptive.compactHeaderHeight),
            contentAlignment = Alignment.Center,
        ) {
            TidePageHeader(
                title = title,
                subtitle = subtitle,
                compact = true,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .alpha(titleFraction),
            )
        }
    }
}

@Composable
internal fun currentTideBackdrop(): Backdrop? = LocalTideBackdrop.current

@Composable
fun Modifier.tideLiquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    intensity: Float = 1f,
): Modifier {
    val fraction = intensity.coerceIn(0f, 1f)
    if (fraction == 0f) return this

    val defaults = TideLiquidGlassDefaults
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val surfaceAlpha = if (MiuixTheme.colorScheme.background.luminance() < 0.5f) {
        defaults.darkSurfaceAlpha
    } else {
        defaults.lightSurfaceAlpha
    }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(
                contrast = 1f + (defaults.contrast - 1f) * fraction,
                saturation = 1f + (defaults.saturation - 1f) * fraction,
            )
            blur((defaults.blurRadius * fraction).toPx())
            lens(
                refractionHeight = (defaults.refractionHeight * fraction).toPx(),
                refractionAmount = (defaults.refractionAmount * fraction).toPx(),
                depthEffect = defaults.depthEffect,
            )
        },
        highlight = {
            Highlight(
                width = defaults.highlightWidth,
                blurRadius = defaults.highlightBlurRadius,
                alpha = defaults.highlightAlpha * fraction,
            )
        },
        shadow = { null },
        onDrawSurface = {
            drawRect(surface.copy(alpha = surfaceAlpha * fraction))
        },
    )
}
