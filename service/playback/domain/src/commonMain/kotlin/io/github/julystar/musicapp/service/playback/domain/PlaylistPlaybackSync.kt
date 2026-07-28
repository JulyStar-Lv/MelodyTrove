package io.github.julystar.musicapp.service.playback.domain

import io.github.julystar.musicapp.core.domain.model.DomainPlaylistTrack
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary

interface PlaylistPlaybackSync {
    suspend fun refreshPlaylistIfCurrent(playlistId: Long)
    fun refreshPlaylistIfCurrent(
        summary: PlaylistSummary,
        tracks: List<DomainPlaylistTrack>,
    )
}
