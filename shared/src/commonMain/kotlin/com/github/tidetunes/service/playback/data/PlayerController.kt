package com.github.tidetunes.service.playback.data

import com.github.tidetunes.service.playback.domain.SleepController
import com.github.tidetunes.service.playback.domain.SleepModeState
import kotlinx.coroutines.flow.StateFlow
import uniffi.tidetunes_core.Playlist
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.PlaylistId

interface PlayerController : SleepController {
    override val sleepState: StateFlow<SleepModeState>
    fun getCurrentPosition(): Long
    fun getBufferedPosition(): Long
    fun getDuration(): Long
    fun play(id: MusicId, playlistId: PlaylistId)
    fun resume()
    fun pause()
    fun stop()
    fun playNext()
    fun playPrevious()
    fun seek(ms: ULong)
    override fun scheduleSleep(newExpiredMs: Long)
    fun refreshPlaylistIfMatch(playlist: Playlist)
    override fun cancelSleep()
}
