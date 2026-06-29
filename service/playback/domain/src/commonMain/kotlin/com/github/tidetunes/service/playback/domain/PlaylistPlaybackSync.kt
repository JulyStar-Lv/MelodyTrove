package com.github.tidetunes.service.playback.domain

import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import com.github.tidetunes.core.domain.model.PlaylistSummary

interface PlaylistPlaybackSync {
    suspend fun refreshPlaylistIfCurrent(playlistId: Long)
    fun refreshPlaylistIfCurrent(
        summary: PlaylistSummary,
        tracks: List<DomainPlaylistTrack>,
    )
}
