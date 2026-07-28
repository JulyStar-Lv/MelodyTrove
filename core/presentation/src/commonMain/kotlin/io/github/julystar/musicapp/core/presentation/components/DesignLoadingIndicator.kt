package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults

@Composable
fun DesignLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = DesignPalette.Primary,
) {
    CircularProgressIndicator(
        modifier = modifier,
        colors = ProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = color,
        ),
        strokeWidth = strokeWidth,
        size = size,
    )
}
