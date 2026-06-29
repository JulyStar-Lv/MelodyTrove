package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import com.github.tidetunes.core.domain.model.PlaylistSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlaylistRepository {
    val playlistSummaries: StateFlow<List<PlaylistSummary>>
    val playlistRefreshEvents: Flow<Unit>
    fun playlistMoveTo(fromIndex: Int, toIndex: Int)
    fun scheduleReload()
    fun observePlaylistTracks(playlistId: Long): Flow<List<DomainPlaylistTrack>>
    fun removePlaylist(id: Long)
    fun requestTotalDurationById(addedTrackIds: List<Long>)
    suspend fun removeMusic(playlistId: Long, musicId: Long)
    suspend fun replaceMusicOrderById(
        playlistId: Long,
        orderedTrackIds: List<Long>,
    )
}
