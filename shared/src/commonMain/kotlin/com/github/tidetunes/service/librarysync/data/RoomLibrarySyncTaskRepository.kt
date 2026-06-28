package com.github.tidetunes.service.librarysync.data

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.database.ImportJobWithFolder
import com.github.tidetunes.database.SyncDao
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTaskRepository
import com.github.tidetunes.source.storage.toLegacyStorageIdOrNull
import com.github.tidetunes.source.storage.toLegacyStorageSourceAccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uniffi.tidetunes_core.StorageId

internal class RoomLibrarySyncTaskRepository(
    private val syncDao: SyncDao,
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

    override suspend fun getTask(id: String): LibrarySyncTask? {
        return syncDao.getJobWithFolder(id)?.toLibrarySyncTask()
    }

    override suspend fun hasActiveTask(
        accountId: SourceAccountId,
        excludingTaskId: String?,
    ): Boolean {
        val storageId = accountId.toLegacyStorageIdOrNull() ?: return false
        return syncDao.activeJobCountForStorage(
            storageId = storageId.value,
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

internal fun ImportJobWithFolder.toLibrarySyncTask(): LibrarySyncTask {
    return LibrarySyncTask(
        id = job.id,
        accountId = StorageId(folderStorageId).toLegacyStorageSourceAccountId(),
        selectedFolderId = job.selectedFolderId,
        selectedFolderRemoteId = folderRemoteId,
        folderPath = folderCanonicalPath,
        folderDisplayPath = folderDisplayPath,
        status = job.status.toLibrarySyncStatus(),
        scannedCount = job.scannedCount,
        importedCount = job.importedCount,
        skippedCount = job.skippedCount,
        failedCount = job.failedCount,
        checkpoint = job.checkpoint?.takeIf { it.isNotBlank() },
        errorMessage = job.errorMessage?.takeIf { it.isNotBlank() },
        createdAtEpochMs = job.createdAt,
        updatedAtEpochMs = job.updatedAt,
    )
}

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
