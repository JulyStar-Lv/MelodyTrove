package io.github.julystar.musicapp.service.librarysync.domain

import io.github.julystar.musicapp.core.domain.model.DEFAULT_IGNORED_SOURCE_DIRECTORIES
import io.github.julystar.musicapp.core.domain.model.DEFAULT_MINIMUM_AUDIO_DURATION_MS
import io.github.julystar.musicapp.core.domain.model.MetadataScanMode
import io.github.julystar.musicapp.core.domain.model.MissingFilePolicy
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import kotlinx.coroutines.flow.Flow

interface LibrarySyncController {
    val recentTasks: Flow<List<LibrarySyncTask>>

    fun observeFailures(taskId: String): Flow<List<LibrarySyncFailure>>
    suspend fun syncFolder(request: LibrarySyncRequest): LibrarySyncResult
    suspend fun pause(scanId: String): Boolean
    suspend fun cancel(scanId: String): Boolean
    suspend fun cancelAll()
    suspend fun recoverInterruptedTasks(): Int
    suspend fun resume(scanId: String): LibrarySyncResult?
    suspend fun retry(scanId: String): LibrarySyncResult?
}

class LibrarySyncAlreadyActiveException(
    val accountId: SourceAccountId,
) : IllegalStateException("A library sync is already active for this source")

interface LibrarySyncTaskRepository {
    fun observeRecentTasks(limit: Int = DEFAULT_LIBRARY_SYNC_TASK_LIMIT): Flow<List<LibrarySyncTask>>
    fun observeActiveTasks(): Flow<List<LibrarySyncTask>>
    fun observeFailures(taskId: String): Flow<List<LibrarySyncFailure>>
    suspend fun getTask(id: String): LibrarySyncTask?
    suspend fun hasActiveTask(
        accountId: SourceAccountId,
        excludingTaskId: String? = null,
    ): Boolean
    suspend fun markPaused(id: String): Boolean
    suspend fun markCancelled(id: String): Boolean
}

data class LibrarySyncRequest(
    val accountId: SourceAccountId,
    val selectedFolderRemoteId: String?,
    val selectedFolderCanonicalPath: String,
    val selectedFolderDisplayPath: String? = null,
    val scanRules: LibrarySyncScanRules = LibrarySyncScanRules(),
    val metadataScanMode: MetadataScanMode = MetadataScanMode.Full,
    val scanId: String? = null,
    val metadataConcurrency: UInt = DEFAULT_LIBRARY_SYNC_METADATA_CONCURRENCY,
    val importBatchSize: Int = DEFAULT_LIBRARY_SYNC_BATCH_SIZE,
) {
    init {
        require(selectedFolderCanonicalPath.isNotBlank()) {
            "selected folder path cannot be blank"
        }
        require(scanId == null || scanId.isNotBlank()) {
            "scan id cannot be blank"
        }
        require(metadataConcurrency in 1u..MAX_LIBRARY_SYNC_METADATA_CONCURRENCY) {
            "metadata concurrency must be between 1 and $MAX_LIBRARY_SYNC_METADATA_CONCURRENCY"
        }
        require(importBatchSize in 1..MAX_LIBRARY_SYNC_IMPORT_BATCH_SIZE) {
            "import batch size must be between 1 and $MAX_LIBRARY_SYNC_IMPORT_BATCH_SIZE"
        }
    }
}

data class LibrarySyncScanRules(
    val scanSubdirectories: Boolean = true,
    val minDurationMs: Long = DEFAULT_MINIMUM_AUDIO_DURATION_MS,
    val missingFilePolicy: MissingFilePolicy = MissingFilePolicy.MarkUnavailable,
    val ignoreHiddenFiles: Boolean = true,
    val ignoredDirectoryNames: Set<String> = DEFAULT_IGNORED_SOURCE_DIRECTORIES.toSet(),
) {
    init {
        require(minDurationMs >= 0L) { "minimum duration cannot be negative" }
    }
}

data class LibrarySyncResult(
    val scanId: String,
    val selectedFolderId: Long,
    val scannedCount: Long,
    val changedCount: Long,
    val skippedCount: Long,
    val importedCount: Long,
    val failedCount: Long,
    val metadataRequestCount: Long = 0,
    val metadataFetchedBytes: Long = 0,
    val metadataElapsedMs: Long = 0,
    val artworkCachedBytes: Long = 0,
    val syncMode: String = "LEGACY_FULL_SCAN_FALLBACK",
    val directoryConcurrency: Int = 4,
    val capabilityDetectionElapsedMs: Long = 0,
    val directoryScanElapsedMs: Long = 0,
    val directoryRequestCount: Long = 0,
    val listedDirectoryCount: Long = 0,
    val visitedEntryCount: Long = 0,
    val discoveredMusicCount: Long = 0,
    val unchangedCount: Long = 0,
    val addedCount: Long = 0,
    val modifiedCount: Long = 0,
    val renamedCount: Long = 0,
    val deletedCount: Long = 0,
    val databaseReadElapsedMs: Long = 0,
    val databaseWriteElapsedMs: Long = 0,
    val totalElapsedMs: Long = 0,
)

data class LibrarySyncFailure(
    val errorType: String,
    val message: String,
    val createdAtEpochMs: Long,
)

const val DEFAULT_LIBRARY_SYNC_BATCH_SIZE = 200
const val DEFAULT_LIBRARY_SYNC_TASK_LIMIT = 5
const val MAX_LIBRARY_SYNC_IMPORT_BATCH_SIZE = 500
const val DEFAULT_LIBRARY_SYNC_METADATA_CONCURRENCY = 8u
const val MAX_LIBRARY_SYNC_METADATA_CONCURRENCY = 16u
