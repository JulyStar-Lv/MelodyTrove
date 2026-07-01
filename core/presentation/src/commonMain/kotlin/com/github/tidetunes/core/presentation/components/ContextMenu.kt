package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class TideTunesContextMenuItem(
    val label: StringResource,
    val onClick: () -> Unit,
    val isError: Boolean = false
) {
}

@Composable
fun TideTunesContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<TideTunesContextMenuItem>
) {
    val scope = rememberCoroutineScope()

    if (!expanded) {
        return
    }

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .widthIn(min = 180.dp)
                .padding(vertical = 6.dp),
        ) {
            for (item in items) {
                val label = stringResource(item.label)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                delay(160)
                                onDismissRequest()
                            }
                            item.onClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = label,
                        color = if (!item.isError) Color.Unspecified else MiuixTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
