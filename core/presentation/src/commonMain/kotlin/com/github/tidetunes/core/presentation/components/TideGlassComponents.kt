package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LocalTideBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val LocalTideBottomContentInset = staticCompositionLocalOf { 0.dp }

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
 * A compact, opaque ActionBar that fades in with the page scroll position.
 */
@Composable
fun TideStickyGlassActionBar(
    title: String,
    subtitle: String? = null,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = collapseFraction.coerceIn(0f, 1f)
    val adaptive = TideTunesTokens.adaptive
    val titleFraction = ((fraction - 0.72f) / 0.28f).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adaptive.compactHeaderHeight)
            .alpha(fraction)
            .background(MiuixTheme.colorScheme.background),
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

@Composable
internal fun currentTideBackdrop(): Backdrop? = LocalTideBackdrop.current

@Composable
fun tideGlassSurfaceAlpha(): Float {
    return if (MiuixTheme.colorScheme.background.luminance() < 0.5f) {
        0.24f
    } else {
        0.52f
    }
}
