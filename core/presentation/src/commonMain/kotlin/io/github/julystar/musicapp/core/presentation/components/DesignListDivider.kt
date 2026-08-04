package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val ListDividerAlpha = 0.05f

@Composable
fun DesignListDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MiuixTheme.colorScheme.dividerLine.copy(alpha = ListDividerAlpha)),
    )
}

@Composable
fun Modifier.designListDivider(): Modifier {
    val color = MiuixTheme.colorScheme.dividerLine.copy(alpha = ListDividerAlpha)
    return drawBehind {
        val strokeWidth = 1.dp.toPx()
        val y = size.height - strokeWidth / 2f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
    }
}
