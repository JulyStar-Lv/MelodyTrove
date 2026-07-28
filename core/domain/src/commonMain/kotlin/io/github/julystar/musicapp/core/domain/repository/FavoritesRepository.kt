package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
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
