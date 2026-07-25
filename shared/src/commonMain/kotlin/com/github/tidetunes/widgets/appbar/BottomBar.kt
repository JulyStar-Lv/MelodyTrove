package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideBottomNavigationBar
import com.github.tidetunes.core.presentation.components.TideBottomNavigationItem
import com.github.tidetunes.core.presentation.components.getBottomBarSpace
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.navigation.HomeTab
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BottomBarSpacer(
    showMiniPlayer: Boolean,
    scaffoldPadding: PaddingValues,
) {
    Box(modifier = Modifier.height(getBottomBarSpace(showMiniPlayer, scaffoldPadding)))
}

@Composable
fun BoxScope.BottomBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    miniPlayerContent: @Composable () -> Unit,
    showMiniPlayer: Boolean,
    showChrome: Boolean,
    scaffoldPadding: PaddingValues,
) {
    if (!showChrome) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scaffoldPadding.calculateBottomPadding()),
        )
        return
    }

    val bottomItems = HomeTab.entries.map { tab ->
        TideBottomNavigationItem(
            label = tab.label,
            painter = painterResource(tab.painterRes),
            contentDescription = tab.label,
        )
    }
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth(),
    ) {
        if (showMiniPlayer) {
            Box(modifier = Modifier.padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 8.dp)) {
                miniPlayerContent()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TideTunesTokens.navigation.compactBarDividerHeight)
                    .background(MiuixTheme.colorScheme.outline),
            )
            TideBottomNavigationBar(
                items = bottomItems,
                selectedIndex = currentTab.index,
                onItemSelected = { index ->
                    HomeTab.entries.getOrNull(index)?.let(onTabSelected)
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaffoldPadding.calculateBottomPadding()),
            )
        }
    }
}
