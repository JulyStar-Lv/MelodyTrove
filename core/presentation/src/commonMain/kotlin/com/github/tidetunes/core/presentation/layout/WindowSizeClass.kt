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
    Large,
    XL,
}

@Composable
fun rememberWindowSizeClass(
    containerSize: DpSize,
): WindowSizeClass {
    val widthDp = containerSize.width

    return remember(widthDp) { windowSizeClassFor(widthDp) }
}

fun windowSizeClassFor(widthDp: Dp): WindowSizeClass = when {
    widthDp < 600.dp -> WindowSizeClass.Compact
    widthDp < 840.dp -> WindowSizeClass.Medium
    widthDp < 1280.dp -> WindowSizeClass.Expanded
    widthDp < 1600.dp -> WindowSizeClass.Large
    else -> WindowSizeClass.XL
}
