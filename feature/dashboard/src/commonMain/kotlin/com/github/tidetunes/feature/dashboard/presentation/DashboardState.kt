package com.github.tidetunes.feature.dashboard.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class DashboardState(
    val sleepEnabled: Boolean = false,
    val sleepRemainingMs: Long = 0L,
    val sleepHour: Int = 0,
    val sleepMinute: Int = 0,
    val importJobs: ImmutableList<ImportJobUi> = persistentListOf(),
)

@Immutable
data class ImportJobUi(
    val id: String,
    val status: LibrarySyncStatus,
    val scannedCount: Long,
    val importedCount: Long,
    val skippedCount: Long,
    val failedCount: Long,
    val checkpoint: String?,
    val errorMessage: String?,
    val hasError: Boolean,
    val isActive: Boolean,
    val canResume: Boolean,
    val canRetry: Boolean,
    val statusLabel: String,
)
