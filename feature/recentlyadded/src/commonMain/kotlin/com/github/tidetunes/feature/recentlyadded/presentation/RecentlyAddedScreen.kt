package com.github.tidetunes.feature.recentlyadded.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppEmptyState
import com.github.tidetunes.core.presentation.components.AppErrorState
import com.github.tidetunes.core.presentation.components.AppLoadingIndicator
import com.github.tidetunes.core.presentation.components.AppTopBar
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType

@Composable
fun RecentlyAddedScreen(
    state: RecentlyAddedState,
    onAction: (RecentlyAddedAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Recently Added",
            navigationIcon = null,
            actions = {
                if (!state.isLoading && state.tracks.isNotEmpty()) {
                    TideTunesTextButton(
                        text = "Play All",
                        type = TideTunesTextButtonType.Primary,
                        size = TideTunesTextButtonSize.Small,
                        onClick = { onAction(RecentlyAddedAction.PlayAll) },
                    )
                }
            },
        )
        when {
            state.isLoading -> AppLoadingIndicator()
            state.error != null -> AppErrorState(message = state.error, onRetry = { onAction(RecentlyAddedAction.Retry) })
            state.tracks.isEmpty() -> AppEmptyState(message = "No recently added tracks.")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.tracks, key = { index, track -> track.lazyListKey(index) }) { _, track ->
                    RecentlyAddedTrackRow(
                        track = track,
                        onPlay = { onAction(RecentlyAddedAction.PlayTrack(track.id)) },
                        onDownload = { if (track.canDownload) onAction(RecentlyAddedAction.DownloadTrack(track)) },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun RecentlyAddedTrackRow(
    track: RecentlyAddedTrackItem,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = track.title, style = MiuixTheme.textStyles.body2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            track.artist?.let {
                Text(text = it, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        track.durationMs?.let { ms ->
            Text(text = durationLabel(ms), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        if (track.canDownload) {
            Spacer(Modifier.width(4.dp))
            TideTunesTextButton(text = "DL", type = TideTunesTextButtonType.Default, size = TideTunesTextButtonSize.Small, onClick = onDownload)
        }
    }
}

internal fun RecentlyAddedTrackItem.lazyListKey(index: Int): String =
    "recently-added-track-$index-$id"

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
