package com.github.tidetunes.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class TideTabsVariant {
    Line,
    Pill,
    Segmented,
}

@Immutable
data class TideTabItem(
    val label: String,
    val badge: String? = null,
    val enabled: Boolean = true,
)

@Composable
fun TideTabs(
    items: List<TideTabItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    variant: TideTabsVariant = TideTabsVariant.Line,
    enabled: Boolean = true,
) {
    if (items.isEmpty()) return

    val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)

    when (variant) {
        TideTabsVariant.Line -> TideLineTabs(
            items = items,
            selectedIndex = safeSelectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            modifier = modifier,
            enabled = enabled,
        )
        TideTabsVariant.Pill -> TidePillTabs(
            items = items,
            selectedIndex = safeSelectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            modifier = modifier,
            enabled = enabled,
        )
        TideTabsVariant.Segmented -> TideSegmentedTabs(
            items = items,
            selectedIndex = safeSelectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

@Composable
private fun TideLineTabs(
    items: List<TideTabItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val motion = TideTunesTokens.motion

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        val tabWidth = maxWidth / items.size.toFloat()
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex.toFloat(),
            animationSpec = tween(durationMillis = motion.fastMillis),
            label = "tideTabsLineIndicator",
        )

        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    TideLineTabItem(
                        item = item,
                        selected = index == selectedIndex,
                        enabled = enabled && item.enabled,
                        onClick = { onSelectedIndexChange(index) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .padding(horizontal = 18.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                        .background(tabIndicatorBrush(enabled)),
                )
            }
        }
    }
}

@Composable
private fun TidePillTabs(
    items: List<TideTabItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TideTunesTokens.spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            TidePillTabItem(
                item = item,
                selected = index == selectedIndex,
                enabled = enabled && item.enabled,
                onClick = { onSelectedIndexChange(index) },
            )
        }
    }
}

@Composable
private fun TideSegmentedTabs(
    items: List<TideTabItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.full)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MiuixTheme.colorScheme.outline, shape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            TideSegmentedTabItem(
                item = item,
                selected = index == selectedIndex,
                enabled = enabled && item.enabled,
                onClick = { onSelectedIndexChange(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TideLineTabItem(
    item: TideTabItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.Tab,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        TideTabLabel(
            item = item,
            selected = selected,
            enabled = enabled,
            compact = false,
        )
    }
}

@Composable
private fun TidePillTabItem(
    item: TideTabItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.full)
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = TideTunesTokens.motion.fastMillis),
        label = "tideTabsPillContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = Color.Transparent,
        animationSpec = tween(durationMillis = TideTunesTokens.motion.fastMillis),
        label = "tideTabsPillBorder",
    )

    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .widthIn(min = 48.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TideTabLabel(
            item = item,
            selected = selected,
            enabled = enabled,
            compact = true,
            selectedContentColor = MiuixTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun TideSegmentedTabItem(
    item: TideTabItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.full)
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.tertiaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = TideTunesTokens.motion.fastMillis),
        label = "tideTabsSegmentContainer",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(containerColor)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TideTabLabel(
            item = item,
            selected = selected,
            enabled = enabled,
            compact = true,
        )
    }
}

@Composable
private fun TideTabLabel(
    item: TideTabItem,
    selected: Boolean,
    enabled: Boolean,
    compact: Boolean,
    selectedContentColor: Color? = null,
) {
    val contentColor = if (enabled && selected && selectedContentColor != null) {
        selectedContentColor
    } else {
        tabContentColor(selected = selected, enabled = enabled)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.label,
            color = contentColor,
            style = if (compact) MiuixTheme.textStyles.body2 else MiuixTheme.textStyles.title4,
            fontWeight = if (compact || selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.badge != null) {
            Text(
                text = item.badge,
                color = contentColor,
                style = MiuixTheme.textStyles.footnote2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun tabContentColor(
    selected: Boolean,
    enabled: Boolean,
): Color {
    return when {
        !enabled -> MiuixTheme.colorScheme.disabledOnSurface
        selected -> MiuixTheme.colorScheme.primary
        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
}

@Composable
private fun tabIndicatorBrush(enabled: Boolean): Brush {
    val colors = if (enabled) {
        listOf(TideTunesBrand.Primary, TideTunesBrand.Secondary)
    } else {
        listOf(
            MiuixTheme.colorScheme.disabledPrimarySlider,
            MiuixTheme.colorScheme.disabledPrimarySlider,
        )
    }
    return Brush.horizontalGradient(colors)
}
