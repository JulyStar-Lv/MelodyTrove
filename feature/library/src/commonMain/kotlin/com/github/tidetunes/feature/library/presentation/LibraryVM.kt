package com.github.tidetunes.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.core.domain.repository.PlaylistRepository
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryVM(
    libraryRepository: LibraryRepository,
    playlistRepository: PlaylistRepository,
    private val enqueueDownload: EnqueueDownloadUseCase,
) : ViewModel() {
    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)

    val events = _events.receiveAsFlow()
    val state = combine(
        libraryRepository.tracks,
        libraryRepository.albums,
        libraryRepository.artists,
        playlistRepository.playlistSummaries,
    ) { tracks, albums, artists, playlists ->
            LibraryState(
                tracks = tracks.toPersistentList(),
                albums = albums.toPersistentList(),
                artists = artists.toPersistentList(),
                playlists = playlists.toPersistentList(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryState(),
        )

    fun onAction(action: LibraryAction) {
        when (action) {
            LibraryAction.Refresh -> Unit
            is LibraryAction.PlayTrack -> Unit
            is LibraryAction.DownloadTrack -> downloadTrack(action.track)
        }
    }

    private fun downloadTrack(track: LibraryTrackItem) {
        val mediaId = track.mediaId
        if (mediaId == null) {
            viewModelScope.launch {
                _events.send(LibraryEvent.ShowMessage("This track cannot be downloaded yet."))
            }
            return
        }
        viewModelScope.launch {
            try {
                enqueueDownload(
                    DownloadRequest(
                        mediaId = mediaId,
                        title = track.title,
                        artist = track.artist,
                        durationMs = track.durationMs,
                    )
                )
                _events.send(LibraryEvent.ShowMessage("Added to Downloads."))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _events.send(
                    LibraryEvent.ShowMessage(
                        exception.message?.takeIf { it.isNotBlank() } ?: "Failed to add download.",
                    )
                )
            }
        }
    }
}
