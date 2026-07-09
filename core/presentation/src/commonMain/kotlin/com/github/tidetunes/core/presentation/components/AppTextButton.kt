package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButtonColors

/**
 * Compatibility text button wrapper.
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
) {
    TideTextButton(
        text = text,
        variant = TideTextButtonVariant.Default,
        size = TideTextButtonSize.Medium,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}
