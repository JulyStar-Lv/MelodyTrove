package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.DEFAULT_IGNORED_SOURCE_DIRECTORIES
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.MAX_MINIMUM_AUDIO_DURATION_MS
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.SUPPORTED_AUDIO_EXTENSIONS
import com.github.tidetunes.core.domain.model.SourceConnectionTestStatus
import com.github.tidetunes.core.presentation.components.TideDialog
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.components.TideTextField
import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SourceSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings
    var metadataScanModeDialogOpen by remember { mutableStateOf(false) }
    var customDurationDialogOpen by remember { mutableStateOf(false) }
    var customDurationInputSeconds by remember { mutableStateOf("") }
    var editingMetadataField by remember { mutableStateOf<MetadataField?>(null) }
    var metadataFieldValue by remember { mutableStateOf("") }

    SettingsPageLayout(title = stringResource(Res.string.settings_sources_title), onBack = onBack) {
        SettingsSection(title = stringResource(Res.string.settings_sources_section)) {
            state.sourceAccounts.forEach { account ->
                val sourceTitle = if (account.isLocal) {
                    stringResource(Res.string.settings_source_local)
                } else {
                    account.title.ifBlank { stringResource(Res.string.settings_source_webdav) }
                }
                SettingsSwitchRow(
                    title = sourceTitle,
                    summary = stringResource(
                        Res.string.settings_source_summary,
                        account.sourceLabel,
                        if (account.enabled) {
                            stringResource(Res.string.settings_source_enabled)
                        } else {
                            stringResource(Res.string.settings_source_disabled)
                        },
                        account.trackCount,
                    ),
                    checked = account.enabled,
                    onCheckedChange = {
                        onAction(SettingsAction.SetAccountEnabled(account.accountId, it))
                    },
                )
                if (account.isWebDav) {
                    SettingsInfoRow(
                        title = stringResource(Res.string.settings_source_edit, sourceTitle),
                        value = stringResource(
                            Res.string.settings_source_edit_summary,
                            account.subtitle,
                            account.rootPath ?: "/",
                        ),
                        enabled = account.enabled,
                        onClick = {
                            onAction(SettingsAction.OpenEditWebDavDialog(account.accountId))
                        },
                    )
                }
                if (!account.isRemoteServer) {
                    SettingsInfoRow(
                        title = stringResource(Res.string.settings_source_scan, sourceTitle),
                        value = stringResource(Res.string.settings_source_scan_summary),
                        enabled = account.enabled,
                        onClick = { onAction(SettingsAction.ScanSourceAccount(account.accountId)) },
                    )
                    SettingsInfoRow(
                        title = stringResource(Res.string.settings_source_last_scan),
                        value = account.lastScanSummary(),
                    )
                }
            }
            if (state.localDirectories.isEmpty()) {
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_source_local),
                    value = stringResource(Res.string.settings_source_no_directory),
                )
            } else {
                state.localDirectories.forEach { directory ->
                    SettingsInfoRow(
                        title = directory.displayName.ifBlank { directory.path },
                        value = stringResource(Res.string.settings_source_remove_hint),
                        onClick = {
                            onAction(
                                SettingsAction.RequestRemoveLocalDirectory(
                                    id = directory.id,
                                    title = directory.displayName.ifBlank { directory.path },
                                )
                            )
                        },
                    )
                }
            }
            if (state.capabilities.customMusicDirectorySupported) {
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_source_add_local),
                    value = stringResource(Res.string.settings_source_add_local_summary),
                    onClick = { onAction(SettingsAction.RequestAddLocalDirectory) },
                )
            }
            if (state.capabilities.secureCredentialStoreSupported) {
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_source_add_webdav),
                    value = stringResource(Res.string.settings_source_add_webdav_summary),
                    onClick = { onAction(SettingsAction.OpenAddWebDavDialog) },
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.settings_scan_section)) {
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_auto_scan),
                summary = stringResource(Res.string.settings_auto_scan_off),
                selected = settings.autoScanMode == AutoScanMode.Off,
                onClick = { onAction(SettingsAction.SetAutoScanMode(AutoScanMode.Off)) },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_auto_scan),
                summary = stringResource(Res.string.settings_auto_scan_startup),
                selected = settings.autoScanMode == AutoScanMode.OnStartup,
                onClick = { onAction(SettingsAction.SetAutoScanMode(AutoScanMode.OnStartup)) },
            )
            if (state.capabilities.backgroundScanSupported) {
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_auto_scan),
                    summary = stringResource(Res.string.settings_auto_scan_periodic),
                    selected = settings.autoScanMode == AutoScanMode.Periodic,
                    onClick = { onAction(SettingsAction.SetAutoScanMode(AutoScanMode.Periodic)) },
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_background_scan),
                    summary = stringResource(Res.string.settings_background_scan_summary),
                    checked = settings.backgroundScanEnabled,
                    onCheckedChange = { onAction(SettingsAction.SetBackgroundScanEnabled(it)) },
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_scan_unmetered),
                    summary = stringResource(Res.string.settings_scan_unmetered_summary),
                    checked = settings.scanOnlyOnUnmeteredNetwork,
                    onCheckedChange = {
                        onAction(SettingsAction.SetScanOnlyOnUnmeteredNetwork(it))
                    },
                )
            }
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_scan_subdirectories),
                summary = stringResource(Res.string.settings_scan_subdirectories_summary),
                checked = settings.scanSubdirectories,
                onCheckedChange = { onAction(SettingsAction.SetScanSubdirectories(it)) },
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_metadata_scan),
                value = settings.webDavMetadataScanMode.summary(),
                onClick = { metadataScanModeDialogOpen = true },
            )
            listOf(0L, 15_000L, 30_000L, 60_000L).forEach { durationMs ->
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_min_duration),
                    summary = if (durationMs == 0L) {
                        stringResource(Res.string.settings_min_duration_off)
                    } else {
                        stringResource(Res.string.settings_seconds, durationMs / 1_000L)
                    },
                    selected = settings.minimumAudioDurationMs == durationMs,
                    onClick = {
                        onAction(SettingsAction.SetMinimumAudioDurationMs(durationMs))
                    },
                )
            }
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_min_duration_custom),
                summary = stringResource(
                    Res.string.settings_min_duration_custom_summary,
                    settings.minimumAudioDurationMs / 1_000L,
                ),
                selected = settings.minimumAudioDurationMs !in MINIMUM_DURATION_PRESETS_MS,
                onClick = {
                    customDurationInputSeconds =
                        (settings.minimumAudioDurationMs / 1_000L).toString()
                    customDurationDialogOpen = true
                },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_missing_file),
                summary = stringResource(Res.string.settings_missing_mark_summary),
                selected = settings.missingFilePolicy == MissingFilePolicy.MarkUnavailable,
                onClick = {
                    onAction(SettingsAction.SetMissingFilePolicy(MissingFilePolicy.MarkUnavailable))
                },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_missing_file),
                summary = stringResource(Res.string.settings_missing_remove_summary),
                selected = settings.missingFilePolicy == MissingFilePolicy.RemoveOnScan,
                onClick = {
                    onAction(SettingsAction.SetMissingFilePolicy(MissingFilePolicy.RemoveOnScan))
                },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_duplicate_policy),
                summary = stringResource(Res.string.settings_duplicate_separate_summary),
                selected = settings.duplicateTrackPolicy == DuplicateTrackPolicy.SeparateBySource,
                onClick = {
                    onAction(
                        SettingsAction.SetDuplicateTrackPolicy(DuplicateTrackPolicy.SeparateBySource)
                    )
                },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_duplicate_policy),
                summary = stringResource(Res.string.settings_duplicate_keep_all_summary),
                selected = settings.duplicateTrackPolicy == DuplicateTrackPolicy.KeepAll,
                onClick = {
                    onAction(SettingsAction.SetDuplicateTrackPolicy(DuplicateTrackPolicy.KeepAll))
                },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_scan_status_section)) {
            val latestTask = state.scanTasks.firstOrNull()
            if (latestTask == null) {
                SettingsInfoRow(
                    title = stringResource(Res.string.settings_scan_status_section),
                    value = stringResource(Res.string.settings_scan_no_history),
                )
            } else {
                SettingsInfoRow(
                    title = latestTask.folderDisplayPath,
                    value = latestTask.statusSummary(),
                    onClick = if (latestTask.status.isActiveInSettings()) {
                        { onAction(SettingsAction.CancelScan(latestTask.id)) }
                    } else null,
                )
                if (latestTask.failedCount > 0L) {
                    SettingsInfoRow(
                        title = stringResource(Res.string.settings_scan_failure_details),
                        value = stringResource(
                            Res.string.settings_scan_failure_count,
                            latestTask.failedCount,
                        ),
                        onClick = { onAction(SettingsAction.OpenScanFailures(latestTask.id)) },
                    )
                } else {
                    latestTask.errorMessage?.let { message ->
                        SettingsInfoRow(
                            title = stringResource(Res.string.settings_scan_failure_details),
                            value = message,
                        )
                    }
                }
            }
        }

        SettingsSection(title = stringResource(Res.string.settings_maintenance_section)) {
            SettingsInfoRow(
                title = stringResource(Res.string.settings_scan_all),
                value = stringResource(Res.string.settings_scan_all_summary),
                enabled = !state.maintenanceOperationInProgress,
                onClick = { onAction(SettingsAction.ScanAllSources) },
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_refresh_missing_artwork),
                value = stringResource(Res.string.settings_refresh_missing_artwork_summary),
                enabled = !state.maintenanceOperationInProgress,
                onClick = { onAction(SettingsAction.RefreshMissingArtwork) },
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_refresh_missing_lyrics),
                value = stringResource(Res.string.settings_refresh_missing_lyrics_summary),
                enabled = !state.maintenanceOperationInProgress,
                onClick = { onAction(SettingsAction.RefreshMissingLyrics) },
            )
            SettingsDangerRow(
                title = stringResource(Res.string.settings_rebuild_library),
                summary = stringResource(Res.string.settings_rebuild_library_summary),
                enabled = !state.maintenanceOperationInProgress,
                onClick = { onAction(SettingsAction.RequestRebuildLibrary) },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_scan_section)) {
            SettingsInfoRow(
                title = stringResource(Res.string.settings_supported_formats),
                value = SUPPORTED_AUDIO_EXTENSIONS.joinToString(", "),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_hidden_files),
                value = stringResource(Res.string.settings_hidden_files_summary),
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_ignored_directories),
                value = DEFAULT_IGNORED_SOURCE_DIRECTORIES.joinToString(", "),
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_metadata_parsing_section)) {
            SettingsInfoRow(
                title = stringResource(Res.string.settings_artist_separators),
                value = settings.metadataParsing.artistSeparators,
                onClick = {
                    editingMetadataField = MetadataField.ArtistSeparators
                    metadataFieldValue = settings.metadataParsing.artistSeparators
                },
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_artist_protected_names),
                value = stringResource(
                    Res.string.settings_protected_names_count,
                    settings.metadataParsing.artistProtectedNames.lineSequence()
                        .count(String::isNotBlank),
                ),
                onClick = {
                    editingMetadataField = MetadataField.ArtistProtectedNames
                    metadataFieldValue = settings.metadataParsing.artistProtectedNames
                },
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_genre_separators),
                value = settings.metadataParsing.genreSeparators,
                onClick = {
                    editingMetadataField = MetadataField.GenreSeparators
                    metadataFieldValue = settings.metadataParsing.genreSeparators
                },
            )
            SettingsInfoRow(
                title = stringResource(Res.string.settings_genre_protected_names),
                value = stringResource(
                    Res.string.settings_protected_names_count,
                    settings.metadataParsing.genreProtectedNames.lineSequence()
                        .count(String::isNotBlank),
                ),
                onClick = {
                    editingMetadataField = MetadataField.GenreProtectedNames
                    metadataFieldValue = settings.metadataParsing.genreProtectedNames
                },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_ignore_tag_case),
                summary = stringResource(Res.string.settings_ignore_tag_case_summary),
                checked = settings.metadataParsing.ignoreTagCase,
                onCheckedChange = {
                    onAction(
                        SettingsAction.SetMetadataParsingSettings(
                            settings.metadataParsing.copy(ignoreTagCase = it)
                        )
                    )
                },
            )
        }
    }

    MetadataScanModeDialog(
        show = metadataScanModeDialogOpen,
        selectedMode = settings.webDavMetadataScanMode,
        onSelect = { mode ->
            onAction(SettingsAction.SetWebDavMetadataScanMode(mode))
            metadataScanModeDialogOpen = false
        },
        onDismiss = { metadataScanModeDialogOpen = false },
    )
    SettingsInputDialog(
        show = customDurationDialogOpen,
        title = stringResource(Res.string.settings_min_duration_custom_title),
        message = stringResource(Res.string.settings_min_duration_custom_message),
        value = customDurationInputSeconds,
        label = stringResource(Res.string.settings_seconds_unit),
        onValueChange = { customDurationInputSeconds = it.filter(Char::isDigit) },
        onConfirm = {
            customDurationInputSeconds.toLongOrNull()?.let { seconds ->
                val normalizedSeconds = seconds.coerceIn(
                    minimumValue = 0L,
                    maximumValue = MAX_MINIMUM_AUDIO_DURATION_MS / 1_000L,
                )
                onAction(SettingsAction.SetMinimumAudioDurationMs(normalizedSeconds * 1_000L))
                customDurationDialogOpen = false
            }
        },
        onDismiss = { customDurationDialogOpen = false },
    )
    val selectedMetadataField = editingMetadataField
    SettingsInputDialog(
        show = selectedMetadataField != null,
        title = stringResource(selectedMetadataField?.titleResource() ?: Res.string.settings_artist_separators),
        message = stringResource(
            if (selectedMetadataField?.multiline == true) {
                Res.string.settings_protected_names_hint
            } else {
                Res.string.settings_separators_hint
            },
        ),
        value = metadataFieldValue,
        label = stringResource(selectedMetadataField?.titleResource() ?: Res.string.settings_artist_separators),
        singleLine = selectedMetadataField?.multiline != true,
        onValueChange = { metadataFieldValue = it },
        onConfirm = {
            val current = settings.metadataParsing
            val updated = when (selectedMetadataField) {
                MetadataField.ArtistSeparators -> current.copy(artistSeparators = metadataFieldValue)
                MetadataField.ArtistProtectedNames -> current.copy(artistProtectedNames = metadataFieldValue)
                MetadataField.GenreSeparators -> current.copy(genreSeparators = metadataFieldValue)
                MetadataField.GenreProtectedNames -> current.copy(genreProtectedNames = metadataFieldValue)
                null -> current
            }
            onAction(SettingsAction.SetMetadataParsingSettings(updated))
            editingMetadataField = null
        },
        onDismiss = { editingMetadataField = null },
    )
    WebDavAccountDialog(state = state, dialog = state.webDavDialog, onAction = onAction)
    SettingsConfirmDialog(
        show = state.pendingConfirmation is SettingsConfirmation.RemoveLocalDirectory,
        title = stringResource(Res.string.settings_confirm_remove_directory_title),
        message = stringResource(Res.string.settings_confirm_remove_directory_message),
        confirmText = stringResource(Res.string.settings_confirm),
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation is SettingsConfirmation.DeleteWebDavAccount,
        title = stringResource(Res.string.settings_confirm_delete_webdav_title),
        message = stringResource(Res.string.settings_confirm_delete_webdav_message),
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
    ScanFailureDialog(
        show = state.failureDialogTaskId != null,
        failures = state.failureDetails,
        onDismiss = { onAction(SettingsAction.DismissScanFailures) },
    )
}

@Composable
private fun MetadataScanModeDialog(
    show: Boolean,
    selectedMode: MetadataScanMode,
    onSelect: (MetadataScanMode) -> Unit,
    onDismiss: () -> Unit,
) {
    TideDialog(show = show, onDismiss = onDismiss) {
        Column {
            Text(
                text = stringResource(Res.string.settings_metadata_scan),
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetadataScanMode.entries.forEach { mode ->
                SettingsChoiceRow(
                    title = mode.title(),
                    summary = mode.description(),
                    selected = selectedMode == mode,
                    onClick = { onSelect(mode) },
                )
            }
        }
    }
}

@Composable
private fun MetadataScanMode.title(): String = when (this) {
    MetadataScanMode.Fast -> stringResource(Res.string.settings_metadata_scan_fast)
    MetadataScanMode.Standard -> stringResource(Res.string.settings_metadata_scan_standard)
    MetadataScanMode.Full -> stringResource(Res.string.settings_metadata_scan_full)
}

@Composable
private fun MetadataScanMode.description(): String = when (this) {
    MetadataScanMode.Fast -> stringResource(Res.string.settings_metadata_scan_fast_description)
    MetadataScanMode.Standard -> stringResource(Res.string.settings_metadata_scan_standard_description)
    MetadataScanMode.Full -> stringResource(Res.string.settings_metadata_scan_full_description)
}

@Composable
private fun MetadataScanMode.summary(): String = when (this) {
    MetadataScanMode.Fast -> stringResource(Res.string.settings_metadata_scan_fast_summary)
    MetadataScanMode.Standard -> stringResource(Res.string.settings_metadata_scan_standard_summary)
    MetadataScanMode.Full -> stringResource(Res.string.settings_metadata_scan_full_summary)
}

@Composable
private fun ScanFailureDialog(
    show: Boolean,
    failures: List<LibrarySyncFailure>,
    onDismiss: () -> Unit,
) {
    TideDialog(show = show, onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(Res.string.settings_failure_dialog_title),
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (failures.isEmpty()) {
                Text(
                    text = stringResource(Res.string.settings_failure_empty),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                failures.forEach { failure ->
                    ScanFailureItem(failure.toScanFailureDisplay())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TideTextButton(
                    text = stringResource(Res.string.settings_close),
                    variant = TideTextButtonVariant.Default,
                    size = TideTextButtonSize.Medium,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun ScanFailureItem(failure: ScanFailureDisplay) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                Res.string.settings_failure_file,
                failure.fileName ?: stringResource(Res.string.settings_failure_unknown_file),
            ),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
        )
        failure.directory?.let { directory ->
            Text(
                text = stringResource(Res.string.settings_failure_location, directory),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Text(
            text = stringResource(Res.string.settings_failure_reason, failure.reason.localizedText()),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun ScanFailureReason.localizedText(): String = when (this) {
    ScanFailureReason.Unknown -> stringResource(Res.string.settings_feedback_unknown_error)
    is ScanFailureReason.RemoteRead -> {
        val status = httpStatus?.let { rawStatus ->
            when {
                rawStatus.startsWith("500 ") -> stringResource(Res.string.settings_failure_http_500)
                rawStatus.startsWith("404 ") -> stringResource(Res.string.settings_failure_http_404)
                rawStatus.startsWith("401 ") -> stringResource(Res.string.settings_failure_http_401)
                rawStatus.startsWith("403 ") -> stringResource(Res.string.settings_failure_http_403)
                else -> "HTTP $rawStatus"
            }
        }
        if (status == null) {
            stringResource(Res.string.settings_failure_remote_read_unknown)
        } else {
            stringResource(Res.string.settings_failure_remote_read_status, status)
        }
    }
    is ScanFailureReason.ByteBudget -> {
        val limit = limitBytes?.let(::formatBytes)
        if (limit == null) {
            stringResource(Res.string.settings_failure_byte_budget)
        } else {
            stringResource(Res.string.settings_failure_byte_budget_limit, limit)
        }
    }
    ScanFailureReason.UnsupportedContainer ->
        stringResource(Res.string.settings_failure_unsupported_container)
    ScanFailureReason.MissingMetadata ->
        stringResource(Res.string.settings_failure_missing_metadata)
    is ScanFailureReason.MetadataError ->
        stringResource(Res.string.settings_failure_metadata_error, detail)
    is ScanFailureReason.Raw -> detail
}

@Composable
private fun WebDavAccountDialog(
    state: SettingsUiState,
    dialog: WebDavAccountDialogState?,
    onAction: (SettingsAction) -> Unit,
) {
    if (dialog == null) return
    var password by remember(dialog.accountId, dialog.isEditing) { mutableStateOf("") }
    TideDialog(show = true, onDismiss = { onAction(SettingsAction.DismissWebDavDialog) }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(
                    if (dialog.isEditing) Res.string.settings_webdav_edit_title
                    else Res.string.settings_webdav_add_title
                ),
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TideTextField(
                value = dialog.name,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogName(it)) },
                label = stringResource(Res.string.settings_webdav_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TideTextField(
                value = dialog.serverUrl,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogServerUrl(it)) },
                label = stringResource(Res.string.settings_webdav_url),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TideTextField(
                value = dialog.username,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogUsername(it)) },
                label = stringResource(Res.string.settings_webdav_username),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TideTextField(
                value = password,
                onValueChange = {
                    password = it
                    onAction(SettingsAction.ResetWebDavConnectionTest)
                },
                label = stringResource(
                    if (dialog.isEditing) Res.string.settings_webdav_password_edit
                    else Res.string.settings_webdav_password_new
                ),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TideTextField(
                value = dialog.rootPath,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogRootPath(it)) },
                label = stringResource(Res.string.settings_webdav_root),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.webDavConnectionTestMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MiuixTheme.textStyles.body2,
                    color = if (state.webDavConnectionTestStatus == SourceConnectionTestStatus.Error) {
                        MiuixTheme.colorScheme.error
                    } else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (dialog.isEditing && dialog.accountId != null) {
                    TideTextButton(
                        text = stringResource(Res.string.settings_delete),
                        variant = TideTextButtonVariant.Error,
                        size = TideTextButtonSize.Medium,
                        onClick = {
                            onAction(
                                SettingsAction.RequestDeleteWebDavAccount(
                                    dialog.accountId,
                                    dialog.name.ifBlank { dialog.serverUrl },
                                )
                            )
                        },
                    )
                }
                TideTextButton(
                    text = stringResource(Res.string.settings_cancel),
                    variant = TideTextButtonVariant.Default,
                    size = TideTextButtonSize.Medium,
                    onClick = { onAction(SettingsAction.DismissWebDavDialog) },
                )
                TideTextButton(
                    text = stringResource(Res.string.settings_test),
                    variant = TideTextButtonVariant.Default,
                    size = TideTextButtonSize.Medium,
                    onClick = { onAction(SettingsAction.TestWebDavConnection(password)) },
                )
                TideTextButton(
                    text = stringResource(Res.string.settings_save),
                    variant = TideTextButtonVariant.Primary,
                    size = TideTextButtonSize.Medium,
                    onClick = { onAction(SettingsAction.SaveWebDavAccount(password)) },
                )
            }
        }
    }
}

@Composable
private fun LibrarySyncTask.statusSummary(): String {
    val statusText = when (status) {
        LibrarySyncStatus.Queued -> stringResource(Res.string.settings_scan_status_queued)
        LibrarySyncStatus.Running -> stringResource(Res.string.settings_scan_status_running)
        LibrarySyncStatus.Paused -> stringResource(Res.string.settings_scan_status_paused)
        LibrarySyncStatus.Completed -> stringResource(Res.string.settings_scan_status_completed)
        LibrarySyncStatus.CompletedWithErrors -> {
            stringResource(Res.string.settings_scan_status_completed_errors)
        }
        LibrarySyncStatus.Failed -> stringResource(Res.string.settings_scan_status_failed)
        LibrarySyncStatus.Cancelled -> stringResource(Res.string.settings_scan_status_cancelled)
        LibrarySyncStatus.Unknown -> stringResource(Res.string.settings_scan_status_unknown)
    }
    return stringResource(
        Res.string.settings_scan_status_summary,
        statusText,
        syncMode,
        scannedCount.toString(),
        addedCount.toString(),
        (modifiedCount + renamedCount).toString(),
        deletedCount.toString(),
        skippedCount.toString(),
        metadataRequestCount.toString(),
        totalElapsedMs.toString(),
    )
}

private fun LibrarySyncStatus.isActiveInSettings(): Boolean {
    return this == LibrarySyncStatus.Queued ||
        this == LibrarySyncStatus.Running ||
        this == LibrarySyncStatus.Paused
}

@Composable
private fun SourceAccountSettingsItem.lastScanSummary(): String {
    val timestamp = lastScanAtEpochMs?.let { epochMs ->
        runCatching { Instant.fromEpochMilliseconds(epochMs).toString() }.getOrNull()
    }
    if (timestamp == null) return stringResource(Res.string.settings_source_never_scanned)
    val status = when (lastScanStatus) {
        "RUNNING" -> stringResource(Res.string.settings_scan_status_running)
        "PAUSED" -> stringResource(Res.string.settings_scan_status_paused)
        "SYNCED" -> stringResource(Res.string.settings_scan_status_completed)
        "SYNCED_WITH_ERRORS" -> stringResource(Res.string.settings_scan_status_completed_errors)
        "CANCELLED" -> stringResource(Res.string.settings_scan_status_cancelled)
        "FAILED" -> stringResource(Res.string.settings_scan_status_failed)
        else -> stringResource(Res.string.settings_scan_status_unknown)
    }
    return stringResource(Res.string.settings_source_last_scan_summary, status, timestamp)
}

private val MINIMUM_DURATION_PRESETS_MS = setOf(0L, 15_000L, 30_000L, 60_000L)

private enum class MetadataField(val multiline: Boolean) {
    ArtistSeparators(false),
    ArtistProtectedNames(true),
    GenreSeparators(false),
    GenreProtectedNames(true),
}

private fun MetadataField.titleResource() = when (this) {
    MetadataField.ArtistSeparators -> Res.string.settings_artist_separators
    MetadataField.ArtistProtectedNames -> Res.string.settings_artist_protected_names
    MetadataField.GenreSeparators -> Res.string.settings_genre_separators
    MetadataField.GenreProtectedNames -> Res.string.settings_genre_protected_names
}
