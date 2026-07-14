package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.LibraryRebuildState
import com.github.tidetunes.core.domain.model.LocalMusicDirectory
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.SettingsCapabilities
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceConnectionTestStatus
import com.github.tidetunes.core.domain.model.StorageUsage
import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask

@Immutable
data class SettingsUiState(
    val settings: AppSettings = AppSettings.Default,
    val capabilities: SettingsCapabilities = SettingsCapabilities(),
    val storageUsage: StorageUsage = StorageUsage.Unknown,
    val storageRefreshing: Boolean = false,
    val pendingConfirmation: SettingsConfirmation? = null,
    val customCacheLimitDialog: CacheLimitType? = null,
    val customCacheLimitInputMb: String = "",
    val localDirectories: List<LocalMusicDirectory> = emptyList(),
    val sourceAccounts: List<SourceAccountSettingsItem> = emptyList(),
    val scanTasks: List<LibrarySyncTask> = emptyList(),
    val sourceOperationInProgress: Boolean = false,
    val maintenanceOperationInProgress: Boolean = false,
    val rebuildState: LibraryRebuildState = LibraryRebuildState(),
    val webDavDialog: WebDavAccountDialogState? = null,
    val webDavConnectionTestStatus: SourceConnectionTestStatus = SourceConnectionTestStatus.None,
    val webDavConnectionTestMessage: String? = null,
    val failureDialogTaskId: String? = null,
    val failureDetails: List<LibrarySyncFailure> = emptyList(),
) {
    val enabledSourceCount: Int
        get() = sourceAccounts.count(SourceAccountSettingsItem::enabled)

    val trackCount: Long
        get() = sourceAccounts.sumOf(SourceAccountSettingsItem::trackCount)
}

enum class SettingsPage {
    Home,
    Appearance,
    Playback,
    Source,
    NetworkCache,
    Storage,
    About,
    Licenses,
}

enum class CacheLimitType {
    Audio,
    Image,
}

sealed interface SettingsConfirmation {
    data object ClearAudio : SettingsConfirmation
    data object ClearImage : SettingsConfirmation
    data object ClearAllCaches : SettingsConfirmation
    data object ClearAllData : SettingsConfirmation
    data object ResetDefaults : SettingsConfirmation
    data object RebuildLibrary : SettingsConfirmation
    data class RemoveLocalDirectory(val id: String, val title: String) : SettingsConfirmation
    data class DeleteWebDavAccount(
        val accountId: SourceAccountId,
        val title: String,
    ) : SettingsConfirmation
}

sealed interface SettingsAction {
    data class SetThemeMode(val mode: AppThemeMode) : SettingsAction
    data class SetDynamicColorEnabled(val enabled: Boolean) : SettingsAction
    data class SetLanguageMode(val mode: AppLanguageMode) : SettingsAction
    data class SetAudioFocusMode(val mode: AudioFocusMode) : SettingsAction
    data class SetPauseOnDisconnect(val enabled: Boolean) : SettingsAction
    data class SetGaplessPlaybackEnabled(val enabled: Boolean) : SettingsAction
    data class SetRetryPlaybackOnFailure(val enabled: Boolean) : SettingsAction
    data class SetResumePlaybackAfterNetworkRecovery(val enabled: Boolean) : SettingsAction
    data class SetKeepScreenOnInPlayer(val enabled: Boolean) : SettingsAction
    data class SetAutoScanMode(val mode: AutoScanMode) : SettingsAction
    data class SetBackgroundScanEnabled(val enabled: Boolean) : SettingsAction
    data class SetScanOnlyOnUnmeteredNetwork(val enabled: Boolean) : SettingsAction
    data class SetScanSubdirectories(val enabled: Boolean) : SettingsAction
    data class SetWebDavMetadataScanMode(val mode: MetadataScanMode) : SettingsAction
    data class SetMinimumAudioDurationMs(val value: Long) : SettingsAction
    data class SetMissingFilePolicy(val policy: MissingFilePolicy) : SettingsAction
    data class SetDuplicateTrackPolicy(val policy: DuplicateTrackPolicy) : SettingsAction
    data class SetAllowMeteredStreaming(val enabled: Boolean) : SettingsAction
    data class SetBackgroundSyncOnlyOnUnmeteredNetwork(val enabled: Boolean) : SettingsAction
    data class SetNetworkRetryCount(val value: Int) : SettingsAction
    data class SetConnectionTimeoutSeconds(val value: Int) : SettingsAction
    data class SetAudioPreloadBytes(val bytes: Long) : SettingsAction
    data class SetAccountEnabled(val accountId: SourceAccountId, val enabled: Boolean) : SettingsAction
    data object RequestAddLocalDirectory : SettingsAction
    data class RequestRemoveLocalDirectory(val id: String, val title: String) : SettingsAction
    data object ScanAllSources : SettingsAction
    data object RefreshMissingArtwork : SettingsAction
    data object RefreshMissingLyrics : SettingsAction
    data object ScanLocalMusic : SettingsAction
    data object OpenAddWebDavDialog : SettingsAction
    data class OpenEditWebDavDialog(val accountId: SourceAccountId) : SettingsAction
    data object DismissWebDavDialog : SettingsAction
    data class SetWebDavDialogName(val value: String) : SettingsAction
    data class SetWebDavDialogServerUrl(val value: String) : SettingsAction
    data class SetWebDavDialogUsername(val value: String) : SettingsAction
    data class SetWebDavDialogRootPath(val value: String) : SettingsAction
    data object ResetWebDavConnectionTest : SettingsAction
    data class TestWebDavConnection(val password: String) : SettingsAction
    data class SaveWebDavAccount(val password: String) : SettingsAction
    data class RequestDeleteWebDavAccount(
        val accountId: SourceAccountId,
        val title: String,
    ) : SettingsAction
    data class ScanSourceAccount(val accountId: SourceAccountId) : SettingsAction
    data class CancelScan(val scanId: String) : SettingsAction
    data class OpenScanFailures(val scanId: String) : SettingsAction
    data object DismissScanFailures : SettingsAction
    data class SetAudioCacheLimitBytes(val bytes: Long) : SettingsAction
    data class SetImageCacheLimitBytes(val bytes: Long) : SettingsAction
    data class SetCustomCacheLimitInput(val value: String) : SettingsAction
    data class OpenCustomCacheLimitDialog(val type: CacheLimitType) : SettingsAction
    data object DismissCustomCacheLimitDialog : SettingsAction
    data object ApplyCustomCacheLimit : SettingsAction
    data object RefreshStorageUsage : SettingsAction
    data object RequestClearAudio : SettingsAction
    data object RequestClearImage : SettingsAction
    data object RequestClearAllCaches : SettingsAction
    data object RequestClearAllData : SettingsAction
    data object RequestResetDefaults : SettingsAction
    data object RequestRebuildLibrary : SettingsAction
    data object ExportDiagnostics : SettingsAction
    data object DismissConfirmation : SettingsAction
    data object ConfirmPendingAction : SettingsAction
}

sealed interface SettingsEvent {
    data object OpenLibraryFolderImport : SettingsEvent
}

@Immutable
data class SourceAccountSettingsItem(
    val accountId: SourceAccountId,
    val title: String,
    val subtitle: String,
    val rootPath: String?,
    val enabled: Boolean,
    val trackCount: Long,
    val lastScanAtEpochMs: Long?,
    val lastScanStatus: String?,
    val isLocal: Boolean,
    val isWebDav: Boolean,
)

@Immutable
data class WebDavAccountDialogState(
    val accountId: SourceAccountId? = null,
    val name: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val rootPath: String = "/",
) {
    val isEditing: Boolean
        get() = accountId != null
}
