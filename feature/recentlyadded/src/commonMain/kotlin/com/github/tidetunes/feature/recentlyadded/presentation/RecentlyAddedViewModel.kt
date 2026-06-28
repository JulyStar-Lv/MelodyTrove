package com.github.tidetunes.feature.recentlyadded.presentation

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

class RecentlyAddedViewModel(
    private val trackBrowserRepository: TrackBrowserRepository,
    private val enqueueDownload: EnqueueDownloadUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RecentlyAddedState())
    private val _events = Channel<RecentlyAddedEvent>(Channel.BUFFERED)
    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: RecentlyAddedAction) {
        when (action) {
            RecentlyAddedAction.NavigateBack -> Unit
            RecentlyAddedAction.Retry -> load()
            RecentlyAddedAction.PlayAll -> Unit
            is RecentlyAddedAction.PlayTrack -> Unit
            is RecentlyAddedAction.DownloadTrack -> downloadTrack(action.track)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tracks = trackBrowserRepository.findRecentlyAdded(100)
                val trackItems = tracks.map { track ->
                    RecentlyAddedTrackItem(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        albumName = track.albumName,
                        durationMs = track.durationMs,
                        mediaId = track.mediaId,
                        canDownload = track.canDownload,
                    )
                }
                _state.value = RecentlyAddedState(
                    isLoading = false,
                    tracks = trackItems.toPersistentList(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load recently added",
                )
            }
        }
    }

    private fun downloadTrack(track: RecentlyAddedTrackItem) {
        val mediaId = track.mediaId ?: run {
            viewModelScope.launch {
                _events.send(RecentlyAddedEvent.ShowMessage("This track cannot be downloaded yet."))
            }
            return
        }
        viewModelScope.launch {
            try {
                enqueueDownload(DownloadRequest(mediaId = mediaId, title = track.title, durationMs = track.durationMs))
                _events.send(RecentlyAddedEvent.ShowMessage("Added to Downloads."))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _events.send(RecentlyAddedEvent.ShowMessage(e.message?.takeIf { it.isNotBlank() } ?: "Failed to add download."))
            }
        }
    }
}
