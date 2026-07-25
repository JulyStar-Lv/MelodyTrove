package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.navigation.HomeTab
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun getNavigationRailWidth(windowSizeClass: WindowSizeClass = WindowSizeClass.Expanded): Dp {
    return if (windowSizeClass == WindowSizeClass.Medium) 72.dp else 80.dp
}

@Composable
fun NavigationRailBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = WindowSizeClass.Expanded,
) {
    val railWidth = getNavigationRailWidth(windowSizeClass)
    val shapes = TideTunesTokens.shapes

    Column(
        modifier = modifier
            .width(railWidth)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 18.dp, bottom = 22.dp)
                .size(38.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(TideTunesBrand.Primary, TideTunesBrand.Secondary),
                    ),
                    shape = RoundedCornerShape(shapes.sm),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(HomeTab.LIBRARY.painterRes),
                contentDescription = "TideTunes",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 6.dp)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (tab in HomeTab.entries) {
                NavigationRailItem(
                    tab = tab,
                    selected = currentTab == tab,
                    itemWidth = railWidth - 12.dp,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun NavigationRailItem(
    tab: HomeTab,
    selected: Boolean,
    itemWidth: Dp,
    onClick: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes
    val itemShape = RoundedCornerShape(shapes.compactCard)
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    }
    val backgroundBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                TideTunesBrand.Primary.copy(alpha = 0.18f),
                TideTunesBrand.Secondary.copy(alpha = 0.15f),
            ),
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Column(
        modifier = Modifier
            .width(itemWidth)
            .clip(itemShape)
            .background(backgroundBrush)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .clearAndSetSemantics {
                contentDescription = tab.label
                this.role = Role.Tab
                this.selected = selected
                onClick { onClick(); true }
            }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(tab.painterRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = tab.label,
            color = contentColor,
            style = MiuixTheme.textStyles.footnote2,
            maxLines = 1,
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}
