package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the Genre browsing section of the library.
 * Backed by the Room `genre` + `track_genre` + `track` tables.
 */
interface GenreRepository {

    /** Load genre name list. Returns [RepositoryState.Loading] until first emission. */
    val genreNames: Flow<RepositoryState<List<String>>>

    /**
     * Load tracks belonging to a specific genre, respecting sort & filter.
     * The returned [Flow] re-emits whenever the underlying track data changes.
     */
    fun genreTracks(
        genreName: String,
        sort: SortCriteria = SortCriteria.Default,
        filter: FilterCriteria.GenreFilter = FilterCriteria.GenreFilter(),
    ): Flow<RepositoryState<List<DomainTrackBrowserItem>>>
}
