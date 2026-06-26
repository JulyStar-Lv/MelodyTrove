package com.github.tidetune.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import tidetune.shared.generated.resources.Res
import tidetune.shared.generated.resources.confirm_dialog_btn_cancel
import tidetune.shared.generated.resources.confirm_dialog_btn_ok
import tidetune.shared.generated.resources.confirm_dialog_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp, 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.confirm_dialog_title),
                color = MaterialTheme.colorScheme.error,
            )
            Box(modifier = Modifier.height(4.dp))
            content()
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TideTuneTextButton(
                    text = stringResource(Res.string.confirm_dialog_btn_cancel),
                    type = TideTuneTextButtonType.Primary,
                    size = TideTuneTextButtonSize.Medium,
                    onClick = onCancel
                )
                TideTuneTextButton(
                    text = stringResource(Res.string.confirm_dialog_btn_ok),
                    type = TideTuneTextButtonType.Primary,
                    size = TideTuneTextButtonSize.Medium,
                    onClick = onConfirm
                )
            }
        }
    }
}
