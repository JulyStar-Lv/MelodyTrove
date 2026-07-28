package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import musicapp.core.presentation.generated.resources.Res
import musicapp.core.presentation.generated.resources.confirm_dialog_btn_cancel
import musicapp.core.presentation.generated.resources.confirm_dialog_btn_ok
import musicapp.core.presentation.generated.resources.confirm_dialog_title
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
    DesignDialog(show = open, onDismiss = onCancel) {
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
            DesignTextButton(
                text = stringResource(Res.string.confirm_dialog_btn_cancel),
                variant = DesignTextButtonVariant.Primary,
                size = DesignTextButtonSize.Medium,
                onClick = onCancel
            )
            DesignTextButton(
                text = stringResource(Res.string.confirm_dialog_btn_ok),
                variant = DesignTextButtonVariant.Primary,
                size = DesignTextButtonSize.Medium,
                onClick = onConfirm
            )
        }
    }
}
