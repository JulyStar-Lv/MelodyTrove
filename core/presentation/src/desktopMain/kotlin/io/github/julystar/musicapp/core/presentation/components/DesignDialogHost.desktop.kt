package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun DesignDialogHost(
    onDismissRequest: () -> Unit,
    dismissOnClickOutside: Boolean,
    navigationBarStyle: DesignDialogNavigationBarStyle,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
        content = content,
    )
}

@Composable
internal actual fun DesignDialogSystemBarsEffect(
    navigationBarStyle: DesignDialogNavigationBarStyle,
) = Unit
