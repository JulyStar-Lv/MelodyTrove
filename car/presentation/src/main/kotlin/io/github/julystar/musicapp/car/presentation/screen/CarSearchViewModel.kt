package io.github.julystar.musicapp.car.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.search.SearchRepository
import io.github.julystar.musicapp.core.domain.search.SearchTrackItem
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CarSearchUiState(
    val query: String = "",
    val results: List<SearchTrackItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val favoriteTrackIds: Set<Long> = emptySet(),
    val currentTrackId: Long? = null,
)

sealed interface CarSearchAction {
    data class QueryChanged(val query: String) : CarSearchAction
    data class ToggleFavorite(val trackId: Long) : CarSearchAction
    data class PlayResult(val index: Int) : CarSearchAction
    data object Retry : CarSearchAction
}

class CarSearchViewModel(
    private val repository: SearchRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CarSearchUiState())
    private var searchJob: Job? = null

    val state: StateFlow<CarSearchUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favoriteTrackIds.collect { ids ->
                mutableState.value = mutableState.value.copy(favoriteTrackIds = ids)
            }
        }
        viewModelScope.launch {
            playbackController.state.collect { playerState ->
                mutableState.value = mutableState.value.copy(
                    currentTrackId = playerState.currentItem?.libraryTrackId,
                )
            }
        }
    }

    fun onAction(action: CarSearchAction) {
        when (action) {
            is CarSearchAction.QueryChanged -> updateQuery(action.query)
            is CarSearchAction.ToggleFavorite -> viewModelScope.launch {
                favoritesRepository.toggleFavorite(action.trackId)
            }
            is CarSearchAction.PlayResult -> playResult(action.index)
            CarSearchAction.Retry -> search(mutableState.value.query, debounce = false)
        }
    }

    private fun updateQuery(query: String) {
        mutableState.value = mutableState.value.copy(query = query)
        search(query, debounce = true)
    }

    private fun search(query: String, debounce: Boolean) {
        searchJob?.cancel()
        val normalized = query.trim()
        if (normalized.isBlank()) {
            mutableState.value = mutableState.value.copy(
                results = emptyList(),
                loading = false,
                error = null,
            )
            return
        }
        searchJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            if (debounce) delay(SEARCH_DEBOUNCE_MILLIS)
            try {
                val results = repository.searchLocalLibrary(normalized).tracks
                if (mutableState.value.query.trim() == normalized) {
                    mutableState.value = mutableState.value.copy(results = results, loading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = mutableState.value.copy(
                    results = emptyList(),
                    loading = false,
                    error = failure.message ?: "搜索失败",
                )
            }
        }
    }

    private fun playResult(index: Int) {
        val request = mutableState.value.results.toSearchPlaybackRequest(index) ?: return
        viewModelScope.launch { playbackController.play(request.items, request.startIndex) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
