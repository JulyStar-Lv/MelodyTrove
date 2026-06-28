package com.github.tidetunes.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.feature.search.domain.SearchHistoryRepository
import com.github.tidetunes.feature.search.domain.SearchLibraryUseCase
import com.github.tidetunes.feature.search.domain.SearchSourceAccountProvider
import com.github.tidetunes.feature.search.domain.SearchSuggestionsUseCase
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import com.github.tidetunes.feature.search.domain.mergeSearchSuggestions
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchLibrary: SearchLibraryUseCase,
    private val searchSuggestions: SearchSuggestionsUseCase,
    private val sourceAccountProvider: SearchSourceAccountProvider,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val enqueueDownload: EnqueueDownloadUseCase,
    private val debounceMillis: Long = SEARCH_DEBOUNCE_MS,
    private val coroutineScopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private val coroutineScope: CoroutineScope
        get() = coroutineScopeOverride ?: viewModelScope

    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    init {
        coroutineScope.launch {
            searchHistoryRepository.history.collect { history ->
                _state.update { current ->
                    current.copy(
                        history = history.toPersistentList(),
                    )
                }
                refreshSuggestions(_state.value.query)
            }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> updateQuery(action.query)
            SearchAction.SubmitSearch -> submitSearch()
            SearchAction.Retry -> submitSearch()
            SearchAction.ClearQuery -> clearQuery()
            SearchAction.ClearHistory -> clearHistory()
            is SearchAction.SelectSuggestion -> selectSuggestion(action.query)
            is SearchAction.OpenTrack -> openTrack(action.track)
            is SearchAction.DownloadTrack -> downloadTrack(action.track)
        }
    }

    private fun updateQuery(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        _state.update { current ->
            current.copy(
                query = query,
                loadState = if (trimmed.isBlank()) SearchLoadState.Idle else SearchLoadState.Typing,
                tracks = if (trimmed.isBlank()) emptyList<SearchTrackItem>().toPersistentList() else current.tracks,
                suggestions = mergeSearchSuggestions(
                    query = trimmed,
                    history = current.history,
                    localSuggestions = emptyList(),
                ).toPersistentList(),
                failedSourceCount = if (trimmed.isBlank()) 0 else current.failedSourceCount,
            )
        }
        refreshSuggestions(trimmed)
        if (trimmed.isBlank()) return
        searchJob = coroutineScope.launch {
            delay(debounceMillis.coerceAtLeast(0))
            runSearch(trimmed)
        }
    }

    private fun submitSearch() {
        val query = _state.value.query.trim()
        searchJob?.cancel()
        if (query.isBlank()) {
            clearQuery()
            return
        }
        searchJob = coroutineScope.launch {
            runSearch(query)
        }
    }

    private fun clearQuery() {
        searchJob?.cancel()
        suggestionJob?.cancel()
        _state.update { current ->
            current.copy(
                query = "",
                loadState = SearchLoadState.Idle,
                tracks = emptyList<SearchTrackItem>().toPersistentList(),
                suggestions = current.history,
                failedSourceCount = 0,
            )
        }
        refreshSuggestions("")
    }

    private fun clearHistory() {
        coroutineScope.launch {
            searchHistoryRepository.clear()
        }
        val query = _state.value.query
        _state.update { current ->
            current.copy(
                history = emptyList<String>().toPersistentList(),
                suggestions = emptyList<String>().toPersistentList(),
            )
        }
        refreshSuggestions(query)
    }

    private fun selectSuggestion(query: String) {
        _state.update { current ->
            current.copy(query = query)
        }
        submitSearch()
    }

    private fun openTrack(track: SearchTrackItem) {
        val mediaId = track.mediaId
        if (mediaId == null) {
            coroutineScope.launch {
                _events.send(SearchEvent.ShowMessage("Local library playback is not wired to Search yet."))
            }
            return
        }
        coroutineScope.launch {
            _events.send(SearchEvent.OpenTrack(mediaId))
        }
    }

    private fun downloadTrack(track: SearchTrackItem) {
        val mediaId = track.mediaId
        if (mediaId == null) {
            coroutineScope.launch {
                _events.send(SearchEvent.ShowMessage("This track cannot be downloaded yet."))
            }
            return
        }
        coroutineScope.launch {
            try {
                enqueueDownload(
                    DownloadRequest(
                        mediaId = mediaId,
                        title = track.title,
                        artist = track.artist,
                        durationMs = track.durationMs,
                    )
                )
                _events.send(SearchEvent.ShowMessage("Added to Downloads."))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _events.send(
                    SearchEvent.ShowMessage(
                        exception.message?.takeIf { it.isNotBlank() } ?: "Failed to add download.",
                    )
                )
            }
        }
    }

    private suspend fun runSearch(query: String) {
        _state.update { current ->
            current.copy(
                query = query,
                loadState = SearchLoadState.Searching,
            )
        }
        val results = try {
            searchLibrary(
                query = query,
                sourceAccounts = sourceAccountProvider.sourceAccounts(),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            searchHistoryRepository.remember(query)
            _state.update { current ->
                current.copy(
                    loadState = SearchLoadState.Error,
                    tracks = emptyList<SearchTrackItem>().toPersistentList(),
                    failedSourceCount = 1,
                )
            }
            return
        }

        searchHistoryRepository.remember(query)
        _state.update { current ->
            current.copy(
                loadState = when {
                    results.tracks.isNotEmpty() -> SearchLoadState.Results
                    results.failedSources.isNotEmpty() -> SearchLoadState.Error
                    else -> SearchLoadState.Empty
                },
                tracks = results.tracks.toPersistentList(),
                failedSourceCount = results.failedSources.size,
            )
        }
    }

    private fun refreshSuggestions(query: String) {
        suggestionJob?.cancel()
        val normalizedQuery = query.trim()
        val history = _state.value.history.toList()
        suggestionJob = coroutineScope.launch {
            val suggestions = try {
                searchSuggestions(
                    query = normalizedQuery,
                    history = history,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                mergeSearchSuggestions(
                    query = normalizedQuery,
                    history = history,
                    localSuggestions = emptyList(),
                )
            }
            _state.update { current ->
                if (current.query.trim() == normalizedQuery) {
                    current.copy(suggestions = suggestions.toPersistentList())
                } else {
                    current
                }
            }
        }
    }
}

const val SEARCH_DEBOUNCE_MS = 300L
