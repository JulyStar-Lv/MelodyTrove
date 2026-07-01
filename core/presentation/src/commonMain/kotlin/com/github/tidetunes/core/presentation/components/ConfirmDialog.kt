package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import tidetunes.core.presentation.generated.resources.Res
import tidetunes.core.presentation.generated.resources.confirm_dialog_btn_cancel
import tidetunes.core.presentation.generated.resources.confirm_dialog_btn_ok
import tidetunes.core.presentation.generated.resources.confirm_dialog_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConfirmDialog(
    open: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!open) {
        return
    }

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(24.dp, 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.confirm_dialog_title),
                color = MiuixTheme.colorScheme.error,
            )
            Box(modifier = Modifier.height(4.dp))
            content()
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TideTunesTextButton(
                    text = stringResource(Res.string.confirm_dialog_btn_cancel),
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = onCancel
                )
                TideTunesTextButton(
                    text = stringResource(Res.string.confirm_dialog_btn_ok),
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = onConfirm
                )
            }
        }
    }
}
