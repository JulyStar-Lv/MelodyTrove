package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.FilterCriteria
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.RepositoryState
import com.github.tidetunes.core.domain.model.SortCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the Lossless section of the library.
 * Tracks where [com.github.tidetunes.database.TrackEntity.lossless] is true
 * or the preferred [com.github.tidetunes.database.TrackSourceRefEntity.lossless] is true.
 */
interface LosslessRepository {

    fun losslessTracks(
        sort: SortCriteria = SortCriteria.Default,
        filter: FilterCriteria.LosslessFilter = FilterCriteria.LosslessFilter(),
    ): Flow<RepositoryState<List<LibraryTrackItem>>>

    val losslessCount: Flow<Int>

    /** Hi-Res subset: sample rate >= 96 kHz or bit depth >= 24. */
    fun hiResTracks(
        sort: SortCriteria = SortCriteria.Default,
        filter: FilterCriteria.LosslessFilter = FilterCriteria.LosslessFilter(),
    ): Flow<RepositoryState<List<LibraryTrackItem>>>

    val hiResCount: Flow<Int>
}
