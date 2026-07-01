package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

fun getBottomBarSpace(
    isPlaying: Boolean,
    scaffoldPadding: PaddingValues
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
    showChrome: Boolean,
    scaffoldPadding: PaddingValues,
) {
    if (!showChrome) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(scaffoldPadding.calculateBottomPadding())
        )
        return
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .dropShadow(
                MiuixTheme.colorScheme.surfaceVariant,
                0.dp,
                (-4).dp,
                8.dp,
            )
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
    ) {
        miniPlayerContent()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            for (tab in HomeTab.entries) {
                val isSelected = currentTab == tab
                val tint = if (isSelected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantActions
                }

                Column(modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .padding(top = 7.dp, bottom = 6.dp)
                    .clickable {
                        onTabSelected(tab)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(tab.painterRes),
                        tint = tint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        style = MiuixTheme.textStyles.footnote2,
                        maxLines = 1,
                    )
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(scaffoldPadding.calculateBottomPadding())
        )
    }
}
