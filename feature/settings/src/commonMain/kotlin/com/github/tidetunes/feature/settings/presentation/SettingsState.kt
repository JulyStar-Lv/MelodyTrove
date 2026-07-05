package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.LocalMusicDirectory
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
    val customCacheLimitDialogOpen: Boolean = false,
    val customCacheLimitInputMb: String = "",
    val localDirectories: List<LocalMusicDirectory> = emptyList(),
    val webDavAccounts: List<WebDavAccountSettingsItem> = emptyList(),
    val scanTasks: List<LibrarySyncTask> = emptyList(),
    val sourceOperationInProgress: Boolean = false,
    val webDavDialog: WebDavAccountDialogState? = null,
    val webDavConnectionTestStatus: SourceConnectionTestStatus = SourceConnectionTestStatus.None,
    val webDavConnectionTestMessage: String? = null,
    val failureDialogTaskId: String? = null,
    val failureDetails: List<LibrarySyncFailure> = emptyList(),
)

enum class SettingsPage {
    Home,
    Appearance,
    Playback,
    Source,
    Storage,
    About,
    Licenses,
}

sealed interface SettingsConfirmation {
    data object ClearAudio : SettingsConfirmation
    data object ClearImage : SettingsConfirmation
    data class RemoveLocalDirectory(
        val id: String,
        val title: String,
    ) : SettingsConfirmation

    data class DeleteWebDavAccount(
        val accountId: SourceAccountId,
        val title: String,
    ) : SettingsConfirmation
}

sealed interface SettingsAction {
    data class SetThemeMode(val mode: AppThemeMode) : SettingsAction
    data class SetDynamicColorEnabled(val enabled: Boolean) : SettingsAction
    data class SetLanguageMode(val mode: AppLanguageMode) : SettingsAction
    data class SetPauseOnDisconnect(val enabled: Boolean) : SettingsAction
    data class SetAllowMixedPlayback(val enabled: Boolean) : SettingsAction
    data class SetKeepScreenOnInPlayer(val enabled: Boolean) : SettingsAction
    data class SetLocalMusicEnabled(val enabled: Boolean) : SettingsAction
    data class SetLocalScanSubdirectories(val enabled: Boolean) : SettingsAction
    data class SetIgnoreShortAudio(val enabled: Boolean) : SettingsAction
    data object RequestAddLocalDirectory : SettingsAction
    data class RequestRemoveLocalDirectory(
        val id: String,
        val title: String,
    ) : SettingsAction
    data object ScanLocalMusic : SettingsAction
    data class SetWebDavEnabled(val enabled: Boolean) : SettingsAction
    data class SetWebDavScanSubdirectories(val enabled: Boolean) : SettingsAction
    data object OpenAddWebDavDialog : SettingsAction
    data class OpenEditWebDavDialog(val accountId: SourceAccountId) : SettingsAction
    data object DismissWebDavDialog : SettingsAction
    data class SetWebDavDialogName(val value: String) : SettingsAction
    data class SetWebDavDialogServerUrl(val value: String) : SettingsAction
    data class SetWebDavDialogUsername(val value: String) : SettingsAction
    data class SetWebDavDialogPassword(val value: String) : SettingsAction
    data class SetWebDavDialogRootPath(val value: String) : SettingsAction
    data object TestWebDavConnection : SettingsAction
    data object SaveWebDavAccount : SettingsAction
    data class RequestDeleteWebDavAccount(
        val accountId: SourceAccountId,
        val title: String,
    ) : SettingsAction
    data class ScanWebDavAccount(val accountId: SourceAccountId) : SettingsAction
    data class CancelScan(val scanId: String) : SettingsAction
    data class OpenScanFailures(val scanId: String) : SettingsAction
    data object DismissScanFailures : SettingsAction
    data class SetAudioCacheLimitBytes(val bytes: Long) : SettingsAction
    data class SetCustomCacheLimitInput(val value: String) : SettingsAction
    data object OpenCustomCacheLimitDialog : SettingsAction
    data object DismissCustomCacheLimitDialog : SettingsAction
    data object ApplyCustomCacheLimit : SettingsAction
    data object RefreshStorageUsage : SettingsAction
    data object RequestClearAudio : SettingsAction
    data object RequestClearImage : SettingsAction
    data object DismissConfirmation : SettingsAction
    data object ConfirmPendingAction : SettingsAction
}

sealed interface SettingsEvent {
    data object OpenLibraryFolderImport : SettingsEvent
}

@Immutable
data class WebDavAccountSettingsItem(
    val accountId: SourceAccountId,
    val title: String,
    val subtitle: String,
    val rootPath: String,
)

@Immutable
data class WebDavAccountDialogState(
    val accountId: SourceAccountId? = null,
    val name: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val rootPath: String = "/",
) {
    val isEditing: Boolean
        get() = accountId != null
}
