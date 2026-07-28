package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.LocalMusicDirectory
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import kotlinx.coroutines.flow.Flow

interface SourceSettingsRepository {
    val localDirectories: Flow<List<LocalMusicDirectory>>

    suspend fun setAccountEnabled(accountId: SourceAccountId, enabled: Boolean)
    suspend fun removeLocalDirectory(id: String)
}
