package io.github.julystar.musicapp.core.data.settings

import io.github.julystar.musicapp.core.domain.model.AutoScanMode
import io.github.julystar.musicapp.core.domain.model.metadataScanModeFor
import io.github.julystar.musicapp.core.domain.model.storageSourceAccountId
import io.github.julystar.musicapp.core.domain.repository.NetworkStatusProvider
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import io.github.julystar.musicapp.database.LibraryRootDao
import io.github.julystar.musicapp.database.ProviderTypes
import io.github.julystar.musicapp.database.SourceAccountDao
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncController
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncRequest
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncStatus
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
        if (!settings.allowMeteredNetworkUsage && network.isMetered) return@withLock
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
