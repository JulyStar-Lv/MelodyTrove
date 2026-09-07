package io.github.julystar.musicapp.car.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Immutable
data class CarTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val supporting: TextStyle,
)

val DefaultCarTypography = CarTypography(
    display = TextStyle(fontWeight = FontWeight.Bold),
    headline = TextStyle(fontWeight = FontWeight.Bold),
    title = TextStyle(fontWeight = FontWeight.SemiBold),
    body = TextStyle.Default,
    label = TextStyle(fontWeight = FontWeight.SemiBold),
    supporting = TextStyle.Default,
)
