package io.github.julystar.musicapp.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.julystar.musicapp.core.domain.model.BackupSchedule
import io.github.julystar.musicapp.core.domain.model.toStorageRouteIdOrNull
import org.jetbrains.compose.resources.stringResource
import musicapp.feature.settings.generated.resources.*

@Composable
fun StorageSettingsSection(
    state: SettingsUiState,
    onBack: (() -> Unit)?,
    onAction: (SettingsAction) -> Unit,
) {
    val usage = state.storageUsage
    val busy = state.maintenanceOperationInProgress
    val backup = state.settings.backup
    var editingRemoteDirectory by remember { mutableStateOf(false) }
    var remoteDirectoryInput by remember(backup.remoteDirectory) {
        mutableStateOf(backup.remoteDirectory)
    }

    SettingsPageLayout(title = stringResource(Res.string.settings_storage_title), onBack = onBack) {
        SettingsSection(title = stringResource(Res.string.settings_usage_section)) {
            SettingsInfoRow(
                title = stringResource(Res.string.settings_usage_audio),
                value = formatBytes(usage.audioBytes),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_usage_image),
                value = formatBytes(usage.imageBytes),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_usage_downloads),
                value = formatBytes(usage.downloadBytes),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_usage_database),
                value = formatBytes(usage.databaseBytes),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_usage_logs),
                value = formatBytes(usage.logBytes),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_usage_total),
                value = formatBytes(usage.totalBytes),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_usage_refresh),
                value = if (state.storageRefreshing) {
                    stringResource(Res.string.settings_usage_refreshing)
                } else {
                    stringResource(Res.string.settings_usage_refresh)
                },
                enabled = !state.storageRefreshing && !busy,
                onClick = { onAction(SettingsAction.RefreshStorageUsage) },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_cleanup_section)) {
            SettingsDangerRow(
                title = stringResource(Res.string.settings_clear_audio),
                summary = stringResource(Res.string.settings_clear_audio_summary),
                enabled = !busy,
                onClick = { onAction(SettingsAction.RequestClearAudio) },
            )
            SettingsDangerRow(
                title = stringResource(Res.string.settings_clear_image),
                summary = stringResource(Res.string.settings_clear_image_summary),
                enabled = !busy,
                onClick = { onAction(SettingsAction.RequestClearImage) },
            )
            SettingsDangerRow(
                title = stringResource(Res.string.settings_clear_all),
                summary = stringResource(Res.string.settings_clear_all_summary),
                enabled = !busy,
                onClick = { onAction(SettingsAction.RequestClearAllCaches) },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_data_section)) {
            if (state.capabilities.diagnosticsExportSupported) {
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_export_diagnostics),
                    value = stringResource(Res.string.settings_export_diagnostics_summary),
                    enabled = !busy,
                    onClick = { onAction(SettingsAction.ExportDiagnostics) },
                )
            }
            SettingsDangerRow(
                title = stringResource(Res.string.settings_reset_defaults),
                summary = stringResource(Res.string.settings_reset_defaults_summary),
                enabled = !busy,
                onClick = { onAction(SettingsAction.RequestResetDefaults) },
            )
        }

        if (state.capabilities.settingsBackupSupported) {
            SettingsSection(title = stringResource(Res.string.settings_backup_section)) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_backup_appearance),
                    checked = backup.selection.appearance,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.SetBackupSettings(
                                backup.copy(selection = backup.selection.copy(appearance = it))
                            )
                        )
                    },
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_backup_playback),
                    checked = backup.selection.playback,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.SetBackupSettings(
                                backup.copy(selection = backup.selection.copy(playback = it))
                            )
                        )
                    },
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_backup_lyrics),
                    checked = backup.selection.lyrics,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.SetBackupSettings(
                                backup.copy(selection = backup.selection.copy(lyrics = it))
                            )
                        )
                    },
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_backup_library),
                    checked = backup.selection.libraryAndMetadata,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.SetBackupSettings(
                                backup.copy(
                                    selection = backup.selection.copy(libraryAndMetadata = it)
                                )
                            )
                        )
                    },
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_backup_network),
                    checked = backup.selection.networkAndCache,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.SetBackupSettings(
                                backup.copy(
                                    selection = backup.selection.copy(networkAndCache = it)
                                )
                            )
                        )
                    },
                )
                if (state.capabilities.scheduledBackupSupported) {
                    SettingsSelectRow(
                        label = stringResource(Res.string.settings_backup_schedule),
                        selected = backup.schedule,
                        options = BackupSchedule.entries.toList(),
                        optionLabel = { schedule -> stringResource(schedule.titleResource()) },
                        onSelect = { schedule ->
                            onAction(SettingsAction.SetBackupSettings(backup.copy(schedule = schedule)))
                        },
                    )
                    val webDavAccounts = state.sourceAccounts
                        .filter(SourceAccountSettingsItem::isWebDav)
                        .mapNotNull { account ->
                            account.accountId.toStorageRouteIdOrNull()?.let { accountId ->
                                SettingsSelectOption(value = accountId.toString(), label = account.title)
                            }
                        }
                    if (webDavAccounts.isNotEmpty()) {
                        val selectedAccount = webDavAccounts.firstOrNull {
                            it.value == backup.webDavAccountId?.toString()
                        }
                        SettingsSelectRow(
                            label = stringResource(Res.string.settings_backup_webdav_account),
                            selectedValue = selectedAccount?.value.orEmpty(),
                            selectedLabel = selectedAccount?.label ?: "—",
                            options = webDavAccounts,
                            onSelect = { accountId ->
                                accountId.toLongOrNull()?.let { storageAccountId ->
                                    onAction(
                                        SettingsAction.SetBackupSettings(
                                            backup.copy(webDavAccountId = storageAccountId)
                                        )
                                    )
                                }
                            },
                        )
                    }
                    SettingsInfoRow(
                        title = stringResource(Res.string.settings_backup_remote_directory),
                        value = backup.remoteDirectory,
                        onClick = {
                            remoteDirectoryInput = backup.remoteDirectory
                            editingRemoteDirectory = true
                        },
                    )
                }
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_backup_create),
                    value = stringResource(Res.string.settings_backup_create_summary),
                    onClick = { onAction(SettingsAction.CreateSettingsBackup) },
                )
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_backup_restore),
                    value = stringResource(Res.string.settings_backup_restore_summary),
                    onClick = { onAction(SettingsAction.RestoreLatestSettingsBackup) },
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.settings_danger_section)) {
            SettingsDangerRow(
                title = stringResource(Res.string.settings_clear_all_data),
                summary = stringResource(Res.string.settings_clear_all_data_summary),
                enabled = !busy,
                onClick = { onAction(SettingsAction.RequestClearAllData) },
            )
            SettingsDangerRow(
                title = stringResource(Res.string.settings_rebuild_library),
                summary = stringResource(Res.string.settings_rebuild_library_summary),
                enabled = !busy,
                onClick = { onAction(SettingsAction.RequestRebuildLibrary) },
            )
            if (busy) {
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_operation_running),
                    value = state.rebuildState.failureMessage.orEmpty(),
                )
            }
        }
    }

    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.ClearAudio,
        title = stringResource(Res.string.settings_confirm_clear_audio_title),
        message = stringResource(Res.string.settings_confirm_clear_audio_message),
        confirmText = stringResource(Res.string.settings_confirm),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsInputDialog(
        show = editingRemoteDirectory,
        title = stringResource(Res.string.settings_backup_remote_directory),
        message = stringResource(Res.string.settings_backup_remote_directory_summary),
        value = remoteDirectoryInput,
        label = stringResource(Res.string.settings_backup_remote_directory),
        onValueChange = { remoteDirectoryInput = it },
        onConfirm = {
            onAction(
                SettingsAction.SetBackupSettings(
                    backup.copy(remoteDirectory = remoteDirectoryInput.trim().ifBlank { "/" })
                )
            )
            editingRemoteDirectory = false
        },
        onDismiss = { editingRemoteDirectory = false },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.ClearImage,
        title = stringResource(Res.string.settings_confirm_clear_image_title),
        message = stringResource(Res.string.settings_confirm_clear_image_message),
        confirmText = stringResource(Res.string.settings_confirm),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.ClearAllCaches,
        title = stringResource(Res.string.settings_confirm_clear_all_title),
        message = stringResource(Res.string.settings_confirm_clear_all_message),
        confirmText = stringResource(Res.string.settings_confirm),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.ResetDefaults,
        title = stringResource(Res.string.settings_confirm_reset_title),
        message = stringResource(Res.string.settings_confirm_reset_message),
        confirmText = stringResource(Res.string.settings_confirm),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.ClearAllData,
        title = stringResource(Res.string.settings_confirm_clear_all_data_title),
        message = stringResource(Res.string.settings_confirm_clear_all_data_message),
        confirmText = stringResource(Res.string.settings_delete),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.RebuildLibrary,
        title = stringResource(Res.string.settings_confirm_rebuild_title),
        message = stringResource(Res.string.settings_confirm_rebuild_message),
        confirmText = stringResource(Res.string.settings_confirm),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
}

private fun BackupSchedule.titleResource() = when (this) {
    BackupSchedule.Off -> Res.string.settings_backup_schedule_off
    BackupSchedule.Daily -> Res.string.settings_backup_schedule_daily
    BackupSchedule.Weekly -> Res.string.settings_backup_schedule_weekly
}
