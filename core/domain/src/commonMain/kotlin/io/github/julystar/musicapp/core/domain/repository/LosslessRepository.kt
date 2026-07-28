package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the Lossless section of the library.
 * Tracks where [io.github.julystar.musicapp.database.TrackEntity.lossless] is true
 * or the preferred [io.github.julystar.musicapp.database.TrackSourceRefEntity.lossless] is true.
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
