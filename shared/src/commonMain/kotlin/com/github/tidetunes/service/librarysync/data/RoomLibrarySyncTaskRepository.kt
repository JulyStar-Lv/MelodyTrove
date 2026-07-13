package com.github.tidetunes.service.librarysync.data

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.database.ImportJobWithFolder
import com.github.tidetunes.database.SourceErrorEntity
import com.github.tidetunes.database.SourceErrorDao
import com.github.tidetunes.database.SyncDao
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure
import com.github.tidetunes.service.librarysync.domain.LibrarySyncScanRules
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTaskRepository
import com.github.tidetunes.service.librarysync.domain.MAX_LIBRARY_SYNC_IMPORT_BATCH_SIZE
import com.github.tidetunes.service.librarysync.domain.MAX_LIBRARY_SYNC_METADATA_CONCURRENCY
import com.github.tidetunes.source.storage.toLegacyStorageIdOrNull
import com.github.tidetunes.source.storage.toLegacyStorageSourceAccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uniffi.tidetunes_backend.StorageId

internal class RoomLibrarySyncTaskRepository(
    private val syncDao: SyncDao,
    private val sourceErrorDao: SourceErrorDao,
) : LibrarySyncTaskRepository {
    override fun observeRecentTasks(limit: Int): Flow<List<LibrarySyncTask>> {
        return syncDao
            .observeRecentJobsWithFolder(limit.coerceAtLeast(1))
            .map { jobs -> jobs.map { job -> job.toLibrarySyncTask() } }
    }

    override fun observeActiveTasks(): Flow<List<LibrarySyncTask>> {
        return syncDao
            .observeActiveJobsWithFolder()
            .map { jobs -> jobs.map { job -> job.toLibrarySyncTask() } }
    }

    override fun observeFailures(taskId: String): Flow<List<LibrarySyncFailure>> {
        return sourceErrorDao
            .observeByImportJob(taskId)
            .map { errors -> errors.map { error -> error.toLibrarySyncFailure() } }
    }

    override suspend fun getTask(id: String): LibrarySyncTask? {
        return syncDao.getJobWithFolder(id)?.toLibrarySyncTask()
    }

    override suspend fun hasActiveTask(
        accountId: SourceAccountId,
        excludingTaskId: String?,
    ): Boolean {
        val storageId = accountId.toLegacyStorageIdOrNull() ?: return false
        return syncDao.activeJobCountForStorage(
            sourceAccountId = storageId.value,
            excludedJobId = excludingTaskId.orEmpty(),
        ) > 0
    }

    override suspend fun markPaused(id: String): Boolean {
        val now = currentTimeMillis()
        val changedRows = syncDao.markJobPaused(id, now)
        if (changedRows > 0) {
            syncDao.markSelectedFolderPausedForJob(id, now)
        }
        return changedRows > 0
    }

    override suspend fun markCancelled(id: String): Boolean {
        val now = currentTimeMillis()
        val changedRows = syncDao.markJobCancelled(id, now)
        if (changedRows > 0) {
            syncDao.markSelectedFolderCancelledForJob(id, now)
        }
        return changedRows > 0
    }
}

private fun SourceErrorEntity.toLibrarySyncFailure(): LibrarySyncFailure {
    return LibrarySyncFailure(
        errorType = errorType,
        message = message,
        createdAtEpochMs = createdAt,
    )
}

internal fun ImportJobWithFolder.toLibrarySyncTask(): LibrarySyncTask {
    return LibrarySyncTask(
        id = job.id,
        accountId = StorageId(sourceAccountId).toLegacyStorageSourceAccountId(),
        selectedFolderId = job.libraryRootId,
        selectedFolderRemoteId = providerRootId,
        folderPath = canonicalPath.orEmpty(),
        folderDisplayPath = displayName,
        status = job.status.toLibrarySyncStatus(),
        scannedCount = job.scannedCount,
        importedCount = job.importedCount,
        skippedCount = job.skippedCount,
        failedCount = job.failedCount,
        checkpoint = job.checkpoint?.takeIf { it.isNotBlank() },
        errorMessage = job.errorMessage?.takeIf { it.isNotBlank() },
        createdAtEpochMs = job.createdAt,
        updatedAtEpochMs = job.updatedAt,
        metadataScanMode = job.metadataScanMode.enumOrDefault(MetadataScanMode.Full),
        metadataConcurrency = job.metadataConcurrency
            .coerceIn(1, MAX_LIBRARY_SYNC_METADATA_CONCURRENCY.toLong())
            .toUInt(),
        importBatchSize = job.importBatchSize.coerceIn(1, MAX_LIBRARY_SYNC_IMPORT_BATCH_SIZE),
        scanRules = LibrarySyncScanRules(
            scanSubdirectories = job.scanSubdirectories,
            minDurationMs = if (job.ignoreShortAudio) job.minDurationMs else 0,
            missingFilePolicy = job.missingFilePolicy.enumOrDefault(MissingFilePolicy.MarkUnavailable),
            duplicateTrackPolicy = job.duplicateTrackPolicy
                .enumOrDefault(DuplicateTrackPolicy.SeparateBySource),
            ignoreHiddenFiles = job.ignoreHiddenFiles,
            ignoredDirectoryNames = job.ignoredDirectoryNames
                .split(SNAPSHOT_LIST_SEPARATOR)
                .filter(String::isNotBlank)
                .toSet(),
        ),
        metadataRequestCount = job.metadataRequestCount,
        metadataFetchedBytes = job.metadataFetchedBytes,
        metadataElapsedMs = job.metadataElapsedMs,
        artworkCachedBytes = job.artworkCachedBytes,
    )
}

private inline fun <reified T : Enum<T>> String.enumOrDefault(default: T): T {
    return enumValues<T>().firstOrNull { it.name == this } ?: default
}

private const val SNAPSHOT_LIST_SEPARATOR = "|"

internal fun String.toLibrarySyncStatus(): LibrarySyncStatus {
    return when (this) {
        "QUEUED" -> LibrarySyncStatus.Queued
        "RUNNING" -> LibrarySyncStatus.Running
        "PAUSED" -> LibrarySyncStatus.Paused
        "COMPLETED" -> LibrarySyncStatus.Completed
        "COMPLETED_WITH_ERRORS" -> LibrarySyncStatus.CompletedWithErrors
        "FAILED" -> LibrarySyncStatus.Failed
        "CANCELLED" -> LibrarySyncStatus.Cancelled
        else -> LibrarySyncStatus.Unknown
    }
}
