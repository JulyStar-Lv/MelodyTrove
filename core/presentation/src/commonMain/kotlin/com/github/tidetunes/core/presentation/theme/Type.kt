package com.github.tidetunes.core.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.defaultTextStyles

internal fun tideTunesTextStyles(): TextStyles {
    val defaults = defaultTextStyles()
    return defaults.copy(
        main = TideTunesTextStyle(size = 16, lineHeight = 24),
        paragraph = TideTunesTextStyle(size = 16, lineHeight = 24),
        body1 = TideTunesTextStyle(size = 16, lineHeight = 24),
        body2 = TideTunesTextStyle(size = 14, lineHeight = 20),
        button = TideTunesTextStyle(size = 14, lineHeight = 20, weight = FontWeight.Medium),
        footnote1 = TideTunesTextStyle(size = 12, lineHeight = 16),
        footnote2 = TideTunesTextStyle(size = 10, lineHeight = 14, weight = FontWeight.Medium),
        headline1 = TideTunesTextStyle(size = 18, lineHeight = 24, weight = FontWeight.SemiBold),
        headline2 = TideTunesTextStyle(size = 16, lineHeight = 22, weight = FontWeight.Medium),
        subtitle = TideTunesTextStyle(size = 13, lineHeight = 18, weight = FontWeight.Medium),
        title1 = TideTunesTextStyle(size = 32, lineHeight = 40, weight = FontWeight.Bold),
        title2 = TideTunesTextStyle(size = 22, lineHeight = 28, weight = FontWeight.SemiBold),
        title3 = TideTunesTextStyle(size = 18, lineHeight = 24, weight = FontWeight.SemiBold),
        title4 = TideTunesTextStyle(size = 16, lineHeight = 22, weight = FontWeight.Medium),
    )
}

private fun TideTunesTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)
