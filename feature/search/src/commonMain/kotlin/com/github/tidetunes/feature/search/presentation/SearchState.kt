package com.github.tidetunes.feature.search.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.feature.search.domain.SearchAlbumItem
import com.github.tidetunes.feature.search.domain.SearchArtistItem
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SearchState(
    val query: String = "",
    val loadState: SearchLoadState = SearchLoadState.Idle,
    val tracks: ImmutableList<SearchTrackItem> = persistentListOf(),
    val albums: ImmutableList<SearchAlbumItem> = persistentListOf(),
    val artists: ImmutableList<SearchArtistItem> = persistentListOf(),
    val history: ImmutableList<String> = persistentListOf(),
    val suggestions: ImmutableList<String> = persistentListOf(),
    val failedSourceCount: Int = 0,
) {
    val isSearching: Boolean
        get() = loadState == SearchLoadState.Searching
}

enum class SearchLoadState {
    Idle,
    Typing,
    Searching,
    Results,
    Empty,
    Error,
}

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data object SubmitSearch : SearchAction
    data object Retry : SearchAction
    data object ClearQuery : SearchAction
    data object ClearHistory : SearchAction
    data class SelectSuggestion(val query: String) : SearchAction
    data class OpenTrack(val track: SearchTrackItem) : SearchAction
    data class OpenAlbum(val album: SearchAlbumItem) : SearchAction
    data class OpenArtist(val artist: SearchArtistItem) : SearchAction
    data class DownloadTrack(val track: SearchTrackItem) : SearchAction
}

sealed interface SearchEvent {
    data class OpenTrack(val track: SearchTrackItem) : SearchEvent
    data class NavigateToAlbum(val albumId: Long) : SearchEvent
    data class NavigateToArtist(val artistId: Long) : SearchEvent
    data class ShowMessage(val message: String) : SearchEvent
}
