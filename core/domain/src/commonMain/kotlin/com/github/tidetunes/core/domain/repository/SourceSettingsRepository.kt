package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.LocalMusicDirectory
import com.github.tidetunes.core.domain.model.SourceAccountId
import kotlinx.coroutines.flow.Flow

interface SourceSettingsRepository {
    val localDirectories: Flow<List<LocalMusicDirectory>>

    suspend fun setAccountEnabled(accountId: SourceAccountId, enabled: Boolean)
    suspend fun removeLocalDirectory(id: String)
}
