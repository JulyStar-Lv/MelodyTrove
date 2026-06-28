package com.github.tidetunes.service.playback.data

import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackEngineResource
import com.github.tidetunes.source.api.PlaybackResource
import uniffi.tidetunes_core.Music
import uniffi.tidetunes_core.MusicAbstract

internal fun PlaybackResource.toPlaybackEngineResource(): PlaybackEngineResource {
    return PlaybackEngineResource(
        uri = uri,
        headers = headers,
        mimeType = mimeType,
        expiresAtEpochMs = expiresAtEpochMs,
        isLocal = isLocal,
    )
}

internal fun Music.toPlayableItem(playlistId: Long?): PlayableItem {
    return PlayableItem(
        title = meta.title,
        durationMs = meta.duration?.inWholeMilliseconds,
        libraryTrackId = meta.id.value,
        libraryPlaylistId = playlistId,
    )
}

internal fun MusicAbstract.toPlayableItem(playlistId: Long): PlayableItem {
    return PlayableItem(
        title = meta.title,
        durationMs = meta.duration?.inWholeMilliseconds,
        libraryTrackId = meta.id.value,
        libraryPlaylistId = playlistId,
    )
}
