package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors

/**
 * Miuix-based text button wrapper.
 *
 * Mirrors the existing [TideTunesTextButton] API contract but delegates to
 * Miuix's [TextButton]. Feature screens should use this wrapper instead of
 * calling Miuix APIs directly.
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}
