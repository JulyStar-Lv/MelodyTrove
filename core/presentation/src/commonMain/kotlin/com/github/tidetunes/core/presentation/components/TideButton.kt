package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class TideButtonVariant {
    Primary,
    Secondary,
    Tertiary,
    Ghost,
    Danger,
}

@Composable
fun TideButton(
    text: String,
    variant: TideButtonVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minWidth: Dp = 0.dp,
    minHeight: Dp = 40.dp,
    insideMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    TideButton(
        variant = variant,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minWidth = minWidth,
        minHeight = minHeight,
        insideMargin = insideMargin,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.button,
        )
    }
}

@Composable
fun TideButton(
    variant: TideButtonVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minWidth: Dp = 0.dp,
    minHeight: Dp = 40.dp,
    insideMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        cornerRadius = TideTunesTokens.shapes.full,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors ?: tideButtonColors(variant),
        insideMargin = insideMargin,
        content = content,
    )
}

@Composable
private fun tideButtonColors(variant: TideButtonVariant): ButtonColors {
    return when (variant) {
        TideButtonVariant.Primary -> ButtonDefaults.buttonColorsPrimary(
            color = MiuixTheme.colorScheme.primary,
            contentColor = MiuixTheme.colorScheme.onPrimary,
        )
        TideButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            color = MiuixTheme.colorScheme.secondaryVariant,
            contentColor = MiuixTheme.colorScheme.onSecondaryVariant,
        )
        TideButtonVariant.Tertiary -> ButtonDefaults.buttonColors(
            color = MiuixTheme.colorScheme.tertiaryContainer,
            contentColor = MiuixTheme.colorScheme.onTertiaryContainer,
        )
        TideButtonVariant.Ghost -> ButtonDefaults.buttonColors(
            color = Color.Transparent,
            disabledColor = Color.Transparent,
            contentColor = MiuixTheme.colorScheme.onSurface,
            disabledContentColor = MiuixTheme.colorScheme.disabledOnSurface,
        )
        TideButtonVariant.Danger -> ButtonDefaults.buttonColors(
            color = MiuixTheme.colorScheme.error,
            disabledColor = MiuixTheme.colorScheme.disabledPrimaryButton,
            contentColor = MiuixTheme.colorScheme.onError,
            disabledContentColor = MiuixTheme.colorScheme.disabledOnPrimaryButton,
        )
    }
}
