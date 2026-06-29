package com.github.tidetunes.feature.artist.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.DomainArtistDetail
import com.github.tidetunes.core.domain.repository.ArtistDetailRepository
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ArtistViewModel(
    private val artistDetailRepository: ArtistDetailRepository,
    private val enqueueDownload: EnqueueDownloadUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistState())
    private val _events = Channel<ArtistEvent>(Channel.BUFFERED)
    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    private val artistId: Long = savedStateHandle["id"]!!

    init {
        loadArtist()
    }

    fun onAction(action: ArtistAction) {
        when (action) {
            ArtistAction.NavigateBack -> Unit
            ArtistAction.Retry -> loadArtist()
            ArtistAction.PlayAll -> Unit
            is ArtistAction.PlayTrack -> Unit
            is ArtistAction.NavigateToAlbum -> Unit
            is ArtistAction.DownloadTrack -> downloadTrack(action.track)
        }
    }

    private fun loadArtist() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val detail: DomainArtistDetail = artistDetailRepository.loadArtistDetail(artistId)

                val albumItems: List<ArtistAlbumItem> = detail.albums.map { album ->
                    ArtistAlbumItem(
                        id = album.id,
                        name = album.name ?: "Unknown Album",
                        year = album.year,
                        artwork = album.firstTrackId?.let {
                            Artwork.LibraryTrack(trackId = it)
                        },
                    )
                }

                val trackItems: List<ArtistTrackItem> = detail.tracks.map { track ->
                    ArtistTrackItem(
                        id = track.id,
                        title = track.title,
                        albumName = track.albumName,
                        trackNumber = track.trackNumber,
                        discNumber = track.discNumber,
                        durationMs = track.durationMs,
                        mediaId = track.mediaId,
                        canDownload = track.canDownload,
                        albumId = track.albumId,
                    )
                }

                val artistArtwork = albumItems.firstOrNull()?.artwork

                _state.value = ArtistState(
                    isLoading = false,
                    artistId = artistId,
                    name = detail.name ?: "Unknown Artist",
                    artwork = artistArtwork,
                    albums = albumItems.toPersistentList(),
                    tracks = trackItems.toPersistentList(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load artist",
                )
            }
        }
    }

    private fun downloadTrack(track: ArtistTrackItem) {
        val mediaId = track.mediaId ?: run {
            viewModelScope.launch {
                _events.send(ArtistEvent.ShowMessage("This track cannot be downloaded yet."))
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
                _events.send(ArtistEvent.ShowMessage("Added to Downloads."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(
                    ArtistEvent.ShowMessage(e.message?.takeIf { it.isNotBlank() } ?: "Failed to add download.")
                )
            }
        }
    }
}
