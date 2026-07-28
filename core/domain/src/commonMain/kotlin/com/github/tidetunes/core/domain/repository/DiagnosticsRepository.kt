package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.DiagnosticExportBundle
import com.github.tidetunes.core.domain.model.DiagnosticExportBundleRequest
import com.github.tidetunes.core.domain.model.DiagnosticFaultInjection
import com.github.tidetunes.core.domain.model.DiagnosticIncident
import com.github.tidetunes.core.domain.model.DiagnosticIncidentDraft
import com.github.tidetunes.core.domain.model.DiagnosticIncidentFilter
import com.github.tidetunes.core.domain.model.DiagnosticIncidentPage
import com.github.tidetunes.core.domain.model.DiagnosticIncidentState
import com.github.tidetunes.core.domain.model.DiagnosticLogCategory
import com.github.tidetunes.core.domain.model.DiagnosticLogFilter
import com.github.tidetunes.core.domain.model.DiagnosticLogLevel
import com.github.tidetunes.core.domain.model.DiagnosticLogPage
import com.github.tidetunes.core.domain.model.DiagnosticLogRetentionPolicy
import com.github.tidetunes.core.domain.model.DiagnosticLogSessionPage
import com.github.tidetunes.core.domain.model.DiagnosticPlatformExit
import com.github.tidetunes.core.domain.model.DiagnosticRuntimeSnapshot
import com.github.tidetunes.core.domain.model.DiagnosticStartupAttempt
import com.github.tidetunes.core.domain.model.DiagnosticStartupStage
import com.github.tidetunes.core.domain.model.DiagnosticStorageBreakdown

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
