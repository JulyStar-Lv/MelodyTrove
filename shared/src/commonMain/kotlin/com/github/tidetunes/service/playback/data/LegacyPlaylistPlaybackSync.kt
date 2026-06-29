package com.github.tidetunes.service.playback.data

import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import com.github.tidetunes.core.domain.model.PlaylistSummary
import com.github.tidetunes.service.playback.domain.PlaylistPlaybackSync
import com.github.tidetunes.singleton.RoomLibraryStore
import kotlin.time.Duration.Companion.milliseconds

class LegacyPlaylistPlaybackSync(
    private val roomLibraryStore: RoomLibraryStore,
    private val playerController: PlayerController,
) : PlaylistPlaybackSync {
    override suspend fun refreshPlaylistIfCurrent(playlistId: Long) {
        val playlist = roomLibraryStore.getPlaylistById(playlistId) ?: return
        playerController.refreshPlaylistIfMatch(playlist)
    }

    override fun refreshPlaylistIfCurrent(
        summary: PlaylistSummary,
        tracks: List<DomainPlaylistTrack>,
    ) {
        playerController.refreshPlaylistIfMatch(
            buildLegacyPlaylist(
                summary = summary,
                entries = tracks.map { track ->
                    PlaylistMusicEntry(
                        id = track.trackId,
                        title = track.title,
                        duration = track.durationMs?.milliseconds,
                        sortOrder = track.sortOrder,
                    )
                },
            )
        )
    }
}
