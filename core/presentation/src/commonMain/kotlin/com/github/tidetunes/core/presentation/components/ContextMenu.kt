package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class TideContextMenuItem(
    val label: StringResource,
    val onClick: () -> Unit,
    val isError: Boolean = false
) {
}

@Composable
fun TideContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<TideContextMenuItem>,
    compact: Boolean = false,
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
                .clip(RoundedCornerShape(TideTunesTokens.shapes.sm))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .then(if (compact) Modifier.widthIn(min = 168.dp, max = 168.dp) else Modifier.widthIn(min = 180.dp))
                .padding(vertical = if (compact) 4.dp else 6.dp),
        ) {
            for (item in items) {
                val label = stringResource(item.label)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = if (compact) 36.dp else TideTunesTokens.adaptive.minimumTouchTarget,
                        )
                        .clickable {
                            scope.launch {
                                delay(160)
                                onDismissRequest()
                            }
                            item.onClick()
                        }
                        .padding(
                            horizontal = if (compact) 12.dp else 16.dp,
                            vertical = if (compact) 4.dp else 10.dp,
                        ),
                ) {
                    val color = if (!item.isError) Color.Unspecified else MiuixTheme.colorScheme.error
                    if (compact) {
                        Text(
                            text = label,
                            color = color,
                            style = MiuixTheme.textStyles.body2,
                        )
                    } else {
                        Text(text = label, color = color)
                    }
                }
            }
        }
    }
}
