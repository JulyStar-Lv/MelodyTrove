package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.core.domain.model.DomainPlaylistTrack
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import io.github.julystar.musicapp.service.playback.domain.PlaylistPlaybackSync
import io.github.julystar.musicapp.singleton.RoomLibraryStore
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
