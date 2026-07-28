package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun DesignBottomSheet(
    show: Boolean,
    title: String? = null,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        content = content,
    )
}
