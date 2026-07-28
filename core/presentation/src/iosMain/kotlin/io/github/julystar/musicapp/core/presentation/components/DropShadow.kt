package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp

actual fun Modifier.dropShadow(
    color: Color,
    offsetX: Dp,
    offsetY: Dp,
    blurRadius: Dp,
): Modifier = then(
    Modifier.shadow(
        elevation = blurRadius,
        shape = RectangleShape,
        clip = false,
        ambientColor = color,
        spotColor = color,
    )
)
