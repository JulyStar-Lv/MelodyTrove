package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.dropShadow
import com.github.tidetunes.navigation.HomeTab
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun getSidebarWidth(): Dp = 248.dp

@Composable
fun SidebarBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    miniPlayerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(getSidebarWidth())
            .dropShadow(
                MiuixTheme.colorScheme.surfaceVariant,
                4.dp,
                0.dp,
                8.dp,
            )
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(MiuixTheme.colorScheme.surface)
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
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val backgroundColor = if (selected) {
        MiuixTheme.colorScheme.secondaryContainer
    } else {
        MiuixTheme.colorScheme.surface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
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
