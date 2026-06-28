package com.github.tidetunes.service.playback.domain

import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val state: StateFlow<PlayerState>
    val position: StateFlow<PlaybackPosition>
    val queue: StateFlow<PlaybackQueue>

    suspend fun play(
        items: List<PlayableItem>,
        startIndex: Int = 0,
    )

    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun setShuffle(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun moveQueueItem(from: Int, to: Int)
    fun removeQueueItem(index: Int)
    fun clearQueue()
}
