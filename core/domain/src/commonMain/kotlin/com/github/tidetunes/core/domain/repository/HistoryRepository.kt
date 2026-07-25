package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.FilterCriteria
import com.github.tidetunes.core.domain.model.LibrarySortField
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.RepositoryState
import com.github.tidetunes.core.domain.model.SortCriteria
import com.github.tidetunes.core.domain.model.SortDirection
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
