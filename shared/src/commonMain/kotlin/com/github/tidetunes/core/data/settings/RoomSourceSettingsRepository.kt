package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.domain.model.LocalMusicDirectory
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.core.domain.model.toStorageRouteIdOrNull
import com.github.tidetunes.core.domain.repository.SourceSettingsRepository
import com.github.tidetunes.database.LibraryRootDao
import com.github.tidetunes.database.LibraryRootEntity
import com.github.tidetunes.database.SourceAccountDao
import com.github.tidetunes.platform.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSourceSettingsRepository(
    private val sourceAccountDao: SourceAccountDao,
    private val libraryRootDao: LibraryRootDao,
) : SourceSettingsRepository {
    override val localDirectories: Flow<List<LocalMusicDirectory>> =
        libraryRootDao.observeBySourceAccount(LOCAL_STORAGE_ID).map { roots ->
            roots.map { root -> root.toLocalMusicDirectory() }
        }

    override suspend fun setAccountEnabled(accountId: SourceAccountId, enabled: Boolean) {
        val id = accountId.toStorageRouteIdOrNull() ?: return
        sourceAccountDao.setEnabled(id = id, enabled = enabled, updatedAt = currentTimeMillis())
    }

    override suspend fun removeLocalDirectory(id: String) {
        val rootId = id.toLongOrNull() ?: return
        libraryRootDao.delete(rootId)
    }

    private fun LibraryRootEntity.toLocalMusicDirectory(): LocalMusicDirectory {
        return LocalMusicDirectory(
            id = id.toString(),
            accountId = storageSourceAccountId(sourceAccountId),
            displayName = displayName,
            path = canonicalPath ?: providerRootId.orEmpty(),
            lastScannedAtEpochMs = lastSyncAt,
        )
    }
}

private const val LOCAL_STORAGE_ID = 1L
