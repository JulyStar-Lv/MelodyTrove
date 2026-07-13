package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.*

@Composable
fun StorageSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val usage = state.storageUsage
    val busy = state.maintenanceOperationInProgress

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

        SettingsSection(title = stringResource(Res.string.settings_danger_section)) {
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
        show = state.pendingConfirmation == SettingsConfirmation.RebuildLibrary,
        title = stringResource(Res.string.settings_confirm_rebuild_title),
        message = stringResource(Res.string.settings_confirm_rebuild_message),
        confirmText = stringResource(Res.string.settings_confirm),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
}
