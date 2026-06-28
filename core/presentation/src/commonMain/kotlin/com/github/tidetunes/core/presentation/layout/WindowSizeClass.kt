package com.github.tidetunes.core.presentation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun rememberWindowSizeClass(
    containerSize: DpSize,
): WindowSizeClass {
    val widthDp = containerSize.width

    return remember(widthDp) {
        when {
            widthDp < 600.dp -> WindowSizeClass.Compact
            widthDp < 840.dp -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        }
    }
}
