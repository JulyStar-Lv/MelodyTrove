package com.github.tidetunes.core.data.settings

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.github.tidetunes.core.domain.model.LibraryRebuildState
import com.github.tidetunes.core.domain.model.LibraryRebuildStatus
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.metadataScanModeFor
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.core.domain.repository.LibraryMaintenanceService
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.database.LibraryRootDao
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.SourceAccountDao
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncRequest
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class RoomLibraryMaintenanceService(
    private val database: TideTunesDatabase,
    private val sourceAccountDao: SourceAccountDao,
    private val libraryRootDao: LibraryRootDao,
    private val settingsRepository: SettingsRepository,
    private val librarySyncController: LibrarySyncController,
) : LibraryMaintenanceService {
    private val mutableRebuildState = MutableStateFlow(LibraryRebuildState())
    override val rebuildState: StateFlow<LibraryRebuildState> = mutableRebuildState.asStateFlow()

    override suspend fun rebuildLibrary() {
        check(librarySyncController.recentTasks.first().none { task -> task.status.isActive() }) {
            "A library scan is already running"
        }
        val accounts = sourceAccountDao.listAll().associateBy { account -> account.id }
        val roots = libraryRootDao.listAll().filter { root -> accounts[root.sourceAccountId]?.enabled == true }
        mutableRebuildState.value = LibraryRebuildState(
            status = LibraryRebuildStatus.Clearing,
            totalSources = roots.size,
        )

        try {
            clearRegenerableScanData()
            val settings = settingsRepository.settings.first()
            val scanRules = settings.toLibrarySyncScanRules()
            val failures = mutableListOf<String>()
            roots.forEachIndexed { index, root ->
                val account = accounts[root.sourceAccountId] ?: return@forEachIndexed
                mutableRebuildState.value = LibraryRebuildState(
                    status = LibraryRebuildStatus.Scanning,
                    completedSources = index,
                    totalSources = roots.size,
                )
                val canonicalPath = root.canonicalPath
                    ?: account.rootPath
                    ?: "/"
                runCatching {
                    librarySyncController.syncFolder(
                        LibrarySyncRequest(
                            accountId = storageSourceAccountId(account.id),
                            selectedFolderRemoteId = root.providerRootId,
                            selectedFolderCanonicalPath = canonicalPath,
                            selectedFolderDisplayPath = root.displayName,
                            scanRules = scanRules,
                            metadataScanMode = settings.metadataScanModeFor(
                                account.providerType == ProviderTypes.WebDav,
                            ),
                        )
                    )
                }.onFailure { error ->
                    failures += "${root.displayName}: ${error.message ?: "unknown error"}"
                }
            }
            if (failures.isNotEmpty()) {
                error(failures.joinToString("; "))
            }
            mutableRebuildState.value = LibraryRebuildState(
                status = LibraryRebuildStatus.Completed,
                completedSources = roots.size,
                totalSources = roots.size,
            )
        } catch (error: Throwable) {
            mutableRebuildState.value = LibraryRebuildState(
                status = LibraryRebuildStatus.Failed,
                totalSources = roots.size,
                failureMessage = error.message ?: "Unknown rebuild error",
            )
            throw error
        }
    }

    private suspend fun clearRegenerableScanData() {
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                database.sourceErrorDao().deleteAll()
                database.sourceSyncCursorDao().deleteAll()
                database.syncDao().deleteAllJobs()
                database.sourceItemDao().deleteAll()
                database.libraryRootDao().resetSyncState()
            }
        }
    }
}

private fun LibrarySyncStatus.isActive(): Boolean {
    return this == LibrarySyncStatus.Queued ||
        this == LibrarySyncStatus.Running ||
        this == LibrarySyncStatus.Paused
}

internal fun AppSettings.toLibrarySyncScanRules() =
    com.github.tidetunes.service.librarysync.domain.LibrarySyncScanRules(
        scanSubdirectories = scanSubdirectories,
        minDurationMs = minimumAudioDurationMs,
        missingFilePolicy = missingFilePolicy,
        duplicateTrackPolicy = duplicateTrackPolicy,
    )
