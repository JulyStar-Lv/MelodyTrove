package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import com.github.tidetune.singleton.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uniffi.tidetune_core.PlaylistAbstract

data class PlaylistsState(
    val playlists: List<PlaylistAbstract> = listOf()
)

enum class PlaylistsMode {
    Normal,
    Adjust
}

class PlaylistsVM constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _mode = MutableStateFlow(PlaylistsMode.Normal)
    val playlists = playlistRepository.playlists

    val mode = _mode.asStateFlow()

    fun setMode(mode: PlaylistsMode) {
        _mode.value = mode
    }

    fun toggleMode() {
        _mode.value = when (_mode.value) {
            PlaylistsMode.Normal -> PlaylistsMode.Adjust
            PlaylistsMode.Adjust -> PlaylistsMode.Normal
        }
    }

    fun moveTo(fromIndex: Int, toIndex: Int) {
        playlistRepository.playlistMoveTo(fromIndex, toIndex)
    }
}