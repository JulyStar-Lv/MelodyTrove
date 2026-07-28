package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens

@Composable
fun getBottomBarSpace(
    showMiniPlayer: Boolean,
    scaffoldPadding: PaddingValues,
): Dp {
    var total = DesignTokens.navigation.compactBarHeight + scaffoldPadding.calculateBottomPadding()
    if (showMiniPlayer) {
        total += DesignTokens.player.compactMiniBarHeight + DesignTokens.spacing.xs
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
