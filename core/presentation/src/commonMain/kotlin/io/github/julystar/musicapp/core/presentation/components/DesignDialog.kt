package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DesignDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!show) return

    val shape = RoundedCornerShape(DesignTokens.shapes.lg)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .widthIn(min = 280.dp, max = 520.dp)
                .clip(shape)
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .border(1.dp, MiuixTheme.colorScheme.outline, shape)
                .padding(20.dp),
            content = content,
        )
    }
}
