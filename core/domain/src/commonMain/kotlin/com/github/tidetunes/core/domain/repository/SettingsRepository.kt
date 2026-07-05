package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.SourceAccountId
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setLanguageMode(mode: AppLanguageMode)
    suspend fun setPauseOnDisconnect(enabled: Boolean)
    suspend fun setAllowMixedPlayback(enabled: Boolean)
    suspend fun setKeepScreenOnInPlayer(enabled: Boolean)
    suspend fun setLocalMusicEnabled(enabled: Boolean)
    suspend fun setLocalScanSubdirectories(enabled: Boolean)
    suspend fun setIgnoreShortAudio(enabled: Boolean)
    suspend fun setWebDavEnabled(enabled: Boolean)
    suspend fun setWebDavScanSubdirectories(enabled: Boolean)
    suspend fun setWebDavRootPath(accountId: SourceAccountId, rootPath: String)
    suspend fun removeWebDavRootPath(accountId: SourceAccountId)
    suspend fun setAudioCacheLimitBytes(bytes: Long)
}
