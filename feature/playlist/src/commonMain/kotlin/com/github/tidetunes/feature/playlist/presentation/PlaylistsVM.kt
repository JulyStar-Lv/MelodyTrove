package com.github.tidetunes.feature.playlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.PlaylistSummary
import kotlin.time.Duration.Companion.milliseconds
import com.github.tidetunes.feature.playlist.presentation.PlaylistListItem
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListAction
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListEvent
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListMode
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListState
import com.github.tidetunes.core.domain.repository.PlaylistRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PlaylistsVM constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _mode = MutableStateFlow(PlaylistsListMode.Normal)
    private val _state = MutableStateFlow(PlaylistsListState())
    private val _events = Channel<PlaylistsListEvent>(Channel.BUFFERED)

    val mode = _mode.asStateFlow()

    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                playlistRepository.playlistSummaries,
                _mode
            ) { summaries, mode ->
                val items = summaries.map { it.toListItem() }
                PlaylistsListState(
                    playlists = items.toImmutableList(),
                    mode = mode,
                    isEmpty = items.isEmpty(),
                )
            }.collect { _state.value = it }
        }
    }

    fun onAction(action: PlaylistsListAction) {
        when (action) {
            PlaylistsListAction.ToggleMode -> toggleMode()
            PlaylistsListAction.SetModeNormal -> setMode(PlaylistsListMode.Normal)
            is PlaylistsListAction.MovePlaylist -> moveTo(action.fromIndex, action.toIndex)
            // CreatePlaylist and NavigateToPlaylist are handled by Root callbacks
            else -> {}
        }
    }

    private fun setMode(mode: PlaylistsListMode) {
        _mode.value = mode
    }

    private fun toggleMode() {
        _mode.value = when (_mode.value) {
            PlaylistsListMode.Normal -> PlaylistsListMode.Adjust
            PlaylistsListMode.Adjust -> PlaylistsListMode.Normal
        }
    }

    private fun moveTo(fromIndex: Int, toIndex: Int) {
        playlistRepository.playlistMoveTo(fromIndex, toIndex)
    }

    companion object {
        internal fun PlaylistSummary.toListItem(): PlaylistListItem {
            return PlaylistListItem(
                id = id,
                title = title,
                musicCount = "$musicCount",
                durationLabel = formatDuration(durationMs.milliseconds),
                cover = coverArtwork,
            )
        }
    }
}
