package io.github.julystar.musicapp.core.presentation.components

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
    DesignTextButton(
        text = text,
        variant = DesignTextButtonVariant.Default,
        size = DesignTextButtonSize.Medium,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}
