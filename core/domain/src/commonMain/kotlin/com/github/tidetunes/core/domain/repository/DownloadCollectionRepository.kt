package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.FilterCriteria
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.RepositoryState
import com.github.tidetunes.core.domain.model.SortCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the Downloads section of the library.
 * Tracks where [com.github.tidetunes.database.TrackSourceRefEntity.isDownloaded] is true.
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
