package com.github.tidetunes.feature.album.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.DomainAlbumDetail
import com.github.tidetunes.core.domain.repository.AlbumDetailRepository
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AlbumViewModel(
    private val albumDetailRepository: AlbumDetailRepository,
    private val enqueueDownload: EnqueueDownloadUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumState())
    private val _events = Channel<AlbumEvent>(Channel.BUFFERED)
    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    private val albumId: Long = savedStateHandle["id"]!!

    init {
        loadAlbum()
    }

    fun onAction(action: AlbumAction) {
        when (action) {
            AlbumAction.NavigateBack -> Unit
            AlbumAction.Retry -> loadAlbum()
            AlbumAction.PlayAll -> Unit
            is AlbumAction.PlayTrack -> Unit
            is AlbumAction.DownloadTrack -> downloadTrack(action.track)
        }
    }

    private fun loadAlbum() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val detail: DomainAlbumDetail = albumDetailRepository.loadAlbumDetail(albumId)

                _state.value = AlbumState(
                    isLoading = false,
                    albumId = albumId,
                    title = detail.albumTitle,
                    artist = detail.albumArtist ?: "",
                    artwork = detail.tracks.firstOrNull()?.let {
                        Artwork.LibraryTrack(trackId = it.id)
                    },
                    tracks = detail.tracks.map { track ->
                        AlbumTrackItem(
                            id = track.id,
                            title = track.title,
                            trackNumber = track.trackNumber,
                            discNumber = track.discNumber,
                            durationMs = track.durationMs,
                            mediaId = track.mediaId,
                            canDownload = track.canDownload,
                        )
                    }.toPersistentList(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load album",
                )
            }
        }
    }

    private fun downloadTrack(track: AlbumTrackItem) {
        val mediaId = track.mediaId ?: run {
            viewModelScope.launch {
                _events.send(AlbumEvent.ShowMessage("This track cannot be downloaded yet."))
            }
            return
        }
        viewModelScope.launch {
            try {
                enqueueDownload(
                    DownloadRequest(
                        mediaId = mediaId,
                        title = track.title,
                        durationMs = track.durationMs,
                    )
                )
                _events.send(AlbumEvent.ShowMessage("Added to Downloads."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(
                    AlbumEvent.ShowMessage(e.message?.takeIf { it.isNotBlank() } ?: "Failed to add download.")
                )
            }
        }
    }
}
