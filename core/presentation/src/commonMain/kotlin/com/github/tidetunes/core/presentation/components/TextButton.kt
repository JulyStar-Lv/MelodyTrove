package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class TideTextButtonVariant {
    Primary,
    PrimaryFilled,
    Error,
    Default,
}

enum class TideTextButtonSize {
    Medium,
    Small,
}

enum class TideTunesTextButtonType {
    Primary,
    PrimaryVariant,
    Error,
    Default,
}

enum class TideTunesTextButtonSize {
    Medium,
    Small,
}

@Composable
fun TideTunesTextButton(
    text: String,
    type: TideTunesTextButtonType,
    size: TideTunesTextButtonSize,
    onClick: () -> Unit,
    disabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    TideTextButton(
        text = text,
        variant = type.toTideTextButtonVariant(),
        size = size.toTideTextButtonSize(),
        onClick = onClick,
        enabled = !disabled,
        modifier = modifier,
    )
}

@Composable
fun TideTextButton(
    text: String,
    variant: TideTextButtonVariant,
    size: TideTextButtonSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: TextButtonColors? = null,
) {
    val buttonColors = colors ?: tideTextButtonColors(variant)

    TextButton(
        modifier = modifier.padding(0.dp),
        colors = buttonColors,
        onClick = onClick,
        enabled = enabled,
        text = text,
        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        minHeight = if (size == TideTextButtonSize.Small) 28.dp else 36.dp,
        minWidth = 0.dp,
    )
}

@Composable
private fun tideTextButtonColors(variant: TideTextButtonVariant): TextButtonColors {
    return when (variant) {
        TideTextButtonVariant.Default -> ButtonDefaults.textButtonColors(
            color = Color.Transparent,
            textColor = MiuixTheme.colorScheme.onSurface,
        )
        TideTextButtonVariant.Primary -> ButtonDefaults.textButtonColors(
            color = Color.Transparent,
            textColor = MiuixTheme.colorScheme.primary,
        )
        TideTextButtonVariant.PrimaryFilled -> ButtonDefaults.textButtonColors(
            color = MiuixTheme.colorScheme.primary,
            textColor = MiuixTheme.colorScheme.onPrimary,
        )
        TideTextButtonVariant.Error -> ButtonDefaults.textButtonColors(
            color = Color.Transparent,
            textColor = MiuixTheme.colorScheme.error,
        )
    }
}

private fun TideTunesTextButtonType.toTideTextButtonVariant(): TideTextButtonVariant {
    return when (this) {
        TideTunesTextButtonType.Primary -> TideTextButtonVariant.Primary
        TideTunesTextButtonType.PrimaryVariant -> TideTextButtonVariant.PrimaryFilled
        TideTunesTextButtonType.Error -> TideTextButtonVariant.Error
        TideTunesTextButtonType.Default -> TideTextButtonVariant.Default
    }
}

private fun TideTunesTextButtonSize.toTideTextButtonSize(): TideTextButtonSize {
    return when (this) {
        TideTunesTextButtonSize.Medium -> TideTextButtonSize.Medium
        TideTunesTextButtonSize.Small -> TideTextButtonSize.Small
    }
}
