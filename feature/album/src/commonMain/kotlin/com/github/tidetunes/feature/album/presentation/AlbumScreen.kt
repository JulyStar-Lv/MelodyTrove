package com.github.tidetunes.feature.album.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppEmptyState
import com.github.tidetunes.core.presentation.components.AppErrorState
import com.github.tidetunes.core.presentation.components.AppLoadingIndicator
import com.github.tidetunes.core.presentation.components.AppSectionHeader
import com.github.tidetunes.core.presentation.components.AppTopBar
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import com.github.tidetunes.core.presentation.media.ArtworkImage

@Composable
fun AlbumScreen(
    state: AlbumState,
    onAction: (AlbumAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.title,
            navigationIcon = null,
            actions = {
                if (!state.isLoading && state.tracks.isNotEmpty()) {
                    TideTunesTextButton(
                        text = "Play All",
                        type = TideTunesTextButtonType.Primary,
                        size = TideTunesTextButtonSize.Small,
                        onClick = { onAction(AlbumAction.PlayAll) },
                    )
                }
            },
        )

        when {
            state.isLoading -> AppLoadingIndicator()
            state.error != null -> AppErrorState(
                message = state.error,
                onRetry = { onAction(AlbumAction.Retry) },
            )
            state.tracks.isEmpty() -> AppEmptyState(message = "No tracks in this album.")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        AlbumHeader(
                            title = state.title,
                            artist = state.artist,
                            artwork = state.artwork,
                        )
                    }

                    item {
                        AppSectionHeader(title = "Tracks")
                    }

                    itemsIndexed(state.tracks) { index, track ->
                        AlbumTrackRow(
                            track = track,
                            onPlay = { onAction(AlbumAction.PlayTrack(track.id)) },
                            onDownload = {
                                if (track.canDownload) {
                                    onAction(AlbumAction.DownloadTrack(track))
                                }
                            },
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    title: String,
    artist: String,
    artwork: com.github.tidetunes.core.domain.model.Artwork?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            ArtworkImage(
                artwork = artwork,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (artist.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = artist,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AlbumTrackRow(
    track: AlbumTrackItem,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val trackLabel = buildString {
            if (track.discNumber != null && track.discNumber > 1) {
                append("${track.discNumber}.")
            }
            if (track.trackNumber != null) {
                if (isNotEmpty()) append("-")
                append("${track.trackNumber}")
            }
        }
        if (trackLabel.isNotEmpty()) {
            Text(
                text = trackLabel,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.width(36.dp),
            )
        } else {
            Spacer(Modifier.width(36.dp))
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        track.durationMs?.let { ms ->
            Text(
                text = durationLabel(ms),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        if (track.canDownload) {
            Spacer(Modifier.width(4.dp))
            TideTunesTextButton(
                text = "DL",
                type = TideTunesTextButtonType.Default,
                size = TideTunesTextButtonSize.Small,
                onClick = onDownload,
            )
        }
    }
}

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
