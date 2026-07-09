package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults

@Composable
fun TideLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = TideTunesBrand.Primary,
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
