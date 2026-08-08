package io.github.julystar.musicapp.service.librarysync.data

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.MetadataScanMode
import io.github.julystar.musicapp.core.domain.model.MissingFilePolicy
import io.github.julystar.musicapp.database.ImportJobWithFolder
import io.github.julystar.musicapp.database.SourceErrorEntity
import io.github.julystar.musicapp.database.SourceErrorDao
import io.github.julystar.musicapp.database.SyncDao
import io.github.julystar.musicapp.platform.currentTimeMillis
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncFailure
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncScanRules
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncStatus
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncTask
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncTaskRepository
import io.github.julystar.musicapp.service.librarysync.domain.MAX_LIBRARY_SYNC_IMPORT_BATCH_SIZE
import io.github.julystar.musicapp.service.librarysync.domain.MAX_LIBRARY_SYNC_METADATA_CONCURRENCY
import io.github.julystar.musicapp.source.storage.toLegacyStorageIdOrNull
import io.github.julystar.musicapp.source.storage.toLegacyStorageSourceAccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uniffi.app_backend.StorageId

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
        syncMode = job.syncMode,
        directoryConcurrency = job.directoryConcurrency,
        capabilityDetectionElapsedMs = job.capabilityDetectionElapsedMs,
        directoryScanElapsedMs = job.directoryScanElapsedMs,
        directoryRequestCount = job.directoryRequestCount,
        listedDirectoryCount = job.listedDirectoryCount,
        visitedEntryCount = job.visitedEntryCount,
        discoveredMusicCount = job.discoveredMusicCount,
        unchangedCount = job.unchangedCount,
        addedCount = job.addedCount,
        modifiedCount = job.modifiedCount,
        renamedCount = job.renamedCount,
        deletedCount = job.deletedCount,
        databaseReadElapsedMs = job.databaseReadElapsedMs,
        databaseWriteElapsedMs = job.databaseWriteElapsedMs,
        totalElapsedMs = job.totalElapsedMs,
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
