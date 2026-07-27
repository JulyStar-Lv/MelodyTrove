package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.metadataScanModeFor
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.core.domain.repository.NetworkStatusProvider
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.database.LibraryRootDao
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.SourceAccountDao
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncRequest
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AutoScanCoordinator(
    private val settingsRepository: SettingsRepository,
    private val sourceAccountDao: SourceAccountDao,
    private val libraryRootDao: LibraryRootDao,
    private val networkStatusProvider: NetworkStatusProvider,
    private val librarySyncController: LibrarySyncController,
) {
    private val startMutex = Mutex()
    private var started = false

    suspend fun runStartupScan() = startMutex.withLock {
        if (started) return@withLock
        started = true
        val settings = settingsRepository.settings.first()
        if (settings.autoScanMode == AutoScanMode.Off) return@withLock
        val network = networkStatusProvider.status.value
        if (!network.isOnline) return@withLock
        if (settings.scanOnlyOnUnmeteredNetwork && network.isMetered) return@withLock
        if (librarySyncController.recentTasks.first().any { task -> task.status.isActiveScan() }) {
            return@withLock
        }

        val accounts = sourceAccountDao.listAll().associateBy { account -> account.id }
        val rules = settings.toLibrarySyncScanRules()
        libraryRootDao.listAll()
            .filter { root -> accounts[root.sourceAccountId]?.enabled == true }
            .forEach { root ->
                val account = accounts[root.sourceAccountId] ?: return@forEach
                val canonicalPath = root.canonicalPath ?: account.rootPath ?: "/"
                runCatching {
                    librarySyncController.syncFolder(
                        LibrarySyncRequest(
                            accountId = storageSourceAccountId(account.id),
                            selectedFolderRemoteId = root.providerRootId,
                            selectedFolderCanonicalPath = canonicalPath,
                            selectedFolderDisplayPath = root.displayName,
                            scanRules = rules,
                            metadataScanMode = settings.metadataScanModeFor(
                                account.providerType == ProviderTypes.WebDav ||
                                    account.providerType == ProviderTypes.Smb,
                            ),
                        )
                    )
                }
            }
    }
}

private fun LibrarySyncStatus.isActiveScan(): Boolean {
    return this == LibrarySyncStatus.Queued ||
        this == LibrarySyncStatus.Running ||
        this == LibrarySyncStatus.Paused
}
