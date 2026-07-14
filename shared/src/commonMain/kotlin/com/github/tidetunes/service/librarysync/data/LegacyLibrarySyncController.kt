package com.github.tidetunes.service.librarysync.data

import com.github.tidetunes.domain.importing.RemoteLibraryImportCoordinator
import com.github.tidetunes.domain.importing.RemoteLibraryImportResult
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure
import com.github.tidetunes.service.librarysync.domain.LibrarySyncRequest
import com.github.tidetunes.service.librarysync.domain.LibrarySyncResult
import com.github.tidetunes.service.librarysync.domain.LibrarySyncScanRules
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTaskRepository
import com.github.tidetunes.source.storage.toLegacyStorageIdOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uniffi.tidetunes_backend.Storage
import uniffi.tidetunes_backend.StorageId
import uniffi.tidetunes_backend.StorageType

internal class LegacyLibrarySyncController(
    private val importer: LegacyLibrarySyncImporter,
    private val storageProvider: LegacyLibrarySyncStorageProvider,
    private val taskRepository: LibrarySyncTaskRepository,
) : LibrarySyncController {
    private val startMutex = Mutex()

    override val recentTasks: Flow<List<LibrarySyncTask>> =
        taskRepository.observeRecentTasks()

    override fun observeFailures(taskId: String): Flow<List<LibrarySyncFailure>> {
        return taskRepository.observeFailures(taskId)
    }

    override suspend fun syncFolder(request: LibrarySyncRequest): LibrarySyncResult {
        return startSync(
            request = request,
            activeTaskExclusion = null,
        )
    }

    private suspend fun startSync(
        request: LibrarySyncRequest,
        activeTaskExclusion: String?,
    ): LibrarySyncResult {
        val result = startMutex.withLock {
            val storageId = request.accountId.toLegacyStorageIdOrNull()
                ?: error("Unsupported source account ${request.accountId.value}")
            val storage = storageProvider.storage(storageId)
                ?: error("Selected storage is no longer available")
            check(
                !taskRepository.hasActiveTask(
                    accountId = request.accountId,
                    excludingTaskId = activeTaskExclusion,
                )
            ) {
                "A library sync is already active for ${request.accountId.value}"
            }

            if (storage.typ == StorageType.ONE_DRIVE) {
                importer.syncOneDriveFolder(
                    storageId = storageId.value,
                    selectedFolderRemoteId = requireNotNull(request.selectedFolderRemoteId) {
                        "OneDrive folder has no DriveItem ID"
                    },
                    selectedFolderCanonicalPath = request.selectedFolderCanonicalPath,
                    selectedFolderDisplayPath = request.selectedFolderDisplayPath,
                    scanId = request.scanId,
                    scanRules = request.scanRules,
                    metadataScanMode = request.metadataScanMode,
                    metadataConcurrency = request.metadataConcurrency,
                    importBatchSize = request.importBatchSize,
                )
            } else {
                importer.scanAndImportFolder(
                    storageId = storageId.value,
                    selectedFolderRemoteId = request.selectedFolderRemoteId,
                    selectedFolderCanonicalPath = request.selectedFolderCanonicalPath,
                    selectedFolderDisplayPath = request.selectedFolderDisplayPath,
                    scanId = request.scanId,
                    scanRules = request.scanRules,
                    metadataScanMode = request.metadataScanMode,
                    metadataConcurrency = request.metadataConcurrency,
                    importBatchSize = request.importBatchSize,
                )
            }
        }

        return result.toLibrarySyncResult()
    }

    override suspend fun pause(scanId: String): Boolean {
        val activePaused = importer.pauseImport(scanId)
        if (!activePaused) return false
        val persistedPaused = taskRepository.markPaused(scanId)
        return activePaused || persistedPaused
    }

    override suspend fun cancel(scanId: String): Boolean {
        val activeCancelled = importer.cancelImport(scanId)
        val canCancelPersistedTask = activeCancelled ||
            taskRepository.getTask(scanId)?.canResume == true
        val persistedCancelled = if (canCancelPersistedTask) {
            taskRepository.markCancelled(scanId)
        } else {
            false
        }
        return activeCancelled || persistedCancelled
    }

    override suspend fun cancelAll() {
        taskRepository.observeActiveTasks().first().forEach { task ->
            cancel(task.id)
        }
    }

    override suspend fun resume(scanId: String): LibrarySyncResult? {
        val task = taskRepository.getTask(scanId) ?: return null
        if (!task.canResume) return null
        return startSync(
            request = task.toLibrarySyncRequest(scanId = scanId),
            activeTaskExclusion = scanId,
        )
    }

    override suspend fun retry(scanId: String): LibrarySyncResult? {
        val task = taskRepository.getTask(scanId) ?: return null
        if (!task.canRetry) return null
        return startSync(
            request = task.toLibrarySyncRequest(scanId = scanId),
            activeTaskExclusion = scanId,
        )
    }
}

internal interface LegacyLibrarySyncImporter {
    suspend fun cancelImport(scanId: String): Boolean
    suspend fun pauseImport(scanId: String): Boolean

    suspend fun syncOneDriveFolder(
        storageId: Long,
        selectedFolderRemoteId: String,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String?,
        scanId: String?,
        scanRules: LibrarySyncScanRules,
        metadataScanMode: MetadataScanMode,
        metadataConcurrency: UInt,
        importBatchSize: Int,
    ): RemoteLibraryImportResult

    suspend fun scanAndImportFolder(
        storageId: Long,
        selectedFolderRemoteId: String?,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String?,
        scanId: String?,
        scanRules: LibrarySyncScanRules,
        metadataScanMode: MetadataScanMode,
        metadataConcurrency: UInt,
        importBatchSize: Int,
    ): RemoteLibraryImportResult
}

internal fun interface LegacyLibrarySyncStorageProvider {
    fun storage(storageId: StorageId): Storage?
}

internal class RemoteLibraryImportGateway(
    private val coordinator: RemoteLibraryImportCoordinator,
) : LegacyLibrarySyncImporter {
    override suspend fun cancelImport(scanId: String): Boolean {
        return coordinator.cancelImport(scanId)
    }

    override suspend fun pauseImport(scanId: String): Boolean {
        return coordinator.pauseImport(scanId)
    }

    override suspend fun syncOneDriveFolder(
        storageId: Long,
        selectedFolderRemoteId: String,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String?,
        scanId: String?,
        scanRules: LibrarySyncScanRules,
        metadataScanMode: MetadataScanMode,
        metadataConcurrency: UInt,
        importBatchSize: Int,
    ): RemoteLibraryImportResult {
        return coordinator.syncOneDriveFolder(
            storageId = storageId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            scanId = scanId,
            scanRules = scanRules,
            metadataScanMode = metadataScanMode,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
    }

    override suspend fun scanAndImportFolder(
        storageId: Long,
        selectedFolderRemoteId: String?,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String?,
        scanId: String?,
        scanRules: LibrarySyncScanRules,
        metadataScanMode: MetadataScanMode,
        metadataConcurrency: UInt,
        importBatchSize: Int,
    ): RemoteLibraryImportResult {
        return coordinator.scanAndImportFolder(
            storageId = storageId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            scanId = scanId,
            scanRules = scanRules,
            metadataScanMode = metadataScanMode,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
    }
}

private fun RemoteLibraryImportResult.toLibrarySyncResult(): LibrarySyncResult {
    return LibrarySyncResult(
        scanId = scanId,
        selectedFolderId = selectedFolderId,
        scannedCount = scannedCount,
        changedCount = changedCount,
        skippedCount = skippedCount,
        importedCount = importedCount,
        failedCount = failedCount,
        metadataRequestCount = metadataRequestCount,
        metadataFetchedBytes = metadataFetchedBytes,
        metadataElapsedMs = metadataElapsedMs,
        artworkCachedBytes = artworkCachedBytes,
    )
}

private fun LibrarySyncTask.toLibrarySyncRequest(scanId: String): LibrarySyncRequest {
    return LibrarySyncRequest(
        accountId = accountId,
        selectedFolderRemoteId = selectedFolderRemoteId,
        selectedFolderCanonicalPath = folderPath,
        selectedFolderDisplayPath = folderDisplayPath,
        scanRules = scanRules,
        scanId = scanId,
        metadataScanMode = metadataScanMode,
        metadataConcurrency = metadataConcurrency,
        importBatchSize = importBatchSize,
    )
}
