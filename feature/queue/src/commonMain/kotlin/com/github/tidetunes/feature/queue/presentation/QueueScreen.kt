package com.github.tidetunes.feature.queue.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppEmptyState
import com.github.tidetunes.core.presentation.components.AppTopBar
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType

@Composable
fun QueueScreen(
    state: QueueState,
    onAction: (QueueAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Queue",
            navigationIcon = null,
            actions = {
                if (state.items.isNotEmpty()) {
                    TideTunesTextButton(
                        text = "Clear",
                        type = TideTunesTextButtonType.Default,
                        size = TideTunesTextButtonSize.Small,
                        onClick = { onAction(QueueAction.ClearQueue) },
                    )
                }
            },
        )
        if (state.items.isEmpty()) {
            AppEmptyState(message = "Queue is empty.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.items, key = { _, item -> item.index }) { _, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onAction(QueueAction.PlayItem(item.index)) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (item.isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            item.artist?.let {
                                Text(text = it, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        item.durationMs?.let {
                            Text(text = durationLabel(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        TideTunesTextButton(
                            text = "✕",
                            type = TideTunesTextButtonType.Default,
                            size = TideTunesTextButtonSize.Small,
                            onClick = { onAction(QueueAction.RemoveItem(item.index)) },
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

private fun durationLabel(durationMs: Long): String {
    val all = durationMs
    val h = all / 1000 / 60 / 60
    val m = all / 1000 / 60 % 60
    val s = all / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
