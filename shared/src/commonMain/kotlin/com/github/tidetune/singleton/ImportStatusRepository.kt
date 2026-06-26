package com.github.tidetune.singleton

import com.github.tidetune.database.ImportJobEntity
import com.github.tidetune.database.SyncDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportStatusItem(
    val id: String,
    val status: String,
    val scannedCount: Long,
    val importedCount: Long,
    val skippedCount: Long,
    val failedCount: Long,
    val checkpoint: String?,
    val errorMessage: String?,
    val updatedAt: Long,
) {
    val hasError: Boolean
        get() = failedCount > 0 || !errorMessage.isNullOrBlank()

    val isActive: Boolean
        get() = status == "QUEUED" || status == "RUNNING" || status == "PAUSED"
}

class ImportStatusRepository(
    scope: CoroutineScope,
    syncDao: SyncDao,
) {
    private val _recentJobs = MutableStateFlow<List<ImportStatusItem>>(emptyList())

    val recentJobs = _recentJobs.asStateFlow()

    init {
        scope.launch {
            syncDao.observeRecentJobs(limit = 5).collect { jobs ->
                _recentJobs.value = jobs.map { it.toImportStatusItem() }
            }
        }
    }
}

internal fun ImportJobEntity.toImportStatusItem(): ImportStatusItem {
    return ImportStatusItem(
        id = id,
        status = status,
        scannedCount = scannedCount,
        importedCount = importedCount,
        skippedCount = skippedCount,
        failedCount = failedCount,
        checkpoint = checkpoint,
        errorMessage = errorMessage?.takeIf { it.isNotBlank() },
        updatedAt = updatedAt,
    )
}
