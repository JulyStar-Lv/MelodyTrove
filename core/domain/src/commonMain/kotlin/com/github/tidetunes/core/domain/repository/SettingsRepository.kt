package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.AudioEffectSettings
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.DiagnosticsExportResult
import com.github.tidetunes.core.domain.model.DiagnosticsReport
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.LibraryRebuildState
import com.github.tidetunes.core.domain.model.LyricFontSettings
import com.github.tidetunes.core.domain.model.LyricOutputSettings
import com.github.tidetunes.core.domain.model.LyricSourceKind
import com.github.tidetunes.core.domain.model.LyricSourceMode
import com.github.tidetunes.core.domain.model.LyricTextAlignment
import com.github.tidetunes.core.domain.model.MetadataParsingSettings
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.NetworkStatus
import com.github.tidetunes.core.domain.model.PlaybackAdvancedSettings
import com.github.tidetunes.core.domain.model.PlayerInteractionSettings
import com.github.tidetunes.core.domain.model.SettingsBackupSettings
import com.github.tidetunes.core.domain.model.SettingsBackupResult
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
    suspend fun setLyricTextAlignment(alignment: LyricTextAlignment)
    suspend fun setLyricPrimaryFontScalePercent(value: Int)
    suspend fun setLyricPrimaryFontSizeSp(value: Int)
    suspend fun setLyricSecondaryFontScalePercent(value: Int)
    suspend fun setLyricSecondaryFontSizeSp(value: Int)
    suspend fun setLyricTranslationVisible(visible: Boolean)
    suspend fun setLyricWordLiftEnabled(enabled: Boolean)
    suspend fun setLyricBlurEffectEnabled(enabled: Boolean)
    suspend fun setLyricPerspectiveEffectEnabled(enabled: Boolean)
    suspend fun setLyricPerspectiveAngleDegrees(value: Int)
    suspend fun setLyricTapToSeekEnabled(enabled: Boolean)
    suspend fun setLyricSourceMode(mode: LyricSourceMode) = Unit
    suspend fun setLyricSourcePriority(priority: List<LyricSourceKind>) = Unit
    suspend fun setIgnoreLyricHeaderTags(enabled: Boolean) = Unit
    suspend fun setLyricLineBlacklist(lines: List<String>) = Unit
    suspend fun setLyricFontSettings(settings: LyricFontSettings) = Unit
    suspend fun setPlaybackAdvancedSettings(settings: PlaybackAdvancedSettings) = Unit
    suspend fun setPlayerInteractionSettings(settings: PlayerInteractionSettings) = Unit
    suspend fun setMetadataParsingSettings(settings: MetadataParsingSettings) = Unit
    suspend fun setAudioEffectSettings(settings: AudioEffectSettings) = Unit
    suspend fun setLyricOutputSettings(settings: LyricOutputSettings) = Unit
    suspend fun setBackupSettings(settings: SettingsBackupSettings) = Unit
    suspend fun replaceSettings(settings: AppSettings) = Unit
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

interface AppDataClearService {
    suspend fun clearAllData()
}

interface SettingsBackupService {
    suspend fun createBackup(): SettingsBackupResult
    suspend fun restoreLatestBackup(): SettingsBackupResult
}
