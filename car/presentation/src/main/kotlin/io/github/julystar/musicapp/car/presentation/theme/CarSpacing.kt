package io.github.julystar.musicapp.car.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class CarSpacing(
    val xSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val compact: Dp = 12.dp,
    val content: Dp = 16.dp,
    val section: Dp = 24.dp,
    val pane: Dp = 40.dp,
    val large: Dp = 64.dp,
    val expansive: Dp = 80.dp,
)

val DefaultCarSpacing = CarSpacing()
