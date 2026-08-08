package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorColors
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DesignLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.progressIndicatorColors(
        foregroundColor = MiuixTheme.colorScheme.primary,
    ),
) {
    LinearProgressIndicator(
        modifier = modifier,
        progress = progress,
        colors = colors,
    )
}
