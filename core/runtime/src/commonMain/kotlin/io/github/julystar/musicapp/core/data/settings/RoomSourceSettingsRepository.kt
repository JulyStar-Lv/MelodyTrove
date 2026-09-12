package io.github.julystar.musicapp.core.data.settings

import io.github.julystar.musicapp.core.domain.model.LocalMusicDirectory
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.storageSourceAccountId
import io.github.julystar.musicapp.core.domain.model.toStorageRouteIdOrNull
import io.github.julystar.musicapp.core.domain.repository.SourceSettingsRepository
import io.github.julystar.musicapp.database.LibraryRootDao
import io.github.julystar.musicapp.database.LibraryRootEntity
import io.github.julystar.musicapp.database.SourceAccountDao
import io.github.julystar.musicapp.platform.currentTimeMillis
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
