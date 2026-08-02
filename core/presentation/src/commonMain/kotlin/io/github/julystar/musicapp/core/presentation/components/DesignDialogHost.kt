package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable

enum class DesignDialogNavigationBarStyle {
    Dimmed,
    Surface,
}

@Composable
expect fun DesignDialogHost(
    onDismissRequest: () -> Unit,
    dismissOnClickOutside: Boolean = true,
    navigationBarStyle: DesignDialogNavigationBarStyle = DesignDialogNavigationBarStyle.Dimmed,
    content: @Composable () -> Unit,
)

@Composable
internal expect fun DesignDialogSystemBarsEffect(
    navigationBarStyle: DesignDialogNavigationBarStyle,
)
