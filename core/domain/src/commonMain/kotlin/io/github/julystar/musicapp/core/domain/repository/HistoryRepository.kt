package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.LibrarySortField
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
import io.github.julystar.musicapp.core.domain.model.SortDirection
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the play History section of the library.
 * Tracks backed by TrackEntity.lastPlayedAt.
 */
interface HistoryRepository {

    fun recentTracks(
        limit: Int = 200,
        sort: SortCriteria = SortCriteria(field = LibrarySortField.LastPlayed, direction = SortDirection.Descending),
        filter: FilterCriteria.HistoryFilter = FilterCriteria.HistoryFilter(),
    ): Flow<RepositoryState<List<LibraryTrackItem>>>

    val historyCount: Flow<Int>

    suspend fun clearHistory()
}
