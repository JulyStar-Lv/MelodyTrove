package com.github.tidetunes.service.librarysync.domain

import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.SourceAccountId

data class LibrarySyncTask(
    val id: String,
    val accountId: SourceAccountId,
    val selectedFolderId: Long,
    val selectedFolderRemoteId: String?,
    val folderPath: String,
    val folderDisplayPath: String,
    val status: LibrarySyncStatus,
    val scannedCount: Long,
    val importedCount: Long,
    val skippedCount: Long,
    val failedCount: Long,
    val checkpoint: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val metadataScanMode: MetadataScanMode = MetadataScanMode.Full,
    val metadataConcurrency: UInt = DEFAULT_LIBRARY_SYNC_METADATA_CONCURRENCY,
    val importBatchSize: Int = DEFAULT_LIBRARY_SYNC_BATCH_SIZE,
    val scanRules: LibrarySyncScanRules = LibrarySyncScanRules(),
    val metadataRequestCount: Long = 0,
    val metadataFetchedBytes: Long = 0,
    val metadataElapsedMs: Long = 0,
    val artworkCachedBytes: Long = 0,
) {
    init {
        require(id.isNotBlank()) { "Library sync task id cannot be blank" }
        require(selectedFolderId >= 0) { "Selected folder id cannot be negative" }
        require(folderPath.isNotBlank()) { "Library sync folder path cannot be blank" }
        require(folderDisplayPath.isNotBlank()) { "Library sync folder display path cannot be blank" }
        require(scannedCount >= 0) { "Scanned count cannot be negative" }
        require(importedCount >= 0) { "Imported count cannot be negative" }
        require(skippedCount >= 0) { "Skipped count cannot be negative" }
        require(failedCount >= 0) { "Failed count cannot be negative" }
    }

    val hasError: Boolean
        get() = failedCount > 0 || !errorMessage.isNullOrBlank()

    val processedCount: Long
        get() = importedCount + skippedCount + failedCount

    val pendingCount: Long
        get() = (scannedCount - processedCount).coerceAtLeast(0)

    val successfulCount: Long
        get() = importedCount + skippedCount

    val hasProgress: Boolean
        get() = scannedCount > 0L || processedCount > 0L

    val isActive: Boolean
        get() = status.isActive

    val canResume: Boolean
        get() = status == LibrarySyncStatus.Paused

    val canRetry: Boolean
        get() = status in setOf(
            LibrarySyncStatus.Failed,
            LibrarySyncStatus.Cancelled,
            LibrarySyncStatus.CompletedWithErrors,
        )
}

enum class LibrarySyncStatus {
    Queued,
    Running,
    Paused,
    Completed,
    CompletedWithErrors,
    Failed,
    Cancelled,
    Unknown,
}

val LibrarySyncStatus.isActive: Boolean
    get() = when (this) {
        LibrarySyncStatus.Queued,
        LibrarySyncStatus.Running,
        LibrarySyncStatus.Paused -> true
        LibrarySyncStatus.Completed,
        LibrarySyncStatus.CompletedWithErrors,
        LibrarySyncStatus.Failed,
        LibrarySyncStatus.Cancelled,
        LibrarySyncStatus.Unknown -> false
    }
