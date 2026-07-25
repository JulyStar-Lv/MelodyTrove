package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.tidetunes.core.presentation.theme.TideTunesTokens

@Composable
fun getBottomBarSpace(
    showMiniPlayer: Boolean,
    scaffoldPadding: PaddingValues,
): Dp {
    var total = TideTunesTokens.navigation.compactBarHeight + scaffoldPadding.calculateBottomPadding()
    if (showMiniPlayer) {
        total += TideTunesTokens.player.compactMiniBarHeight + TideTunesTokens.spacing.xs
    }
    return total
}

@Composable
fun BottomBarSpacer(
    showMiniPlayer: Boolean,
    scaffoldPadding: PaddingValues,
) {
    Box(modifier = Modifier.height(getBottomBarSpace(showMiniPlayer, scaffoldPadding)))
}
