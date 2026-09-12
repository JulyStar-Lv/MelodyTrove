package io.github.julystar.musicapp.car.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class CarColors(
    val backgroundBase: Color,
    val backgroundSubtle: Color,
    val panel: Color,
    val surface: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceSelected: Color,
    val surfacePressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textSummary: Color,
    val textActions: Color,
    val textDisabled: Color,
    val accentPrimary: Color,
    val accentSubtle: Color,
    val controlTrack: Color,
    val controlTrackDisabled: Color,
    val borderDefault: Color,
    val borderSubtle: Color,
    val focusBorder: Color,
    val error: Color,
)

val DarkCarColors = CarColors(
    backgroundBase = Color.Black,
    backgroundSubtle = Color(0xFF181818),
    panel = Color(0xFF242424),
    surface = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2D2D2D),
    surfaceSelected = Color(0xFF303030),
    surfacePressed = Color(0xFF393939),
    textPrimary = Color(0xFFF2F2F2),
    textSecondary = Color.White.copy(alpha = 0.8f),
    textSummary = Color.White.copy(alpha = 0.66f),
    textActions = Color.White.copy(alpha = 0.62f),
    textDisabled = Color(0xFF8A8A8A),
    accentPrimary = Color(0xFFFA2D48),
    accentSubtle = Color(0xFF4B0E16),
    controlTrack = Color(0xFF505050),
    controlTrackDisabled = Color(0xFF3F3F3F),
    borderDefault = Color(0xFF404040),
    borderSubtle = Color(0xFF393939),
    focusBorder = Color(0xFFFA2D48),
    error = Color(0xFFF12522),
)
