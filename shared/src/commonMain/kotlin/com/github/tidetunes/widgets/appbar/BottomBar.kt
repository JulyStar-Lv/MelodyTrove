package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideBottomNavigationBar
import com.github.tidetunes.core.presentation.components.TideBottomNavigationItem
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.navigation.HomeTab
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun getBottomBarSpace(
    isPlaying: Boolean,
    scaffoldPadding: PaddingValues,
): Dp {
    var total = 64.dp + scaffoldPadding.calculateBottomPadding()
    if (isPlaying) {
        total += 80.dp
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
    val shape = RoundedCornerShape(
        topStart = TideTunesTokens.shapes.lg,
        topEnd = TideTunesTokens.shapes.lg,
    )

    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth(),
    ) {
        if (hasCurrentMusic) {
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                miniPlayerContent()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f))
                .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.58f), shape),
        ) {
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
