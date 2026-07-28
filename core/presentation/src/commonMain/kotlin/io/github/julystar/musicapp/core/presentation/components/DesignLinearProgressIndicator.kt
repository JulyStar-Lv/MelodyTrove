package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorColors
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults

@Composable
fun DesignLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.progressIndicatorColors(
        foregroundColor = DesignPalette.Primary,
    ),
) {
    LinearProgressIndicator(
        modifier = modifier,
        progress = progress,
        colors = colors,
    )
}
