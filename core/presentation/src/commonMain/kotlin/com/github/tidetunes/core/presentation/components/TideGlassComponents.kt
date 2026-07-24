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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LocalTideBackdrop = staticCompositionLocalOf<Backdrop?> { null }

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
 * A compact ActionBar that fades in with the page scroll position and samples [TideGlassScene].
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
    val backdrop = LocalTideBackdrop.current
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                colorControls(saturation = 1.1f)
                blur(18.dp.toPx() * fraction)
            },
            highlight = { null },
            shadow = { null },
            onDrawSurface = {
                drawRect(surface.copy(alpha = 0.82f))
            },
        )
    } else {
        Modifier.background(surface.copy(alpha = 0.94f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adaptive.compactHeaderHeight)
            .alpha(fraction)
            .then(glassModifier),
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
