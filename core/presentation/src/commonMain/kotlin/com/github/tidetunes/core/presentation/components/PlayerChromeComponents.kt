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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val shapes = TideTunesTokens.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(shapes.lg))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.72f),
                shape = RoundedCornerShape(shapes.lg),
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            artwork()
            Spacer(modifier = Modifier.width(10.dp))
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
                    style = MiuixTheme.textStyles.footnote2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    val shapes = TideTunesTokens.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(shapes.lg))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.72f),
                shape = RoundedCornerShape(shapes.lg),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
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
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
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
    height: Dp = 64.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
) {
    if (items.isEmpty()) return

    val shapes = TideTunesTokens.shapes
    val motion = TideTunesTokens.motion

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
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
                    .padding(top = 7.dp)
                    .size(width = 42.dp, height = 26.dp)
                    .clip(RoundedCornerShape(shapes.full))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                TideTunesBrand.Primary.copy(alpha = 0.18f),
                                TideTunesBrand.Secondary.copy(alpha = 0.16f),
                            ),
                        ),
                    ),
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
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.label,
                        color = tint,
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
