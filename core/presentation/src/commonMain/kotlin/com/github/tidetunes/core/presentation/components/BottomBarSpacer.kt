package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesTokens

@Composable
fun getBottomBarSpace(
    isPlaying: Boolean,
    scaffoldPadding: PaddingValues,
): Dp {
    var total = TideTunesTokens.navigation.compactBarHeight + scaffoldPadding.calculateBottomPadding()
    if (isPlaying) {
        total += 124.dp
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
