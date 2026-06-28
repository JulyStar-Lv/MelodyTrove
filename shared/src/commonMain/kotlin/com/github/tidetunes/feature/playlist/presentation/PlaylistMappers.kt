package com.github.tidetunes.feature.playlist.presentation

import com.github.tidetunes.core.domain.model.PlaylistSummary
import com.github.tidetunes.core.toArtwork
import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.storage.legacyStorageTrackMediaIdOrNull
import com.github.tidetunes.core.utils.formatDuration
import kotlin.time.Duration.Companion.milliseconds

internal fun PlaylistSummary.toPlaylistHeaderState(
    current: PlaylistState,
): PlaylistState {
    return current.copy(
        playlistId = id,
        title = title,
        cover = coverArtwork,
        durationLabel = formatDuration(durationMs.milliseconds),
    )
}

internal suspend fun DomainPlaylistTrack.toPlaylistTrackItem(
    storageLookup: LegacyStorageLookup,
): PlaylistTrackItem {
    return PlaylistTrackItem(
        id = trackId,
        title = title,
        durationMs = durationMs,
        sortOrder = sortOrder,
        mediaId = legacyStorageTrackMediaIdOrNull(
            storageLookup = storageLookup,
            sourceStorageId = sourceStorageId,
            sourcePath = sourcePath,
        ),
    )
}
