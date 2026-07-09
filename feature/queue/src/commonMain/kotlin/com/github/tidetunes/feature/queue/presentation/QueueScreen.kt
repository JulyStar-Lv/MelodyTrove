package com.github.tidetunes.feature.queue.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideStatusCard
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun QueueScreen(
    state: QueueState,
    onAction: (QueueAction) -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            TidePageHeader(
                title = "Queue",
                subtitle = "${state.items.size} tracks",
                trailing = {
                    if (state.items.isNotEmpty()) {
                        TideTextButton(text = "Clear", variant = TideTextButtonVariant.Default, size = TideTextButtonSize.Small, onClick = { onAction(QueueAction.ClearQueue) })
                    }
                },
            )
            if (state.items.isEmpty()) {
                TideStatusCard(title = "Empty queue", message = "Add tracks to start playing", modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = spacing.xl),
                ) {
                    itemsIndexed(state.items, key = { index, item -> item.lazyListKey(index) }) { _, item ->
                        QueueTrackRow(
                            item = item,
                            onPlay = { onAction(QueueAction.PlayItem(item.index)) },
                            onRemove = { onAction(QueueAction.RemoveItem(item.index)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(item: QueueItemUi, onPlay: () -> Unit, onRemove: () -> Unit) {
    val titleColor = if (item.isCurrent) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
    TideCardSurface(modifier = Modifier.heightIn(min = 58.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), onClick = onPlay) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.title, style = MiuixTheme.textStyles.body1, color = titleColor, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.artist?.let {
                    Text(text = it, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item.durationMs?.let { Text(text = durationLabel(it), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
            TideTextButton(text = "Remove", variant = TideTextButtonVariant.Default, size = TideTextButtonSize.Small, onClick = onRemove)
        }
    }
}

internal fun QueueItemUi.lazyListKey(index: Int): String = "queue-item-$index-${this.index}"

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
