package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scaffoldPadding.calculateBottomPadding())
        )
        return
    }

    val shapes = TideTunesTokens.shapes

    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 10.dp)
            .dropShadow(
                Color.Black.copy(alpha = 0.10f),
                0.dp,
                (-6).dp,
                18.dp,
            )
            .clip(RoundedCornerShape(topStart = shapes.xxl, topEnd = shapes.xxl))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.70f),
                shape = RoundedCornerShape(topStart = shapes.xxl, topEnd = shapes.xxl),
            )
            .fillMaxWidth()
    ) {
        miniPlayerContent()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            for (tab in HomeTab.entries) {
                val isSelected = currentTab == tab
                val tint = if (isSelected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantActions
                }

                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(shapes.lg))
                        .clickable { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 26.dp)
                            .clip(RoundedCornerShape(shapes.full))
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(
                                        listOf(
                                            TideTunesBrand.Primary.copy(alpha = 0.18f),
                                            TideTunesBrand.Secondary.copy(alpha = 0.16f),
                                        ),
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                        ),
                                    )
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(tab.painterRes),
                            tint = tint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scaffoldPadding.calculateBottomPadding())
        )
    }
}
