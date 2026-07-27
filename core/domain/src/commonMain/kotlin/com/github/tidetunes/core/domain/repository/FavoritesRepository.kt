package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.FilterCriteria
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.RepositoryState
import com.github.tidetunes.core.domain.model.SortCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the Favorites section of the library.
 * "Favorites" means tracks explicitly marked by the user as favorites.
 */
interface FavoritesRepository {

    val favoriteTrackIds: Flow<Set<Long>>

    fun favoriteTracks(
        sort: SortCriteria = SortCriteria.Default,
        filter: FilterCriteria.FavoritesFilter = FilterCriteria.FavoritesFilter(),
    ): Flow<RepositoryState<List<LibraryTrackItem>>>

    suspend fun isFavorite(trackId: Long): Boolean

    suspend fun toggleFavorite(trackId: Long): Boolean

    val favoriteCount: Flow<Int>
}
