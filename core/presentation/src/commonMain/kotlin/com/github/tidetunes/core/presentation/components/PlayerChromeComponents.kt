package com.github.tidetunes.core.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TideGradientPlayButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: TidePlayerControlSize = TidePlayerControlSize.Mini,
    contentDescription: String? = null,
) {
    TidePlayerControlButton(
        painter = painter,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        variant = TidePlayerControlVariant.Primary,
        contentDescription = contentDescription,
    )
}

@Composable
fun TideMiniPlayerBar(
    title: String,
    subtitle: String,
    progress: Float,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit,
    controls: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TideTunesTokens
    val shape = RoundedCornerShape(22.dp)
    val backdrop = currentTideBackdrop()
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = 1.18f)
                blur(16.dp.toPx())
                lens(
                    refractionHeight = 12.dp.toPx(),
                    refractionAmount = 18.dp.toPx(),
                    depthEffect = true,
                )
            },
            shadow = { null },
            onDrawSurface = { drawRect(surface.copy(alpha = 0.50f)) },
        )
    } else {
        Modifier
            .clip(shape)
            .background(surface.copy(alpha = 0.90f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.player.miniBarHeight)
            .shadow(tokens.elevation.popup, shape, clip = false)
            .then(glassModifier)
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.42f), shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            TideTunesBrand.Primary.copy(alpha = 0.12f),
                            Color.Transparent,
                            TideTunesBrand.Secondary.copy(alpha = 0.10f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            artwork()
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title.ifBlank { "TideTunes" },
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle.ifBlank { "Ready to play" },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = controls,
            )
        }
        TideMiniPlayerProgress(
            progress = progress,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
fun TideCompactMiniPlayerBar(
    progress: Float,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlayControls: @Composable BoxScope.() -> Unit,
) {
    val tokens = TideTunesTokens
    val shape = RoundedCornerShape(tokens.shapes.lg)
    val backdrop = currentTideBackdrop()
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = 1.16f)
                blur(14.dp.toPx())
                lens(
                    refractionHeight = 10.dp.toPx(),
                    refractionAmount = 14.dp.toPx(),
                    depthEffect = true,
                )
            },
            shadow = { null },
            onDrawSurface = { drawRect(surface.copy(alpha = 0.52f)) },
        )
    } else {
        Modifier
            .clip(shape)
            .background(surface.copy(alpha = 0.92f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.player.compactMiniBarHeight)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .shadow(tokens.elevation.card, shape, clip = false)
            .then(glassModifier)
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.60f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            TideTunesBrand.Primary.copy(alpha = 0.10f),
                            Color.Transparent,
                            TideTunesBrand.Secondary.copy(alpha = 0.08f),
                        ),
                    ),
                ),
        )
        artwork()
        overlayControls()
        TideMiniPlayerProgress(
            progress = progress,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
fun TideMiniPlayerProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            TideTunesBrand.Primary,
                            TideTunesBrand.Secondary,
                        ),
                    ),
                ),
        )
    }
}

@Immutable
data class TideBottomNavigationItem(
    val label: String,
    val painter: Painter,
    val contentDescription: String? = label,
)

@Composable
fun TideBottomNavigationBar(
    items: List<TideBottomNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
) {
    if (items.isEmpty()) return

    val shapes = TideTunesTokens.shapes
    val motion = TideTunesTokens.motion
    val navigation = TideTunesTokens.navigation

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height ?: navigation.compactBarHeight)
            .padding(contentPadding),
    ) {
        val selected = selectedIndex.coerceIn(0, items.lastIndex)
        val itemWidth = maxWidth / items.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selected,
            animationSpec = tween(durationMillis = motion.standardMillis),
            label = "tideBottomNavigationIndicatorOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(
                        width = navigation.compactSelectedIndicatorWidth,
                        height = navigation.compactSelectedIndicatorHeight,
                    )
                    .clip(RoundedCornerShape(shapes.full))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val isSelected = selected == index
                val tint = if (isSelected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantActions
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(shapes.lg))
                        .clickable { onItemSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = item.painter,
                        tint = tint,
                        contentDescription = item.contentDescription,
                        modifier = Modifier.size(navigation.compactIconSize),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = tint,
                        style = MiuixTheme.textStyles.footnote2.copy(
                            fontSize = navigation.compactLabelSize,
                            lineHeight = 12.sp,
                        ),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
