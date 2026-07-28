package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
import kotlinx.coroutines.flow.Flow

/**
 * Data contract for the Folder browser section of the library.
 * Represents user-selected import roots and their sub-directories.
 */
interface FolderRepository {

    /** Observable list of library roots (top-level import folders). */
    val libraryRoots: Flow<RepositoryState<List<LibraryFolderItem>>>

    /**
     * List child folders/files under a given [parentPath],
     * with optional sort and name filter.
     */
    fun browseFolder(
        parentPath: String,
        sort: SortCriteria = SortCriteria.Default,
        filter: FilterCriteria.FolderFilter = FilterCriteria.FolderFilter(),
    ): Flow<RepositoryState<List<LibraryFolderItem>>>
}

/**
 * Lightweight presentation model for a folder entry in the library.
 */
data class LibraryFolderItem(
    val path: String,
    val displayName: String,
    val isDirectory: Boolean,
    val trackCount: Int = 0,
    val fileSizeBytes: Long? = null,
    val lastModifiedMs: Long? = null,
)
