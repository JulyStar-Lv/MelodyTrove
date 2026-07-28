package io.github.julystar.musicapp.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncident
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentState
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentSeverity
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentType
import io.github.julystar.musicapp.core.domain.model.DiagnosticFaultInjection
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogCategory
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogEntry
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogLevel
import io.github.julystar.musicapp.core.presentation.components.DesignDialog
import io.github.julystar.musicapp.core.presentation.components.DesignPreferenceRow
import io.github.julystar.musicapp.core.presentation.components.DesignSearchBar
import io.github.julystar.musicapp.core.presentation.components.DesignSettingsGroup
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import musicapp.feature.settings.generated.resources.Res
import musicapp.feature.settings.generated.resources.diagnostics_acknowledge
import musicapp.feature.settings.generated.resources.diagnostics_artifact
import musicapp.feature.settings.generated.resources.diagnostics_category
import musicapp.feature.settings.generated.resources.diagnostics_clear_all
import musicapp.feature.settings.generated.resources.diagnostics_clear_exports
import musicapp.feature.settings.generated.resources.diagnostics_clear_selected
import musicapp.feature.settings.generated.resources.diagnostics_confirm_delete
import musicapp.feature.settings.generated.resources.diagnostics_confirm_delete_message
import musicapp.feature.settings.generated.resources.diagnostics_copy_log
import musicapp.feature.settings.generated.resources.diagnostics_copy_path
import musicapp.feature.settings.generated.resources.diagnostics_copy_incident
import musicapp.feature.settings.generated.resources.diagnostics_current_attempt
import musicapp.feature.settings.generated.resources.diagnostics_delete
import musicapp.feature.settings.generated.resources.diagnostics_delete_resolved
import musicapp.feature.settings.generated.resources.diagnostics_disabled_components
import musicapp.feature.settings.generated.resources.diagnostics_export
import musicapp.feature.settings.generated.resources.diagnostics_export_incident
import musicapp.feature.settings.generated.resources.diagnostics_enforce_retention
import musicapp.feature.settings.generated.resources.diagnostics_fault_injection
import musicapp.feature.settings.generated.resources.diagnostics_fault_warning
import musicapp.feature.settings.generated.resources.diagnostics_history
import musicapp.feature.settings.generated.resources.diagnostics_incidents
import musicapp.feature.settings.generated.resources.diagnostics_incident_type
import musicapp.feature.settings.generated.resources.diagnostics_level
import musicapp.feature.settings.generated.resources.diagnostics_load_more
import musicapp.feature.settings.generated.resources.diagnostics_logs
import musicapp.feature.settings.generated.resources.diagnostics_refresh
import musicapp.feature.settings.generated.resources.diagnostics_reveal
import musicapp.feature.settings.generated.resources.diagnostics_request_safe_mode
import musicapp.feature.settings.generated.resources.diagnostics_retention
import musicapp.feature.settings.generated.resources.diagnostics_sessions
import musicapp.feature.settings.generated.resources.diagnostics_save_as
import musicapp.feature.settings.generated.resources.diagnostics_severity
import musicapp.feature.settings.generated.resources.diagnostics_state
import musicapp.feature.settings.generated.resources.diagnostics_startup
import musicapp.feature.settings.generated.resources.diagnostics_storage
import musicapp.feature.settings.generated.resources.diagnostics_time_range
import musicapp.feature.settings.generated.resources.diagnostics_title
import musicapp.feature.settings.generated.resources.settings_cancel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Suppress("DEPRECATION")
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var confirmation by remember { mutableStateOf<DiagnosticsConfirmation?>(null) }

    SettingsPageLayout(
        title = stringResource(Res.string.diagnostics_title),
        onBack = onBack,
    ) {
        DiagnosticsActions(
            state = state,
            onRefresh = viewModel::refresh,
            onExport = viewModel::exportAndShare,
            onSave = viewModel::saveLastExport,
            onReveal = viewModel::revealLastExport,
            onCopyPath = viewModel::copyLastExportPath,
            onClearExports = { confirmation = DiagnosticsConfirmation.ClearExports },
            onEnforceRetention = { confirmation = DiagnosticsConfirmation.EnforceRetention },
            onRequestSafeMode = viewModel::requestSafeModeNextStart,
        )
        DiagnosticsStartupSection(state)
        DiagnosticsLogSection(
            state = state,
            onKeywordChange = viewModel::setKeyword,
            onSearch = viewModel::applyKeyword,
            onSession = viewModel::selectSession,
            onLevel = viewModel::setLogLevel,
            onCategory = viewModel::setLogCategory,
            onWindow = viewModel::setLogWindow,
            onCopyLog = { clipboard.setText(AnnotatedString(it.copyText())) },
            onLoadMoreSessions = viewModel::loadMoreSessions,
            onLoadMoreLogs = viewModel::loadMoreLogs,
            onClearSelected = { confirmation = DiagnosticsConfirmation.ClearSelected },
            onClearAll = { confirmation = DiagnosticsConfirmation.ClearAll },
            onRetention = viewModel::setRetentionDays,
        )
        DiagnosticsIncidentSection(
            state = state,
            onAcknowledge = viewModel::acknowledgeIncident,
            onType = viewModel::setIncidentType,
            onSeverity = viewModel::setIncidentSeverity,
            onState = viewModel::setIncidentState,
            onCopy = { clipboard.setText(AnnotatedString(it.copySummary())) },
            onExport = viewModel::exportIncidentAndShare,
            onReadArtifact = viewModel::readArtifact,
            onDelete = { incident ->
                confirmation = DiagnosticsConfirmation.DeleteIncident(incident)
            },
            onDeleteResolved = {
                confirmation = DiagnosticsConfirmation.DeleteResolved
            },
            onLoadMore = viewModel::loadMoreIncidents,
        )
        if (state.faultInjectionSupported) {
            SettingsSection(title = stringResource(Res.string.diagnostics_fault_injection)) {
                DiagnosticFaultInjection.entries.forEach { fault ->
                    DesignPreferenceRow(
                        title = fault.name,
                        summary = stringResource(Res.string.diagnostics_fault_warning),
                        onClick = {
                            confirmation = DiagnosticsConfirmation.DebugFault(fault)
                        },
                    )
                }
            }
        }
    }

    val pendingConfirmation = confirmation
    DesignDialog(
        show = pendingConfirmation != null,
        onDismiss = { confirmation = null },
    ) {
        Text(
            text = if (pendingConfirmation is DiagnosticsConfirmation.DebugFault) {
                stringResource(Res.string.diagnostics_fault_injection)
            } else {
                stringResource(Res.string.diagnostics_confirm_delete)
            },
            style = MiuixTheme.textStyles.title3,
        )
        Text(
            text = if (pendingConfirmation is DiagnosticsConfirmation.DebugFault) {
                stringResource(Res.string.diagnostics_fault_warning)
            } else {
                stringResource(Res.string.diagnostics_confirm_delete_message)
            },
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            ActionButton(
                text = stringResource(Res.string.settings_cancel),
                onClick = { confirmation = null },
            )
            ActionButton(
                text = stringResource(Res.string.diagnostics_delete),
                variant = DesignTextButtonVariant.Error,
                onClick = {
                    when (pendingConfirmation) {
                        DiagnosticsConfirmation.ClearSelected -> viewModel.clearSelectedSession()
                        DiagnosticsConfirmation.ClearAll -> viewModel.clearAllLogs()
                        DiagnosticsConfirmation.ClearExports -> viewModel.clearExports()
                        DiagnosticsConfirmation.EnforceRetention -> viewModel.enforceRetention()
                        DiagnosticsConfirmation.DeleteResolved -> viewModel.deleteResolvedIncidents()
                        is DiagnosticsConfirmation.DeleteIncident -> {
                            val incident = pendingConfirmation.incident
                            viewModel.deleteIncident(
                                incidentId = incident.id,
                                allowUnresolved = incident.state !in resolvedStates,
                            )
                        }
                        is DiagnosticsConfirmation.DebugFault ->
                            viewModel.triggerDebugFault(pendingConfirmation.fault)
                        null -> Unit
                    }
                    confirmation = null
                },
            )
        }
    }

    DesignDialog(
        show = state.artifactText != null,
        onDismiss = viewModel::clearArtifact,
    ) {
        Text(
            text = state.artifactText.orEmpty(),
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun DiagnosticsActions(
    state: DiagnosticsUiState,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit,
    onReveal: () -> Unit,
    onCopyPath: () -> Unit,
    onClearExports: () -> Unit,
    onEnforceRetention: () -> Unit,
    onRequestSafeMode: () -> Unit,
) {
    DesignSettingsGroup(title = null) {
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_refresh),
            summary = state.error ?: state.status,
            onClick = onRefresh,
        )
        state.snapshot?.previousStartupAttempt?.let { previous ->
            DesignPreferenceRow(
                title = "Previous attempt",
                summary = "${previous.lastStage} · stable=${previous.stable} · " +
                    "graceful=${previous.gracefulShutdown}",
            )
        }
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_history),
            summary = "${state.startupHistory.size}",
        )
        state.startupHistory.take(10).forEach { attempt ->
            DesignPreferenceRow(
                title = "${attempt.lastStage} · ${attempt.attemptId}",
                summary = "safeMode=${attempt.safeMode} · stable=${attempt.stable} · " +
                    "graceful=${attempt.gracefulShutdown}",
            )
        }
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_export),
            summary = state.storage?.let {
                "${formatBytes(it.totalBytes)} · ${it.totalBytes} B"
            },
            onClick = onExport,
            showDivider = false,
        )
        if (state.lastExportPath != null) {
            DesignPreferenceRow(
                title = stringResource(Res.string.diagnostics_save_as),
                summary = state.lastExportPath,
                onClick = onSave,
            )
            DesignPreferenceRow(
                title = stringResource(Res.string.diagnostics_reveal),
                onClick = onReveal,
            )
            DesignPreferenceRow(
                title = stringResource(Res.string.diagnostics_copy_path),
                onClick = onCopyPath,
                showDivider = false,
            )
        }
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_clear_exports),
            summary = state.storage?.let { formatBytes(it.exportBytes) },
            onClick = onClearExports,
        )
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_enforce_retention),
            summary = "${state.retention.retentionDays} d · " +
                "${formatBytes(state.retention.maxTotalBytes)}",
            onClick = onEnforceRetention,
            showDivider = false,
        )
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_request_safe_mode),
            onClick = onRequestSafeMode,
            showDivider = false,
        )
    }
}

@Composable
private fun DiagnosticsStartupSection(state: DiagnosticsUiState) {
    val snapshot = state.snapshot
    SettingsSection(title = stringResource(Res.string.diagnostics_startup)) {
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_current_attempt),
            summary = snapshot?.startupAttempt?.let {
                "${it.attemptId} · ${it.lastStage} · safeMode=${it.safeMode}"
            } ?: "—",
        )
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_disabled_components),
            summary = snapshot?.startupAttempt?.disabledComponents?.joinToString().orEmpty()
                .ifBlank { "—" },
        )
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_storage),
            summary = state.storage?.let {
                "logs=${formatBytes(it.logBytes)}, incidents=${formatBytes(it.incidentBytes)}, " +
                    "startup=${formatBytes(it.startupBytes)}, exports=${formatBytes(it.exportBytes)}"
            } ?: "—",
            showDivider = false,
        )
    }
}

@Composable
private fun DiagnosticsLogSection(
    state: DiagnosticsUiState,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSession: (String) -> Unit,
    onLevel: (DiagnosticLogLevel?) -> Unit,
    onCategory: (DiagnosticLogCategory?) -> Unit,
    onWindow: (Long?) -> Unit,
    onCopyLog: (DiagnosticLogEntry) -> Unit,
    onLoadMoreSessions: () -> Unit,
    onLoadMoreLogs: () -> Unit,
    onClearSelected: () -> Unit,
    onClearAll: () -> Unit,
    onRetention: (Long) -> Unit,
) {
    SettingsSection(title = stringResource(Res.string.diagnostics_logs)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignSearchBar(
                value = state.logKeyword,
                onValueChange = onKeywordChange,
                placeholder = stringResource(Res.string.diagnostics_logs),
                onSearch = onSearch,
                onClear = {
                    onKeywordChange("")
                    onSearch()
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionButton(
                    text = "${stringResource(Res.string.diagnostics_level)}: " +
                        (state.logLevel?.name ?: "ALL"),
                    onClick = { onLevel(state.logLevel.nextOrNull()) },
                )
                ActionButton(
                    text = "${stringResource(Res.string.diagnostics_category)}: " +
                        (state.logCategory?.name ?: "ALL"),
                    onClick = { onCategory(state.logCategory.nextOrNull()) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Long?>(null, 60L * 60 * 1_000, 24L * 60 * 60 * 1_000).forEach { window ->
                    ActionButton(
                        text = "${stringResource(Res.string.diagnostics_time_range)}: " +
                            when (window) {
                                null -> "ALL"
                                60L * 60 * 1_000 -> "1h"
                                else -> "24h"
                            },
                        onClick = { onWindow(window) },
                        variant = if (state.logWindowMs == window) {
                            DesignTextButtonVariant.Primary
                        } else {
                            DesignTextButtonVariant.Default
                        },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = stringResource(Res.string.diagnostics_clear_selected),
                    onClick = onClearSelected,
                )
                ActionButton(
                    text = stringResource(Res.string.diagnostics_clear_all),
                    variant = DesignTextButtonVariant.Error,
                    onClick = onClearAll,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1L, 7L, 30L).forEach { days ->
                    ActionButton(
                        text = "${stringResource(Res.string.diagnostics_retention)} $days d",
                        onClick = { onRetention(days) },
                    )
                }
            }
        }
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_sessions),
            summary = "${state.sessions.size}",
        )
        state.sessions.forEach { session ->
            DesignPreferenceRow(
                title = if (session.current) "● ${session.sessionId}" else session.sessionId,
                summary = "${session.platform} · ${session.appVersion} · ${formatBytes(session.logBytes)}",
                onClick = { onSession(session.sessionId) },
            )
        }
        if (state.sessionHasMore) {
            DesignPreferenceRow(
                title = stringResource(Res.string.diagnostics_load_more),
                onClick = onLoadMoreSessions,
            )
        }
        state.logWarnings.forEach { warning ->
            DesignPreferenceRow(title = "Warning", summary = warning)
        }
        state.logEntries.forEach { entry ->
            DesignPreferenceRow(
                title = "${entry.level} · ${entry.category} · ${entry.message}",
                summary = "${entry.timestampEpochMs} · ${entry.target}\n${entry.detail.orEmpty()}",
                onClick = { onCopyLog(entry) },
            )
        }
        if (state.logHasMore) {
            DesignPreferenceRow(
                title = stringResource(Res.string.diagnostics_load_more),
                onClick = onLoadMoreLogs,
                showDivider = false,
            )
        } else {
            DesignPreferenceRow(
                title = stringResource(Res.string.diagnostics_copy_log),
                summary = "Tap a log entry to copy it",
                showDivider = false,
            )
        }
    }
}

@Composable
private fun DiagnosticsIncidentSection(
    state: DiagnosticsUiState,
    onAcknowledge: (String) -> Unit,
    onType: (DiagnosticIncidentType?) -> Unit,
    onSeverity: (DiagnosticIncidentSeverity?) -> Unit,
    onState: (DiagnosticIncidentState?) -> Unit,
    onCopy: (DiagnosticIncident) -> Unit,
    onExport: (DiagnosticIncident) -> Unit,
    onReadArtifact: (String, String) -> Unit,
    onDelete: (DiagnosticIncident) -> Unit,
    onDeleteResolved: () -> Unit,
    onLoadMore: () -> Unit,
) {
    SettingsSection(title = stringResource(Res.string.diagnostics_incidents)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                text = "${stringResource(Res.string.diagnostics_incident_type)}: " +
                    (state.incidentType?.name ?: "ALL"),
                onClick = { onType(state.incidentType.nextOrNull()) },
            )
            ActionButton(
                text = "${stringResource(Res.string.diagnostics_severity)}: " +
                    (state.incidentSeverity?.name ?: "ALL"),
                onClick = { onSeverity(state.incidentSeverity.nextOrNull()) },
            )
            ActionButton(
                text = "${stringResource(Res.string.diagnostics_state)}: " +
                    (state.incidentState?.name ?: "ALL"),
                onClick = { onState(state.incidentState.nextOrNull()) },
            )
        }
        state.incidents.forEach { incident ->
            DesignPreferenceRow(
                title = "${incident.severity} · ${incident.type} · ×${incident.occurrenceCount}",
                summary = "${incident.state} · recovery=${incident.requiresRecovery}\n${incident.summary}",
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = stringResource(Res.string.diagnostics_acknowledge),
                        onClick = { onAcknowledge(incident.id) },
                    )
                    ActionButton(
                        text = stringResource(Res.string.diagnostics_copy_incident),
                        onClick = { onCopy(incident) },
                    )
                    ActionButton(
                        text = stringResource(Res.string.diagnostics_export_incident),
                        onClick = { onExport(incident) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    incident.artifactPaths.firstOrNull()?.let { path ->
                        ActionButton(
                            text = stringResource(Res.string.diagnostics_artifact),
                            onClick = { onReadArtifact(incident.id, path) },
                        )
                    }
                    ActionButton(
                        text = stringResource(Res.string.diagnostics_delete),
                        variant = DesignTextButtonVariant.Error,
                        onClick = { onDelete(incident) },
                    )
                }
            }
        }
        if (state.incidentHasMore) {
            DesignPreferenceRow(
                title = stringResource(Res.string.diagnostics_load_more),
                onClick = onLoadMore,
            )
        }
        DesignPreferenceRow(
            title = stringResource(Res.string.diagnostics_delete_resolved),
            onClick = onDeleteResolved,
            showDivider = false,
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    variant: DesignTextButtonVariant = DesignTextButtonVariant.Primary,
) {
    DesignTextButton(
        text = text,
        variant = variant,
        size = DesignTextButtonSize.Small,
        onClick = onClick,
    )
}

private inline fun <reified T : Enum<T>> T?.nextOrNull(): T? {
    val values = enumValues<T>()
    return if (this == null) values.firstOrNull() else values.getOrNull(ordinal + 1)
}

private fun DiagnosticLogEntry.copyText(): String = buildString {
    appendLine("timestamp=$timestampEpochMs")
    appendLine("level=$level category=$category")
    appendLine("target=$target")
    appendLine("message=$message")
    detail?.let { appendLine("detail=$it") }
    correlationId?.let { appendLine("correlationId=$it") }
    append("fields=$fields")
}

private fun DiagnosticIncident.copySummary(): String = buildString {
    appendLine("incidentId=$id")
    appendLine("type=$type severity=$severity state=$state")
    appendLine("detectedAtEpochMs=$detectedAtEpochMs")
    appendLine("startupStage=${startupStage ?: "UNKNOWN"}")
    appendLine("occurrenceCount=$occurrenceCount")
    appendLine("summary=$summary")
    detail?.let { append("detail=$it") }
}

private sealed interface DiagnosticsConfirmation {
    data object ClearSelected : DiagnosticsConfirmation
    data object ClearAll : DiagnosticsConfirmation
    data object ClearExports : DiagnosticsConfirmation
    data object EnforceRetention : DiagnosticsConfirmation
    data object DeleteResolved : DiagnosticsConfirmation
    data class DeleteIncident(val incident: DiagnosticIncident) : DiagnosticsConfirmation
    data class DebugFault(val fault: DiagnosticFaultInjection) : DiagnosticsConfirmation
}

private val resolvedStates = setOf(
    DiagnosticIncidentState.Resolved,
    DiagnosticIncidentState.Ignored,
)
