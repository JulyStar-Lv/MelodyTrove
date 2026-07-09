package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tidetunes.core.presentation.generated.resources.Res
import tidetunes.core.presentation.generated.resources.confirm_dialog_btn_cancel
import tidetunes.core.presentation.generated.resources.confirm_dialog_btn_ok
import tidetunes.core.presentation.generated.resources.confirm_dialog_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConfirmDialog(
    open: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    TideDialog(show = open, onDismiss = onCancel) {
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
            TideTextButton(
                text = stringResource(Res.string.confirm_dialog_btn_cancel),
                variant = TideTextButtonVariant.Primary,
                size = TideTextButtonSize.Medium,
                onClick = onCancel
            )
            TideTextButton(
                text = stringResource(Res.string.confirm_dialog_btn_ok),
                variant = TideTextButtonVariant.Primary,
                size = TideTextButtonSize.Medium,
                onClick = onConfirm
            )
        }
    }
}
