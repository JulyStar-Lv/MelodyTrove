package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideBottomNavigationBar
import com.github.tidetunes.core.presentation.components.TideBottomNavigationItem
import com.github.tidetunes.navigation.HomeTab
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun getBottomBarSpace(
    isPlaying: Boolean,
    scaffoldPadding: PaddingValues,
): Dp {
    var total = 64.dp + scaffoldPadding.calculateBottomPadding()
    if (isPlaying) {
        total += 76.dp
    }
    return total
}

@Composable
fun BottomBarSpacer(
    hasCurrentMusic: Boolean,
    scaffoldPadding: PaddingValues,
) {
    Box(modifier = Modifier.height(getBottomBarSpace(hasCurrentMusic, scaffoldPadding)))
}

@Composable
fun BoxScope.BottomBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    miniPlayerContent: @Composable () -> Unit,
    hasCurrentMusic: Boolean,
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
        if (hasCurrentMusic) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                miniPlayerContent()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f)),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
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
