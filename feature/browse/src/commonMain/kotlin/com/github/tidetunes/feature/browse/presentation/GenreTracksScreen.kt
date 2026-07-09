package com.github.tidetunes.feature.browse.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.github.tidetunes.core.presentation.components.TideTrackNumberBadge
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GenreTracksScreen(
    state: GenreTracksState,
    onAction: (GenreTracksAction) -> Unit,
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
                title = state.genre,
                subtitle = "${state.tracks.size} tracks",
                trailing = {
                    if (!state.isLoading && state.tracks.isNotEmpty()) {
                        TideTextButton(
                            text = "Play All",
                            variant = TideTextButtonVariant.PrimaryFilled,
                            size = TideTextButtonSize.Small,
                            onClick = { onAction(GenreTracksAction.PlayAll) },
                        )
                    }
                },
            )
            when {
                state.isLoading -> TideStatusCard(title = "Loading genre", message = state.genre, loading = true, modifier = Modifier.weight(1f))
                state.error != null -> TideStatusCard(title = "Genre unavailable", message = state.error, actionText = "Retry", onAction = { onAction(GenreTracksAction.Retry) }, modifier = Modifier.weight(1f))
                state.tracks.isEmpty() -> TideStatusCard(title = "No tracks", message = state.genre, modifier = Modifier.weight(1f))
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = spacing.xl),
                    ) {
                        itemsIndexed(state.tracks, key = { index, track -> track.lazyListKey(index) }) { _, track ->
                            GenreTrackRow(
                                track = track,
                                onPlay = { onAction(GenreTracksAction.PlayTrack(track.id)) },
                                onDownload = { if (track.canDownload) onAction(GenreTracksAction.DownloadTrack(track)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreTrackRow(
    track: GenreTrackItem,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    TideCardSurface(
        modifier = Modifier.heightIn(min = 58.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TideTrackNumberBadge(label = "")
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = track.title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                track.artist?.let {
                    Text(text = it, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            track.durationMs?.let { ms ->
                Text(text = durationLabel(ms), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (track.canDownload) {
                TideTextButton(text = "DL", variant = TideTextButtonVariant.Default, size = TideTextButtonSize.Small, onClick = onDownload)
            }
        }
    }
}

internal fun GenreTrackItem.lazyListKey(index: Int): String = "genre-track-$index-$id"

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
