package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetune.singleton.PlayerController
import com.github.tidetune.singleton.PlayerRepository
import com.github.tidetune.singleton.ToastRepository
import com.github.tidetune.utils.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uniffi.tidetune_core.DataSourceKey
import uniffi.tidetune_core.MusicId
import uniffi.tidetune_core.PlaylistId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class PlayerVM constructor(
    private val playerRepository: PlayerRepository,
    private val playerControllerRepository: PlayerController
) : ViewModel() {
    private val _currentDuration = MutableStateFlow(Duration.ZERO)
    private val _bufferDuration = MutableStateFlow(Duration.ZERO)
    private val _playerDuration = MutableStateFlow(Duration.ZERO)
    val music = playerRepository.music
    val previousMusic = playerRepository.previousMusic
    val nextMusic = playerRepository.nextMusic
    val playing = playerRepository.playing
    val currentDuration = _currentDuration.asStateFlow()
    val bufferDuration = _bufferDuration.asStateFlow()
    val playerDuration = _playerDuration.asStateFlow()
    val playMode = playerRepository.playMode
    val loading = playerRepository.loading

    val lyricIndex = combine(currentDuration, music) {
        currentDuration, music ->
            music?.lyric?.data?.lines?.indexOfLast { it.duration <= currentDuration } ?: -1
    }.stateIn(viewModelScope, SharingStarted.Lazily, -1)

    init {
        viewModelScope.launch {
            while (true) {
                syncPosition()
                delay(1000)
            }
        }
        viewModelScope.launch {
            playerRepository.durationChanged.collect {
                syncPosition()
            }
        }
    }

    fun resume() {
        playerControllerRepository.resume()
    }

    fun pause() {
        playerControllerRepository.pause()
    }

    fun stop() {
        playerControllerRepository.stop()
    }

    fun playNext() {
        playerControllerRepository.playNext()
    }

    fun playPrevious() {
        playerControllerRepository.playPrevious()
    }

    fun remove() {
        playerRepository.remove()
    }

    fun seek(ms: ULong) {
        playerControllerRepository.seek(ms)
    }

    fun play(id: MusicId, playlistId: PlaylistId) {
        playerControllerRepository.play(id, playlistId)
    }

    fun changePlayModeToNext() {
        playerRepository.changePlayModeToNext()
    }

    fun removeLyric() {
        playerRepository.removeLyric()
    }

    fun syncPosition() {
        _currentDuration.value = playerControllerRepository.getCurrentPosition().milliseconds
        _bufferDuration.value = playerControllerRepository.getBufferedPosition().milliseconds
        _playerDuration.value = playerControllerRepository.getDuration().milliseconds
    }
}
