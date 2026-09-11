package io.github.julystar.musicapp.car.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class CarShapes(
    val navigationItem: Shape,
    val panel: Shape,
    val card: Shape,
    val artwork: Shape,
    val control: Shape,
)

val DefaultCarShapes = CarShapes(
    navigationItem = RoundedCornerShape(18.dp),
    panel = RoundedCornerShape(24.dp),
    card = RoundedCornerShape(20.dp),
    artwork = RoundedCornerShape(18.dp),
    control = RoundedCornerShape(28.dp),
)
