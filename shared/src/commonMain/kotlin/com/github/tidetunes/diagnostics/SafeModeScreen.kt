package com.github.tidetunes.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.DiagnosticExportBundleRequest
import com.github.tidetunes.core.presentation.components.AppButton
import com.github.tidetunes.core.presentation.components.AppText
import com.github.tidetunes.core.presentation.components.TideDialog
import com.github.tidetunes.core.presentation.components.TidePreferenceRow
import com.github.tidetunes.core.presentation.components.TideSettingsGroup
import com.github.tidetunes.core.presentation.theme.TideTunesTheme
import com.github.tidetunes.core.presentation.theme.TideTunesThemeMode
import com.github.tidetunes.platform.diagnosticExportPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import tidetunes.shared.generated.resources.Res
import kotlin.time.Instant
import tidetunes.shared.generated.resources.diagnostics_copy_summary
import tidetunes.shared.generated.resources.diagnostics_action_failed
import tidetunes.shared.generated.resources.diagnostics_backup_playlists
import tidetunes.shared.generated.resources.diagnostics_backup_ready
import tidetunes.shared.generated.resources.diagnostics_backup_settings
import tidetunes.shared.generated.resources.diagnostics_cache_cleared
import tidetunes.shared.generated.resources.diagnostics_cancel
import tidetunes.shared.generated.resources.diagnostics_check_database
import tidetunes.shared.generated.resources.diagnostics_clear_cache
import tidetunes.shared.generated.resources.diagnostics_clear_queue
import tidetunes.shared.generated.resources.diagnostics_confirm
import tidetunes.shared.generated.resources.diagnostics_confirm_action
import tidetunes.shared.generated.resources.diagnostics_confirm_action_message
import tidetunes.shared.generated.resources.diagnostics_database_missing
import tidetunes.shared.generated.resources.diagnostics_database_result
import tidetunes.shared.generated.resources.diagnostics_disable_plugins
import tidetunes.shared.generated.resources.diagnostics_disable_remote
import tidetunes.shared.generated.resources.diagnostics_disable_scan
import tidetunes.shared.generated.resources.diagnostics_export_bundle
import tidetunes.shared.generated.resources.diagnostics_export_failed
import tidetunes.shared.generated.resources.diagnostics_export_ready
import tidetunes.shared.generated.resources.diagnostics_hide_incident
import tidetunes.shared.generated.resources.diagnostics_no_incident
import tidetunes.shared.generated.resources.diagnostics_rebuild_library
import tidetunes.shared.generated.resources.diagnostics_recovery_actions
import tidetunes.shared.generated.resources.diagnostics_reset_audio
import tidetunes.shared.generated.resources.diagnostics_restore_defaults
import tidetunes.shared.generated.resources.diagnostics_selected
import tidetunes.shared.generated.resources.diagnostics_safe_mode_disabled
import tidetunes.shared.generated.resources.diagnostics_safe_mode_privacy
import tidetunes.shared.generated.resources.diagnostics_safe_mode_reason
import tidetunes.shared.generated.resources.diagnostics_safe_mode_title
import tidetunes.shared.generated.resources.diagnostics_summary_copied
import tidetunes.shared.generated.resources.diagnostics_try_normal
import tidetunes.shared.generated.resources.diagnostics_view_incident
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Suppress("DEPRECATION")
fun SafeModeScreen(
    state: DiagnosticsBootstrapState,
    onTryNormalStartup: (Set<String>) -> Unit,
) {
    TideTunesTheme(themeMode = TideTunesThemeMode.FollowSystem) {
        val scope = rememberCoroutineScope()
        val clipboard = LocalClipboardManager.current
        val incident = state.pendingIncidents.firstOrNull {
            it.id == state.startupPlan.primaryIncidentId
        } ?: state.pendingIncidents.firstOrNull()
        val summary = remember(state, incident) { state.safeModeSummary(incident) }
        var showDetail by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf<String?>(null) }
        var selectedOptions by remember {
            mutableStateOf(SafeModeRecoveryStore.selectedOptions())
        }
        var pendingAction by remember { mutableStateOf<SafeModeAction?>(null) }
        var recoveryInProgress by remember { mutableStateOf(false) }
        val exportReadyText = stringResource(Res.string.diagnostics_export_ready)
        val exportFailedText = stringResource(Res.string.diagnostics_export_failed)
        val summaryCopiedText = stringResource(Res.string.diagnostics_summary_copied)
        val actionFailedTemplate = stringResource(Res.string.diagnostics_action_failed)
        val backupReadyTemplate = stringResource(Res.string.diagnostics_backup_ready)
        val cacheClearedTemplate = stringResource(Res.string.diagnostics_cache_cleared)
        val databaseResultTemplate = stringResource(Res.string.diagnostics_database_result)
        val databaseMissingText = stringResource(Res.string.diagnostics_database_missing)
        val selectedText = stringResource(Res.string.diagnostics_selected)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AppText(
                    text = stringResource(Res.string.diagnostics_safe_mode_title),
                    style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold),
                )
                TideSettingsGroup(
                    title = stringResource(Res.string.diagnostics_safe_mode_reason),
                ) {
                    TidePreferenceRow(
                        title = state.startupPlan.reason
                            ?: stringResource(Res.string.diagnostics_no_incident),
                        summary = incident?.let {
                            "${it.type} · ${it.detectedAtEpochMs.toIncidentTime()} · " +
                                "${it.startupStage ?: "UNKNOWN"} · ×${it.occurrenceCount}"
                        },
                    )
                    if (showDetail && incident != null) {
                        TidePreferenceRow(
                            title = incident.summary,
                            summary = incident.detail ?: incident.artifactPaths.joinToString("\n"),
                        )
                    }
                }
                TideSettingsGroup(
                    title = stringResource(Res.string.diagnostics_safe_mode_disabled),
                ) {
                    state.startupPlan.disabledComponents.sorted().forEach { component ->
                        TidePreferenceRow(title = component)
                    }
                }
                AppText(
                    text = stringResource(Res.string.diagnostics_safe_mode_privacy),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TideSettingsGroup(
                    title = stringResource(Res.string.diagnostics_recovery_actions),
                ) {
                    RecoveryRow(
                        title = stringResource(Res.string.diagnostics_backup_settings),
                        onClick = {
                            scope.launch {
                                status = withContext(Dispatchers.Default) {
                                    runCatching { SafeModeRecoveryStore.backupSettings() }
                                }.fold(
                                    onSuccess = { backupReadyTemplate.replace("%1\$s", it) },
                                    onFailure = {
                                        actionFailedTemplate.replace("%1\$s", it.message.orEmpty())
                                    },
                                )
                            }
                        },
                    )
                    RecoveryRow(
                        title = stringResource(Res.string.diagnostics_backup_playlists),
                        onClick = {
                            scope.launch {
                                status = withContext(Dispatchers.Default) {
                                    runCatching { SafeModeRecoveryStore.backupPlaylists() }
                                }.fold(
                                    onSuccess = { backupReadyTemplate.replace("%1\$s", it) },
                                    onFailure = {
                                        actionFailedTemplate.replace("%1\$s", it.message.orEmpty())
                                    },
                                )
                            }
                        },
                    )
                    recoveryOptionRows().forEach { action ->
                        RecoveryRow(
                            title = stringResource(action.title),
                            selected = action.option in selectedOptions,
                            onClick = { pendingAction = action },
                        )
                    }
                    RecoveryRow(
                        title = stringResource(Res.string.diagnostics_clear_cache),
                        onClick = { pendingAction = SafeModeAction.ClearCache },
                    )
                    RecoveryRow(
                        title = stringResource(Res.string.diagnostics_check_database),
                        onClick = {
                            scope.launch {
                                status = withContext(Dispatchers.Default) {
                                    runCatching { SafeModeRecoveryStore.checkDatabase() }
                                }.fold(
                                    onSuccess = { result ->
                                        if (!result.databaseExists) {
                                            databaseMissingText
                                        } else {
                                            databaseResultTemplate
                                                .replace("%1\$s", result.integrityResult.orEmpty())
                                                .replace("%2\$d", (result.userVersion ?: 0).toString())
                                                .replace("%3\$s", result.walExists.toString())
                                                .replace("%4\$s", result.shmExists.toString())
                                        }
                                    },
                                    onFailure = {
                                        actionFailedTemplate.replace("%1\$s", it.message.orEmpty())
                                    },
                                )
                            }
                        },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppButton(
                        onClick = {
                            scope.launch {
                                val result = runCatching {
                                    val bundle = withContext(Dispatchers.Default) {
                                        val databaseCheck =
                                            SafeModeRecoveryStore.databaseCheckSummaryJson()
                                        RustDiagnosticsRepository.export(
                                            DiagnosticExportBundleRequest(
                                                summary = summary,
                                                environmentJson =
                                                    """{"startupMode":"safe","databaseCheck":""" +
                                                        databaseCheck +
                                                        "}",
                                                playbackSummaryJson = "{}",
                                                scanSummaryJson = "{}",
                                                pluginSummaryJson = "{}",
                                                sourceSummaryJson = "{}",
                                                storageSummaryJson = "{}",
                                            )
                                        )
                                    }
                                    try {
                                        diagnosticExportPresenter().share(bundle.path).getOrThrow()
                                    } finally {
                                        RustDiagnosticsRepository.releaseExport(bundle.path)
                                    }
                                }
                                status = if (result.isSuccess) exportReadyText else exportFailedText
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AppText(stringResource(Res.string.diagnostics_export_bundle))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(summary))
                                status = summaryCopiedText
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            AppText(stringResource(Res.string.diagnostics_copy_summary))
                        }
                        AppButton(
                            onClick = { showDetail = !showDetail },
                            modifier = Modifier.weight(1f),
                            enabled = incident != null,
                        ) {
                            AppText(
                                stringResource(
                                    if (showDetail) {
                                        Res.string.diagnostics_hide_incident
                                    } else {
                                        Res.string.diagnostics_view_incident
                                    }
                                )
                            )
                        }
                    }
                    AppButton(
                        onClick = {
                            if (!recoveryInProgress) {
                                recoveryInProgress = true
                                onTryNormalStartup(SafeModeRecoveryStore.disabledComponents())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !recoveryInProgress,
                    ) {
                        AppText(stringResource(Res.string.diagnostics_try_normal))
                    }
                }
                status?.let { AppText(it) }
                Spacer(Modifier.height(8.dp))
            }
        }

        TideDialog(
            show = pendingAction != null,
            onDismiss = { pendingAction = null },
        ) {
            AppText(
                text = stringResource(Res.string.diagnostics_confirm_action),
                style = MiuixTheme.textStyles.title3,
            )
            AppText(
                text = stringResource(Res.string.diagnostics_confirm_action_message),
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AppButton(onClick = { pendingAction = null }) {
                    AppText(stringResource(Res.string.diagnostics_cancel))
                }
                AppButton(
                    onClick = {
                        when (val action = pendingAction) {
                            is SafeModeAction.Option -> {
                                selectedOptions = SafeModeRecoveryStore.setOption(
                                    action.option,
                                    action.option !in selectedOptions,
                                )
                                status = selectedText.takeIf { action.option in selectedOptions }
                            }
                            SafeModeAction.ClearCache -> {
                                scope.launch {
                                    status = withContext(Dispatchers.Default) {
                                        runCatching { SafeModeRecoveryStore.clearCache() }
                                    }.fold(
                                        onSuccess = {
                                            cacheClearedTemplate.replace("%1\$d", it.toString())
                                        },
                                        onFailure = {
                                            actionFailedTemplate.replace(
                                                "%1\$s",
                                                it.message.orEmpty(),
                                            )
                                        },
                                    )
                                }
                            }
                            null -> Unit
                        }
                        pendingAction = null
                    },
                ) {
                    AppText(stringResource(Res.string.diagnostics_confirm))
                }
            }
        }
    }
}

@Composable
private fun RecoveryRow(
    title: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    TidePreferenceRow(
        title = if (selected) "✓ $title" else title,
        summary = if (selected) {
            stringResource(Res.string.diagnostics_selected)
        } else {
            null
        },
        onClick = onClick,
    )
}

private sealed interface SafeModeAction {
    data class Option(
        val option: SafeModeRecoveryOption,
        val title: org.jetbrains.compose.resources.StringResource,
    ) : SafeModeAction

    data object ClearCache : SafeModeAction
}

private fun recoveryOptionRows(): List<SafeModeAction.Option> = listOf(
    SafeModeAction.Option(
        SafeModeRecoveryOption.DisableThirdPartyPlugins,
        Res.string.diagnostics_disable_plugins,
    ),
    SafeModeAction.Option(
        SafeModeRecoveryOption.ClearPlaybackQueue,
        Res.string.diagnostics_clear_queue,
    ),
    SafeModeAction.Option(
        SafeModeRecoveryOption.ResetAudio,
        Res.string.diagnostics_reset_audio,
    ),
    SafeModeAction.Option(
        SafeModeRecoveryOption.DisableAutomaticScan,
        Res.string.diagnostics_disable_scan,
    ),
    SafeModeAction.Option(
        SafeModeRecoveryOption.DisableRemoteSources,
        Res.string.diagnostics_disable_remote,
    ),
    SafeModeAction.Option(
        SafeModeRecoveryOption.RebuildLibraryIndex,
        Res.string.diagnostics_rebuild_library,
    ),
    SafeModeAction.Option(
        SafeModeRecoveryOption.RestoreDefaultSettings,
        Res.string.diagnostics_restore_defaults,
    ),
)

private fun DiagnosticsBootstrapState.safeModeSummary(
    incident: com.github.tidetunes.core.domain.model.DiagnosticIncident?,
): String = buildString {
    appendLine("TideTunes safe mode")
    appendLine("reason=${startupPlan.reason.orEmpty()}")
    appendLine("incidentType=${incident?.type ?: "UNKNOWN"}")
    appendLine("incidentId=${incident?.id.orEmpty()}")
    appendLine("detectedAtEpochMs=${incident?.detectedAtEpochMs ?: 0}")
    appendLine("startupStage=${incident?.startupStage ?: snapshot.previousStartupAttempt?.lastStage}")
    appendLine("occurrenceCount=${incident?.occurrenceCount ?: 0}")
    appendLine("disabledComponents=${startupPlan.disabledComponents.sorted().joinToString(",")}")
    append("No data was uploaded automatically.")
}

private fun Long.toIncidentTime(): String =
    runCatching { Instant.fromEpochMilliseconds(this).toString() }.getOrDefault(toString())
