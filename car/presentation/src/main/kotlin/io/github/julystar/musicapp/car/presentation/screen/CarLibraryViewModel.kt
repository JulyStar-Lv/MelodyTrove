package io.github.julystar.musicapp.car.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import io.github.julystar.musicapp.core.domain.repository.PlaylistRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CarLibraryUiState(
    val initialLoadComplete: Boolean = false,
    val loadError: String? = null,
    val tracks: List<LibraryTrackItem> = emptyList(),
    val albums: List<LibraryAlbumItem> = emptyList(),
    val artists: List<LibraryArtistItem> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val currentTrackId: Long? = null,
)

private data class CarLibraryContent(
    val initialLoadComplete: Boolean,
    val loadError: String?,
    val tracks: List<LibraryTrackItem>,
    val albums: List<LibraryAlbumItem>,
    val artists: List<LibraryArtistItem>,
)

class CarLibraryViewModel(
    libraryRepository: LibraryRepository,
    playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {
    private val content = combine(
        libraryRepository.initialLoadComplete,
        libraryRepository.loadError,
        libraryRepository.tracks,
        libraryRepository.albums,
        libraryRepository.artists,
    ) { initialized, error, tracks, albums, artists ->
        CarLibraryContent(initialized, error, tracks, albums, artists)
    }

    val state: StateFlow<CarLibraryUiState> = combine(
        content,
        playlistRepository.playlistSummaries,
        playbackController.state,
    ) { content, playlists, player ->
        CarLibraryUiState(
            initialLoadComplete = content.initialLoadComplete,
            loadError = content.loadError,
            tracks = content.tracks,
            albums = content.albums,
            artists = content.artists,
            playlists = playlists,
            currentTrackId = player.currentItem?.libraryTrackId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CarLibraryUiState(
            initialLoadComplete = libraryRepository.initialLoadComplete.value,
            loadError = libraryRepository.loadError.value,
            tracks = libraryRepository.tracks.value,
            albums = libraryRepository.albums.value,
            artists = libraryRepository.artists.value,
            playlists = playlistRepository.playlistSummaries.value,
            currentTrackId = playbackController.state.value.currentItem?.libraryTrackId,
        ),
    )

    fun play(request: CarPlaybackRequest) {
        viewModelScope.launch {
            playbackController.play(request.items, request.startIndex)
        }
    }
}
