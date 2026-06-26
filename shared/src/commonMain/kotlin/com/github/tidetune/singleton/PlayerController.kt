package com.github.tidetune.singleton

import kotlinx.coroutines.flow.StateFlow
import uniffi.tidetune_core.Playlist
import uniffi.tidetune_core.MusicId
import uniffi.tidetune_core.PlaylistId

interface PlayerController {
    val sleepState: StateFlow<SleepModeState>
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
    fun scheduleSleep(newExpiredMs: Long)
    fun refreshPlaylistIfMatch(playlist: Playlist)
    fun cancelSleep()
}
