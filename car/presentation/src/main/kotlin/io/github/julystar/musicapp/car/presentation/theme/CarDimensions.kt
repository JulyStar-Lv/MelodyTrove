package io.github.julystar.musicapp.car.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class CarDimensions(
    val focusBorderWidth: Dp = 3.dp,
    val dividerWidth: Dp = 1.dp,
)

@Immutable
data class CarTouchTargets(
    val minimum: Dp = Minimum,
    val exit: Dp = 72.dp,
    val navigation: Dp = 84.dp,
    val miniControl: Dp = 56.dp,
    val playerControl: Dp = 112.dp,
    val primaryControl: Dp = 128.dp,
) {
    companion object {
        val Minimum: Dp = 48.dp
    }
}

@Immutable
data class CarFocusVisuals(
    val borderWidth: Dp = 3.dp,
)
