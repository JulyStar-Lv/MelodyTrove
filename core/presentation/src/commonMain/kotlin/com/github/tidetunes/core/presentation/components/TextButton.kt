package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val buttonColors = when (type) {
        TideTunesTextButtonType.Default -> ButtonDefaults.textButtonColors(
            color = Color.Transparent,
            textColor = MiuixTheme.colorScheme.onSurface,
        )
        TideTunesTextButtonType.Primary -> {
            ButtonDefaults.textButtonColors(
                color = Color.Transparent,
                textColor = MiuixTheme.colorScheme.primary,
            )
        }
        TideTunesTextButtonType.PrimaryVariant -> {
            ButtonDefaults.textButtonColors(
                color = MiuixTheme.colorScheme.primary,
                textColor = MiuixTheme.colorScheme.onPrimary,
            )
        }
        TideTunesTextButtonType.Error -> {
            ButtonDefaults.textButtonColors(
                color = Color.Transparent,
                textColor = MiuixTheme.colorScheme.error,
            )
        }
    }

    TextButton(
        modifier = modifier.padding(0.dp),
        colors = buttonColors,
        onClick = onClick,
        enabled = !disabled,
        text = text,
        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        minHeight = if (size == TideTunesTextButtonSize.Small) 28.dp else 36.dp,
        minWidth = 0.dp,
    )
}
