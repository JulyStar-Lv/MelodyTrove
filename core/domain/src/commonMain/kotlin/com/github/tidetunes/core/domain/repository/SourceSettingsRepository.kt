package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.LocalMusicDirectory
import kotlinx.coroutines.flow.Flow

interface SourceSettingsRepository {
    val localDirectories: Flow<List<LocalMusicDirectory>>

    suspend fun setLocalMusicEnabled(enabled: Boolean)
    suspend fun setWebDavEnabled(enabled: Boolean)
    suspend fun removeLocalDirectory(id: String)
}
