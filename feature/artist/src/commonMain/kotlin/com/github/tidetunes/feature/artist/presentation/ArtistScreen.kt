package com.github.tidetunes.feature.artist.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
fun ArtistScreen(
    state: ArtistState,
    onAction: (ArtistAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.name,
            navigationIcon = null,
            actions = {
                if (!state.isLoading && state.tracks.isNotEmpty()) {
                    TideTunesTextButton(
                        text = "Play All",
                        type = TideTunesTextButtonType.Primary,
                        size = TideTunesTextButtonSize.Small,
                        onClick = { onAction(ArtistAction.PlayAll) },
                    )
                }
            },
        )

        when {
            state.isLoading -> AppLoadingIndicator()
            state.error != null -> AppErrorState(
                message = state.error,
                onRetry = { onAction(ArtistAction.Retry) },
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ArtistHeader(
                            name = state.name,
                            artwork = state.artwork,
                        )
                    }

                    if (state.albums.isNotEmpty()) {
                        item {
                            AppSectionHeader(title = "Albums")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.albums, key = { it.id }) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAction(ArtistAction.NavigateToAlbum(album.id)) },
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    if (state.tracks.isNotEmpty()) {
                        item {
                            AppSectionHeader(title = "Top Tracks")
                        }
                        itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                            ArtistTrackRow(
                                track = track,
                                onPlay = { onAction(ArtistAction.PlayTrack(track.id)) },
                                onAlbumClick = track.albumId?.let { id ->
                                    ({ onAction(ArtistAction.NavigateToAlbum(id)) })
                                },
                                onDownload = {
                                    if (track.canDownload) {
                                        onAction(ArtistAction.DownloadTrack(track))
                                    }
                                },
                            )
                        }
                    }

                    if (state.albums.isEmpty() && state.tracks.isEmpty()) {
                        item {
                            AppEmptyState(message = "No content found for this artist.")
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    name: String,
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
                .clip(RoundedCornerShape(100.dp)),
        ) {
            ArtworkImage(
                artwork = artwork,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumCard(
    album: ArtistAlbumItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            ArtworkImage(
                artwork = album.artwork,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        album.year?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ArtistTrackRow(
    track: ArtistTrackItem,
    onPlay: () -> Unit,
    onAlbumClick: (() -> Unit)?,
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp),
            )
        } else {
            Spacer(Modifier.width(36.dp))
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            track.albumName?.let { album ->
                Text(
                    text = album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onAlbumClick != null) {
                        Modifier.clickable(onClick = onAlbumClick)
                    } else {
                        Modifier
                    },
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        track.durationMs?.let { ms ->
            Text(
                text = durationLabel(ms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
