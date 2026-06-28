package com.github.tidetunes.feature.lyrics.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LyricsViewModel(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(LyricsState())
    private val _events = Channel<LyricsEvent>(Channel.BUFFERED)
    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    private val trackId: Long = savedStateHandle["id"]!!

    init {
        load()
    }

    fun onAction(action: LyricsAction) {
        when (action) {
            LyricsAction.NavigateBack -> Unit
            LyricsAction.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val track = trackDao.findByIds(listOf(trackId)).firstOrNull()
                val lyrics = metadataDao.getLyrics(trackId)
                val artistNames = metadataDao.artistNamesForTrack(trackId)

                val lines = lyrics?.content
                    ?.lines()
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()

                _state.value = LyricsState(
                    isLoading = false,
                    trackTitle = track?.title ?: "Unknown Track",
                    trackArtist = artistNames.joinToString(", ").ifBlank { track?.artist },
                    lines = lines.toPersistentList(),
                    format = lyrics?.format,
                    synchronized = lyrics?.synchronized ?: false,
                )
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load lyrics")
            }
        }
    }
}
