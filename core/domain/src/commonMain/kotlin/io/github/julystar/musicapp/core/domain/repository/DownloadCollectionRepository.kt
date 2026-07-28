package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the Downloads section of the library.
 * Tracks where [io.github.julystar.musicapp.database.TrackSourceRefEntity.isDownloaded] is true.
 */
interface DownloadCollectionRepository {

    fun downloadedTracks(
        sort: SortCriteria = SortCriteria.Default,
        filter: FilterCriteria.DownloadsFilter = FilterCriteria.DownloadsFilter(),
    ): Flow<RepositoryState<List<LibraryTrackItem>>>

    val downloadCount: Flow<Int>

    /** Total bytes used by downloaded tracks (to show storage footprint). */
    val totalDownloadedBytes: Flow<Long>

    /** Remove a downloaded track's local file without deleting the track metadata. */
    suspend fun removeDownload(trackId: Long)
}
