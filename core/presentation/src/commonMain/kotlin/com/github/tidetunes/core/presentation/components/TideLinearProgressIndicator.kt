package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorColors
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults

@Composable
fun TideLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.progressIndicatorColors(
        foregroundColor = TideTunesBrand.Primary,
    ),
) {
    LinearProgressIndicator(
        modifier = modifier,
        progress = progress,
        colors = colors,
    )
}
