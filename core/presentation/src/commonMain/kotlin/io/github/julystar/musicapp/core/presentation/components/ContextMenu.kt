package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class DesignContextMenuItem(
    val label: StringResource,
    val icon: DrawableResource,
    val onClick: () -> Unit,
    val isError: Boolean = false
) {
}

@Composable
fun DesignContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<DesignContextMenuItem>,
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
                .clip(RoundedCornerShape(DesignTokens.shapes.sm))
                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                .width(IntrinsicSize.Max)
                .widthIn(
                    min = if (compact) 144.dp else 160.dp,
                    max = if (compact) 240.dp else 280.dp,
                )
                .padding(vertical = if (compact) 4.dp else 6.dp),
        ) {
            for (item in items) {
                val label = stringResource(item.label)
                val color = if (item.isError) {
                    MiuixTheme.colorScheme.error
                } else {
                    MiuixTheme.colorScheme.onSurface
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = if (compact) 36.dp else DesignTokens.adaptive.minimumTouchTarget,
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
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = label,
                        color = color,
                        style = if (compact) MiuixTheme.textStyles.body2 else MiuixTheme.textStyles.body1,
                    )
                }
            }
        }
    }
}
