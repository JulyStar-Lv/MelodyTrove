package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
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

fun getSidebarWidth(windowSizeClass: WindowSizeClass = WindowSizeClass.Large): Dp {
    return if (windowSizeClass == WindowSizeClass.XL) 260.dp else 240.dp
}

@Composable
fun SidebarBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    miniPlayerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = WindowSizeClass.Large,
) {
    val shapes = TideTunesTokens.shapes
    val sidebarShape = RoundedCornerShape(topEnd = shapes.xl, bottomEnd = shapes.xl)

    Column(
        modifier = modifier
            .width(getSidebarWidth(windowSizeClass))
            .dropShadow(
                MiuixTheme.colorScheme.surfaceVariant,
                4.dp,
                0.dp,
                8.dp,
            )
            .clip(sidebarShape)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.70f), sidebarShape)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Text(
            text = "TideTunes",
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        for (tab in HomeTab.entries) {
            SidebarItem(
                tab = tab,
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        miniPlayerContent()
    }
}

@Composable
private fun SidebarItem(
    tab: HomeTab,
    selected: Boolean,
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(itemShape)
            .background(backgroundBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Icon(
            painter = painterResource(tab.painterRes),
            tint = contentColor,
            contentDescription = null,
            modifier = Modifier
                .width(22.dp)
                .height(22.dp),
        )
        Box(modifier = Modifier.width(14.dp))
        Text(
            text = tab.label,
            color = contentColor,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
        )
    }
}
