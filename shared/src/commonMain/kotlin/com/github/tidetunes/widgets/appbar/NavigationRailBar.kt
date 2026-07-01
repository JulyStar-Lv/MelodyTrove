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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.dropShadow
import com.github.tidetunes.navigation.HomeTab
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun getNavigationRailWidth(): Dp = 80.dp

@Composable
fun NavigationRailBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    miniPlayerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(getNavigationRailWidth())
            .dropShadow(
                MiuixTheme.colorScheme.surfaceVariant,
                4.dp,
                0.dp,
                8.dp,
            )
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(MiuixTheme.colorScheme.surface),
    ) {
        miniPlayerContent()
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
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.surface)
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
