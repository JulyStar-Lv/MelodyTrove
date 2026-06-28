package com.github.tidetunes.feature.browse.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.repository.TrackBrowserRepository
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface GenreTracksEvent {
    data class ShowMessage(val message: String) : GenreTracksEvent
}

class GenreTracksViewModel(
    private val trackBrowserRepository: TrackBrowserRepository,
    private val enqueueDownload: EnqueueDownloadUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(GenreTracksState())
    private val _events = Channel<GenreTracksEvent>(Channel.BUFFERED)
    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    private val genre: String = savedStateHandle["genre"]!!

    init { load() }

    fun onAction(action: GenreTracksAction) {
        when (action) {
            GenreTracksAction.NavigateBack -> Unit
            GenreTracksAction.Retry -> load()
            GenreTracksAction.PlayAll -> Unit
            is GenreTracksAction.PlayTrack -> Unit
            is GenreTracksAction.DownloadTrack -> downloadTrack(action.track)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, genre = genre)
            try {
                val tracks = trackBrowserRepository.findTracksByGenre(genre, 100)
                val items = tracks.map { track ->
                    GenreTrackItem(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        albumName = track.albumName,
                        durationMs = track.durationMs,
                        mediaId = track.mediaId,
                        canDownload = track.canDownload,
                    )
                }
                _state.value = _state.value.copy(isLoading = false, tracks = items.toPersistentList())
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load genre tracks")
            }
        }
    }

    private fun downloadTrack(track: GenreTrackItem) {
        val mediaId = track.mediaId ?: run {
            viewModelScope.launch { _events.send(GenreTracksEvent.ShowMessage("This track cannot be downloaded yet.")) }
            return
        }
        viewModelScope.launch {
            try {
                enqueueDownload(DownloadRequest(mediaId = mediaId, title = track.title, durationMs = track.durationMs))
                _events.send(GenreTracksEvent.ShowMessage("Added to Downloads."))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _events.send(GenreTracksEvent.ShowMessage(e.message?.takeIf { it.isNotBlank() } ?: "Failed to add download."))
            }
        }
    }
}
