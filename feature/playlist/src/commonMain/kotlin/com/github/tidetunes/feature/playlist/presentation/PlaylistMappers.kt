package com.github.tidetunes.feature.playlist.presentation

import com.github.tidetunes.core.domain.model.PlaylistSummary
import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import kotlin.time.Duration.Companion.milliseconds

fun PlaylistSummary.toPlaylistHeaderState(
    current: PlaylistState,
): PlaylistState {
    return current.copy(
        playlistId = id,
        title = title,
        cover = coverArtwork,
        durationLabel = formatDuration(durationMs.milliseconds),
    )
}

fun DomainPlaylistTrack.toPlaylistTrackItem(): PlaylistTrackItem {
    return PlaylistTrackItem(
        id = trackId,
        title = title,
        durationMs = durationMs,
        sortOrder = sortOrder,
        mediaId = mediaId,
    )
}
