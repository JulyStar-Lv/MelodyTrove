package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.service.playback.domain.PlayableItem
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngineResource
import io.github.julystar.musicapp.source.api.PlaybackResource
import uniffi.app_backend.Music
import uniffi.app_backend.MusicAbstract

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
