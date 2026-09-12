package io.github.julystar.musicapp.car.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.TrackBrowserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CarHomeState(
    val recentlyAdded: RepositoryState<List<LibraryTrackItem>> = RepositoryState.Loading,
    val recentlyPlayed: RepositoryState<List<LibraryTrackItem>> = RepositoryState.Loading,
    val favorites: RepositoryState<List<LibraryTrackItem>> = RepositoryState.Loading,
)

class CarHomeViewModel(
    private val trackBrowserRepository: TrackBrowserRepository,
    favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CarHomeState())
    val state = _state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            favoritesRepository.favoriteTracks().collect { favorites ->
                _state.value = _state.value.copy(favorites = favorites)
            }
        }
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                recentlyAdded = RepositoryState.Loading,
                recentlyPlayed = RepositoryState.Loading,
            )
            val added = async {
                loadBrowserSection("最近添加载入失败") { trackBrowserRepository.findRecentlyAdded(HOME_TRACK_LIMIT) }
            }
            val played = async {
                loadBrowserSection("播放历史载入失败") { trackBrowserRepository.findRecentlyPlayed(HOME_TRACK_LIMIT) }
            }
            _state.value = _state.value.copy(
                recentlyAdded = added.await(),
                recentlyPlayed = played.await(),
            )
        }
    }

    private suspend fun loadBrowserSection(
        errorMessage: String,
        load: suspend () -> List<DomainTrackBrowserItem>,
    ): RepositoryState<List<LibraryTrackItem>> = try {
        load().map { track -> track.toLibraryTrackItem() }.let { tracks ->
            if (tracks.isEmpty()) RepositoryState.Empty() else RepositoryState.Loaded(tracks)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        RepositoryState.Error(error, errorMessage)
    }

    private fun DomainTrackBrowserItem.toLibraryTrackItem() = LibraryTrackItem(
        id = id,
        title = title,
        artist = artist,
        durationMs = durationMs,
        mediaId = mediaId,
        albumName = albumName,
        albumId = albumId,
        codec = codec,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
    )

    private companion object {
        const val HOME_TRACK_LIMIT = 12
    }
}
