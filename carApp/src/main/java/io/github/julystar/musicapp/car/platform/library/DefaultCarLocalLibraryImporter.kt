package io.github.julystar.musicapp.car.platform.library

import android.os.Environment
import io.github.julystar.musicapp.car.presentation.screen.CarLocalLibraryImporter
import io.github.julystar.musicapp.core.domain.repository.StorageRepository
import io.github.julystar.musicapp.service.librarysync.domain.SourceAccountLibrarySyncController
import io.github.julystar.musicapp.service.librarysync.domain.SourceAccountLibrarySyncResult
import kotlinx.coroutines.flow.first

class DefaultCarLocalLibraryImporter(
    private val storageRepository: StorageRepository,
    private val syncController: SourceAccountLibrarySyncController,
    private val musicDirectory: () -> String = {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
    },
) : CarLocalLibraryImporter {
    override suspend fun importMusicFolder(): SourceAccountLibrarySyncResult {
        storageRepository.reload()
        val localAccount = storageRepository.storageAccounts
            .first { accounts -> accounts.any { it.isLocal } }
            .first { it.isLocal }
        storageRepository.replaceAccountRootPaths(
            accountId = localAccount.accountId,
            rootPaths = listOf(musicDirectory()),
        )
        return syncController.sync(localAccount.accountId)
    }
}
