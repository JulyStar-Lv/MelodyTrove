package com.github.tidetunes.feature.artist.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.AlbumEntity
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.storage.legacyStorageTrackMediaIdOrNull
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ArtistViewModel(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
    private val storageLookup: LegacyStorageLookup,
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
                val artist = metadataDao.getArtist(artistId)
                val albums = metadataDao.albumsByArtistId(artistId)
                val tracks = trackDao.findTracksByArtistId(artistId)

                val albumItems: List<ArtistAlbumItem> = albums.map { album: AlbumEntity ->
                    val albumTracks = tracks.filter { it.albumId == album.id }
                    ArtistAlbumItem(
                        id = album.id,
                        name = album.name,
                        year = album.year,
                        artwork = album.artworkId?.let {
                            Artwork.LibraryTrack(
                                trackId = albumTracks.firstOrNull()?.id
                                    ?: return@let null
                            )
                        },
                    )
                }

                val trackItems: List<ArtistTrackItem> = tracks.map { track: TrackEntity ->
                    val albumName = albums.find { it.id == track.albumId }?.name
                    ArtistTrackItem(
                        id = track.id,
                        title = track.title,
                        albumName = albumName,
                        trackNumber = track.trackNumber,
                        discNumber = track.discNumber,
                        durationMs = track.durationMs,
                        mediaId = legacyStorageTrackMediaIdOrNull(
                            storageLookup = storageLookup,
                            sourceStorageId = track.sourceStorageId,
                            sourcePath = track.sourcePath,
                        ),
                        canDownload = track.sourceStorageId != null && track.sourcePath != null,
                        albumId = track.albumId,
                    )
                }

                val artistArtwork = albumItems.firstOrNull()?.artwork

                _state.value = ArtistState(
                    isLoading = false,
                    artistId = artistId,
                    name = artist?.name ?: "Unknown Artist",
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
