package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.dropShadow
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

    Column(
        modifier = modifier
            .width(railWidth)
            .dropShadow(
                MiuixTheme.colorScheme.surfaceVariant,
                4.dp,
                0.dp,
                8.dp,
            )
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (tab in HomeTab.entries) {
                NavigationRailItem(
                    tab = tab,
                    selected = currentTab == tab,
                    itemWidth = railWidth - 8.dp,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
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
    val itemShape = RoundedCornerShape(shapes.full)
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    }
    val backgroundBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                TideTunesBrand.Primary.copy(alpha = 0.18f),
                TideTunesBrand.Secondary.copy(alpha = 0.16f),
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
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(tab.painterRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .width(24.dp)
                .height(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.label,
            color = contentColor,
            style = MiuixTheme.textStyles.footnote2,
            maxLines = 1,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}
