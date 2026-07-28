package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.DiagnosticExportBundle
import io.github.julystar.musicapp.core.domain.model.DiagnosticExportBundleRequest
import io.github.julystar.musicapp.core.domain.model.DiagnosticFaultInjection
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncident
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentDraft
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentFilter
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentPage
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentState
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogCategory
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogFilter
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogLevel
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogPage
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogRetentionPolicy
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogSessionPage
import io.github.julystar.musicapp.core.domain.model.DiagnosticPlatformExit
import io.github.julystar.musicapp.core.domain.model.DiagnosticRuntimeSnapshot
import io.github.julystar.musicapp.core.domain.model.DiagnosticStartupAttempt
import io.github.julystar.musicapp.core.domain.model.DiagnosticStartupStage
import io.github.julystar.musicapp.core.domain.model.DiagnosticStorageBreakdown

interface DiagnosticsRepository {
    fun snapshot(): DiagnosticRuntimeSnapshot

    fun log(
        level: DiagnosticLogLevel,
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        detail: String? = null,
        correlationId: String? = null,
        fields: Map<String, String> = emptyMap(),
    )

    fun listLogSessions(offset: Long, limit: Long): DiagnosticLogSessionPage
    fun readLogEntries(filter: DiagnosticLogFilter): DiagnosticLogPage
    fun readLogTail(sessionId: String, limit: Long): DiagnosticLogPage
    fun clearLogSessions(sessionIds: List<String>): Long
    fun clearAllLogs(): Long
    fun getLogRetentionPolicy(): DiagnosticLogRetentionPolicy
    fun setLogRetentionPolicy(policy: DiagnosticLogRetentionPolicy): DiagnosticLogRetentionPolicy
    fun enforceLogRetentionPolicy(): Long
    fun getStorageUsage(): DiagnosticStorageBreakdown

    fun listIncidents(filter: DiagnosticIncidentFilter): DiagnosticIncidentPage
    fun recordIncident(draft: DiagnosticIncidentDraft): DiagnosticIncident
    fun recordFatalIncident(draft: DiagnosticIncidentDraft): DiagnosticIncident?
    fun importPlatformExit(exit: DiagnosticPlatformExit): DiagnosticIncident?
    fun readIncidentArtifact(incidentId: String, artifactPath: String): String
    fun setIncidentState(incidentId: String, state: DiagnosticIncidentState): DiagnosticIncident
    fun deleteIncident(incidentId: String, allowUnresolved: Boolean)
    fun deleteResolvedIncidents(): Long

    fun updateStartupStage(stage: DiagnosticStartupStage): DiagnosticStartupAttempt
    fun setStartupMode(
        safeMode: Boolean,
        reason: String?,
        disabledComponents: Set<String>,
    ): DiagnosticStartupAttempt
    fun startupHistory(limit: Long): List<DiagnosticStartupAttempt>
    fun beginRecovery(disabledComponents: Set<String>): DiagnosticStartupAttempt
    fun markRecoveryAttempted(incidentId: String, disabledComponents: Set<String>): DiagnosticIncident
    fun completeRecovery(incidentIds: List<String>)
    fun requestSafeModeNextStart()

    fun export(request: DiagnosticExportBundleRequest): DiagnosticExportBundle
    fun releaseExport(path: String)
    fun clearExports(): Long
    fun flush()
    fun setMusicRoots(roots: List<String>)
    fun shutdown()

    fun debugFaultInjectionSupported(): Boolean
    fun triggerDebugFault(fault: DiagnosticFaultInjection)
}

interface DiagnosticExportPresenter {
    suspend fun share(path: String): Result<Unit>
    suspend fun saveAs(path: String): Result<Unit>
    suspend fun reveal(path: String): Result<Unit>
    suspend fun copyPath(path: String): Result<Unit>
}
