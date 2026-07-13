package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.DiagnosticsExportResult
import com.github.tidetunes.core.domain.model.DiagnosticsReport
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.LibraryRebuildState
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setLanguageMode(mode: AppLanguageMode)
    suspend fun setAudioFocusMode(mode: AudioFocusMode)
    suspend fun setPauseOnDisconnect(enabled: Boolean)
    suspend fun setGaplessPlaybackEnabled(enabled: Boolean)
    suspend fun setRetryPlaybackOnFailure(enabled: Boolean)
    suspend fun setResumePlaybackAfterNetworkRecovery(enabled: Boolean)
    suspend fun setKeepScreenOnInPlayer(enabled: Boolean)
    suspend fun setAutoScanMode(mode: AutoScanMode)
    suspend fun setBackgroundScanEnabled(enabled: Boolean)
    suspend fun setScanOnlyOnUnmeteredNetwork(enabled: Boolean)
    suspend fun setScanSubdirectories(enabled: Boolean)
    suspend fun setWebDavMetadataScanMode(mode: MetadataScanMode)
    suspend fun setMinimumAudioDurationMs(value: Long)
    suspend fun setMissingFilePolicy(policy: MissingFilePolicy)
    suspend fun setDuplicateTrackPolicy(policy: DuplicateTrackPolicy)
    suspend fun setAllowMeteredStreaming(enabled: Boolean)
    suspend fun setBackgroundSyncOnlyOnUnmeteredNetwork(enabled: Boolean)
    suspend fun setNetworkRetryCount(value: Int)
    suspend fun setConnectionTimeoutSeconds(value: Int)
    suspend fun setAudioPreloadBytes(bytes: Long)
    suspend fun setAudioCacheLimitBytes(bytes: Long)
    suspend fun setImageCacheLimitBytes(bytes: Long)
    suspend fun resetToDefaults()
}

interface SettingsMigration {
    suspend fun migrate()
}

interface NetworkStatusProvider {
    val status: StateFlow<NetworkStatus>
}

interface DiagnosticsService {
    suspend fun collectDiagnostics(): DiagnosticsReport
    suspend fun exportDiagnostics(): DiagnosticsExportResult
}

interface LibraryMaintenanceService {
    val rebuildState: StateFlow<LibraryRebuildState>
    suspend fun rebuildLibrary()
}
