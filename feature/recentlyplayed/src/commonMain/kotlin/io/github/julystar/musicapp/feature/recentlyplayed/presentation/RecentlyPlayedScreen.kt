package io.github.julystar.musicapp.feature.recentlyplayed.presentation

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
import io.github.julystar.musicapp.core.presentation.components.DesignCardSurface
import io.github.julystar.musicapp.core.presentation.components.LocalDesignBottomContentInset
import io.github.julystar.musicapp.core.presentation.components.DesignPageHeader
import io.github.julystar.musicapp.core.presentation.components.DesignStatusCard
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RecentlyPlayedScreen(
    state: RecentlyPlayedState,
    onAction: (RecentlyPlayedAction) -> Unit,
) {
    val spacing = DesignTokens.spacing
    val bottomContentInset = LocalDesignBottomContentInset.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            DesignPageHeader(
                title = "Recently Played",
                subtitle = "${state.tracks.size} tracks",
                trailing = {
                    if (!state.isLoading && state.tracks.isNotEmpty()) {
                        DesignTextButton(text = "Play All", variant = DesignTextButtonVariant.PrimaryFilled, size = DesignTextButtonSize.Small, onClick = { onAction(RecentlyPlayedAction.PlayAll) })
                    }
                },
            )
            when {
                state.isLoading -> DesignStatusCard(title = "Loading recently played", message = "Checking library…", loading = true, modifier = Modifier.weight(1f))
                state.error != null -> DesignStatusCard(title = "Recently played unavailable", message = state.error, actionText = "Retry", onAction = { onAction(RecentlyPlayedAction.Retry) }, modifier = Modifier.weight(1f))
                state.tracks.isEmpty() -> DesignStatusCard(title = "No recently played tracks", message = "Play some music first", modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = spacing.xl + bottomContentInset),
                ) {
                    itemsIndexed(state.tracks, key = { index, track -> track.lazyListKey(index) }) { _, track ->
                        RecentlyPlayedTrackRow(track = track, onPlay = { onAction(RecentlyPlayedAction.PlayTrack(track.id)) }, onDownload = { if (track.canDownload) onAction(RecentlyPlayedAction.DownloadTrack(track)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedTrackRow(track: RecentlyPlayedTrackItem, onPlay: () -> Unit, onDownload: () -> Unit) {
    DesignCardSurface(modifier = Modifier.heightIn(min = 58.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), onClick = onPlay) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = track.title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                track.artist?.let { Text(text = it, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            track.durationMs?.let { Text(text = durationLabel(it), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
            if (track.canDownload) DesignTextButton(text = "DL", variant = DesignTextButtonVariant.Default, size = DesignTextButtonSize.Small, onClick = onDownload)
        }
    }
}

internal fun RecentlyPlayedTrackItem.lazyListKey(index: Int): String = "recently-played-track-$index-$id"

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
