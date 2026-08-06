package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DesignBottomSheet(
    show: Boolean,
    title: String? = null,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    DesignDialog(
        show = show,
        onDismiss = onDismissRequest,
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.title3,
            )
        }
        content()
    }
}
