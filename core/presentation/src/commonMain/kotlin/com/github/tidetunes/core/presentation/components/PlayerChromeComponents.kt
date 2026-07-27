package com.github.tidetunes.core.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
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
    showClickIndication: Boolean = true,
) {
    TidePlayerControlButton(
        painter = painter,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        variant = TidePlayerControlVariant.Primary,
        contentDescription = contentDescription,
        showClickIndication = showClickIndication,
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
    val glassSurfaceAlpha = tideGlassSurfaceAlpha()
    val clickInteractionSource = remember { MutableInteractionSource() }
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(contrast = 1.04f, saturation = 1.10f)
                blur(18.dp.toPx())
                lens(
                    refractionHeight = 8.dp.toPx(),
                    refractionAmount = 14.dp.toPx(),
                    depthEffect = true,
                )
            },
            highlight = {
                Highlight(
                    width = 0.25.dp,
                    blurRadius = 0.5.dp,
                    alpha = 0.78f,
                )
            },
            shadow = { null },
            onDrawSurface = { drawRect(surface.copy(alpha = glassSurfaceAlpha)) },
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
            .clip(shape)
            .then(glassModifier)
            .border(0.5.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f), shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = clickInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .clearAndSetSemantics {
                        contentDescription = "$title, $subtitle"
                        this.role = Role.Button
                        onClick { onClick(); true }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                artwork()
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
fun TideExpandedMiniPlayerBar(
    title: String,
    subtitle: String,
    progress: Float,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TideTunesTokens
    val shape = RoundedCornerShape(22.dp)
    val backdrop = currentTideBackdrop()
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val glassSurfaceAlpha = tideGlassSurfaceAlpha()
    val clickInteractionSource = remember { MutableInteractionSource() }
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(contrast = 1.04f, saturation = 1.10f)
                blur(18.dp.toPx())
                lens(
                    refractionHeight = 8.dp.toPx(),
                    refractionAmount = 14.dp.toPx(),
                    depthEffect = true,
                )
            },
            highlight = {
                Highlight(
                    width = 0.25.dp,
                    blurRadius = 0.5.dp,
                    alpha = 0.78f,
                )
            },
            shadow = { null },
            onDrawSurface = { drawRect(surface.copy(alpha = glassSurfaceAlpha)) },
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
            .clip(shape)
            .then(glassModifier)
            .border(0.5.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f), shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = clickInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .clearAndSetSemantics {
                        contentDescription = "$title, $subtitle"
                        this.role = Role.Button
                        onClick { onClick(); true }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    artwork()
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
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
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = subtitle,
                            color = MiuixTheme.colorScheme.onSurface,
                            style = MiuixTheme.textStyles.footnote1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
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
    accessibilityLabel: String,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlayControls: @Composable BoxScope.() -> Unit,
) {
    val tokens = TideTunesTokens
    val cornerRadius = tokens.shapes.lg
    val shape = RoundedCornerShape(cornerRadius)
    val backdrop = currentTideBackdrop()
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val glassSurfaceAlpha = tideGlassSurfaceAlpha()
    val clickInteractionSource = remember { MutableInteractionSource() }
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(contrast = 1.04f, saturation = 1.10f)
                blur(14.dp.toPx())
                lens(
                    refractionHeight = 7.dp.toPx(),
                    refractionAmount = 12.dp.toPx(),
                    depthEffect = true,
                )
            },
            highlight = {
                Highlight(
                    width = 0.25.dp,
                    blurRadius = 0.5.dp,
                    alpha = 0.78f,
                )
            },
            shadow = { null },
            onDrawSurface = { drawRect(surface.copy(alpha = glassSurfaceAlpha)) },
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
            .clip(shape)
            .then(glassModifier)
            .border(0.5.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f), shape)
            .semantics { contentDescription = accessibilityLabel }
            .clickable(
                interactionSource = clickInteractionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
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
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(MiuixTheme.colorScheme.primary),
        )
    }
}

@Composable
fun TideBottomNavigationGlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(0.dp)
    val backdrop = currentTideBackdrop()
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val glassSurfaceAlpha = tideGlassSurfaceAlpha()
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(contrast = 1.04f, saturation = 1.10f)
                blur(18.dp.toPx())
                lens(
                    refractionHeight = 8.dp.toPx(),
                    refractionAmount = 14.dp.toPx(),
                    depthEffect = true,
                )
            },
            highlight = {
                Highlight(
                    width = 0.25.dp,
                    blurRadius = 0.5.dp,
                    alpha = 0.78f,
                )
            },
            shadow = { null },
            onDrawSurface = { drawRect(surface.copy(alpha = glassSurfaceAlpha)) },
        )
    } else {
        Modifier.background(surface.copy(alpha = 0.90f))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(glassModifier),
        content = content,
    )
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
        ) {
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
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onItemSelected(index) },
                        )
                        .clearAndSetSemantics {
                            contentDescription = item.contentDescription ?: item.label
                            this.role = Role.Tab
                            this.selected = isSelected
                            onClick { onItemSelected(index); true }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = item.painter,
                        tint = tint,
                        contentDescription = null,
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
