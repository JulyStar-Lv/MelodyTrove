package com.github.tidetunes.diagnostics

import com.github.tidetunes.core.domain.model.DiagnosticExportBundle
import com.github.tidetunes.core.domain.model.DiagnosticExportBundleRequest
import com.github.tidetunes.core.domain.model.DiagnosticFaultInjection
import com.github.tidetunes.core.domain.model.DiagnosticIncident
import com.github.tidetunes.core.domain.model.DiagnosticIncidentDraft
import com.github.tidetunes.core.domain.model.DiagnosticIncidentFilter
import com.github.tidetunes.core.domain.model.DiagnosticIncidentPage
import com.github.tidetunes.core.domain.model.DiagnosticIncidentSeverity
import com.github.tidetunes.core.domain.model.DiagnosticIncidentState
import com.github.tidetunes.core.domain.model.DiagnosticIncidentType
import com.github.tidetunes.core.domain.model.DiagnosticLogCategory
import com.github.tidetunes.core.domain.model.DiagnosticLogEntry
import com.github.tidetunes.core.domain.model.DiagnosticLogFilter
import com.github.tidetunes.core.domain.model.DiagnosticLogLevel
import com.github.tidetunes.core.domain.model.DiagnosticLogPage
import com.github.tidetunes.core.domain.model.DiagnosticLogRetentionPolicy
import com.github.tidetunes.core.domain.model.DiagnosticLogSession
import com.github.tidetunes.core.domain.model.DiagnosticLogSessionPage
import com.github.tidetunes.core.domain.model.DiagnosticPendingRecovery
import com.github.tidetunes.core.domain.model.DiagnosticPlatformExit
import com.github.tidetunes.core.domain.model.DiagnosticRuntimeSnapshot
import com.github.tidetunes.core.domain.model.DiagnosticStartupAttempt
import com.github.tidetunes.core.domain.model.DiagnosticStartupStage
import com.github.tidetunes.core.domain.model.DiagnosticStorageBreakdown
import com.github.tidetunes.core.domain.repository.DiagnosticsRepository
import uniffi.tidetunes_backend.DiagnosticExportRequest
import uniffi.tidetunes_backend.DiagnosticLogEvent
import uniffi.tidetunes_backend.IncidentDraft
import uniffi.tidetunes_backend.IncidentFilter
import uniffi.tidetunes_backend.PlatformExitRecord
import uniffi.tidetunes_backend.clearAllLogs as nativeClearAllLogs
import uniffi.tidetunes_backend.beginDiagnosticsRecovery
import uniffi.tidetunes_backend.clearDiagnosticExports
import uniffi.tidetunes_backend.clearLogSessions as nativeClearLogSessions
import uniffi.tidetunes_backend.completeDiagnosticsRecovery
import uniffi.tidetunes_backend.deleteDiagnosticIncident
import uniffi.tidetunes_backend.deleteResolvedIncidents
import uniffi.tidetunes_backend.debugTriggerRustPanic
import uniffi.tidetunes_backend.debugMarkStartupIncomplete
import uniffi.tidetunes_backend.diagnosticsDebugFaultInjectionSupported
import uniffi.tidetunes_backend.enforceLogRetentionPolicy as nativeEnforceLogRetentionPolicy
import uniffi.tidetunes_backend.exportDiagnosticsBundle
import uniffi.tidetunes_backend.flushDiagnostics as nativeFlushDiagnostics
import uniffi.tidetunes_backend.getDiagnosticStorageUsage
import uniffi.tidetunes_backend.getDiagnosticsRuntimeState
import uniffi.tidetunes_backend.getLogRetentionPolicy as nativeGetLogRetentionPolicy
import uniffi.tidetunes_backend.importPlatformExit as nativeImportPlatformExit
import uniffi.tidetunes_backend.listDiagnosticIncidents
import uniffi.tidetunes_backend.listLogSessions as nativeListLogSessions
import uniffi.tidetunes_backend.listStartupHistory
import uniffi.tidetunes_backend.logDiagnosticEvent
import uniffi.tidetunes_backend.markRecoveryAttempted as nativeMarkRecoveryAttempted
import uniffi.tidetunes_backend.readIncidentArtifact as nativeReadIncidentArtifact
import uniffi.tidetunes_backend.readLogEntries as nativeReadLogEntries
import uniffi.tidetunes_backend.readLogTail as nativeReadLogTail
import uniffi.tidetunes_backend.recordDiagnosticIncident as nativeRecordDiagnosticIncident
import uniffi.tidetunes_backend.recordFatalIncident as nativeRecordFatalIncident
import uniffi.tidetunes_backend.releaseDiagnosticExport as nativeReleaseDiagnosticExport
import uniffi.tidetunes_backend.requestSafeModeNextStart as nativeRequestSafeModeNextStart
import uniffi.tidetunes_backend.setDiagnosticsStartupMode
import uniffi.tidetunes_backend.setDiagnosticsMusicRoots
import uniffi.tidetunes_backend.setIncidentState as nativeSetIncidentState
import uniffi.tidetunes_backend.setLogRetentionPolicy as nativeSetLogRetentionPolicy
import uniffi.tidetunes_backend.shutdownDiagnosticsRuntime as nativeShutdownDiagnosticsRuntime
import uniffi.tidetunes_backend.updateStartupStage as nativeUpdateStartupStage

/**
 * The only application-facing adapter for generated diagnostics bindings.
 * It intentionally has no database, Koin, player, network, or plugin dependency.
 */
object RustDiagnosticsRepository : DiagnosticsRepository {
    override fun snapshot(): DiagnosticRuntimeSnapshot =
        getDiagnosticsRuntimeState().toDomain()

    override fun log(
        level: DiagnosticLogLevel,
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        detail: String?,
        correlationId: String?,
        fields: Map<String, String>,
    ) {
        logDiagnosticEvent(
            DiagnosticLogEvent(
                level = level.toNative(),
                category = category.toNative(),
                target = target,
                message = message,
                detail = detail,
                correlationId = correlationId,
                fields = fields,
            )
        )
    }

    override fun listLogSessions(offset: Long, limit: Long): DiagnosticLogSessionPage =
        nativeListLogSessions(offset, limit).let { page ->
            DiagnosticLogSessionPage(
                sessions = page.sessions.map { it.toDomain() },
                offset = page.offset,
                limit = page.limit,
                total = page.total,
                hasMore = page.hasMore,
            )
        }

    override fun readLogEntries(filter: DiagnosticLogFilter): DiagnosticLogPage =
        nativeReadLogEntries(filter.toNative()).toDomain()

    override fun readLogTail(sessionId: String, limit: Long): DiagnosticLogPage =
        nativeReadLogTail(sessionId, limit).toDomain()

    override fun clearLogSessions(sessionIds: List<String>): Long = nativeClearLogSessions(sessionIds)

    override fun clearAllLogs(): Long = nativeClearAllLogs()

    override fun getLogRetentionPolicy(): DiagnosticLogRetentionPolicy =
        nativeGetLogRetentionPolicy().toDomain()

    override fun setLogRetentionPolicy(
        policy: DiagnosticLogRetentionPolicy,
    ): DiagnosticLogRetentionPolicy = nativeSetLogRetentionPolicy(policy.toNative()).toDomain()

    override fun enforceLogRetentionPolicy(): Long = nativeEnforceLogRetentionPolicy()

    override fun getStorageUsage(): DiagnosticStorageBreakdown =
        getDiagnosticStorageUsage().let { usage ->
            DiagnosticStorageBreakdown(
                logBytes = usage.logBytes,
                incidentBytes = usage.incidentBytes,
                startupBytes = usage.startupBytes,
                exportBytes = usage.exportBytes,
                totalBytes = usage.totalBytes,
            )
        }

    override fun listIncidents(filter: DiagnosticIncidentFilter): DiagnosticIncidentPage =
        listDiagnosticIncidents(filter.toNative()).let { page ->
            DiagnosticIncidentPage(
                incidents = page.incidents.map { it.toDomain() },
                offset = page.offset,
                limit = page.limit,
                totalMatched = page.totalMatched,
                hasMore = page.hasMore,
            )
        }

    override fun recordIncident(draft: DiagnosticIncidentDraft): DiagnosticIncident =
        nativeRecordDiagnosticIncident(draft.toNative()).toDomain()

    override fun recordFatalIncident(draft: DiagnosticIncidentDraft): DiagnosticIncident? =
        nativeRecordFatalIncident(draft.toNative())?.toDomain()

    override fun importPlatformExit(exit: DiagnosticPlatformExit): DiagnosticIncident? =
        nativeImportPlatformExit(exit.toNative())?.toDomain()

    override fun readIncidentArtifact(incidentId: String, artifactPath: String): String =
        nativeReadIncidentArtifact(incidentId, artifactPath)

    override fun setIncidentState(
        incidentId: String,
        state: DiagnosticIncidentState,
    ): DiagnosticIncident = nativeSetIncidentState(incidentId, state.toNative()).toDomain()

    override fun deleteIncident(incidentId: String, allowUnresolved: Boolean) {
        deleteDiagnosticIncident(incidentId, allowUnresolved)
    }

    override fun deleteResolvedIncidents(): Long = uniffi.tidetunes_backend.deleteResolvedIncidents()

    override fun updateStartupStage(stage: DiagnosticStartupStage): DiagnosticStartupAttempt =
        nativeUpdateStartupStage(stage.toNative()).toDomain()

    override fun setStartupMode(
        safeMode: Boolean,
        reason: String?,
        disabledComponents: Set<String>,
    ): DiagnosticStartupAttempt = setDiagnosticsStartupMode(
        safeMode = safeMode,
        reason = reason,
        disabledComponents = disabledComponents.sorted(),
    ).toDomain()

    override fun startupHistory(limit: Long): List<DiagnosticStartupAttempt> =
        listStartupHistory(limit).map { it.toDomain() }

    override fun beginRecovery(
        disabledComponents: Set<String>,
    ): DiagnosticStartupAttempt =
        beginDiagnosticsRecovery(disabledComponents.sorted()).toDomain()

    override fun markRecoveryAttempted(
        incidentId: String,
        disabledComponents: Set<String>,
    ): DiagnosticIncident = nativeMarkRecoveryAttempted(
        incidentId,
        disabledComponents.sorted(),
    ).toDomain()

    override fun completeRecovery(incidentIds: List<String>) {
        completeDiagnosticsRecovery(incidentIds)
    }

    override fun requestSafeModeNextStart() = nativeRequestSafeModeNextStart()

    override fun export(request: DiagnosticExportBundleRequest): DiagnosticExportBundle =
        exportDiagnosticsBundle(
            DiagnosticExportRequest(
                summary = request.summary,
                environmentJson = request.environmentJson,
                playbackSummaryJson = request.playbackSummaryJson,
                scanSummaryJson = request.scanSummaryJson,
                pluginSummaryJson = request.pluginSummaryJson,
                sourceSummaryJson = request.sourceSummaryJson,
                storageSummaryJson = request.storageSummaryJson,
                includeResolvedIncidents = request.includeResolvedIncidents,
                incidentIds = request.incidentIds.sorted(),
            )
        ).let { result ->
            DiagnosticExportBundle(
                path = result.path,
                fileName = result.fileName,
                bytes = result.bytes,
                includedLogSessions = result.includedLogSessions,
                includedIncidents = result.includedIncidents,
            )
        }

    override fun releaseExport(path: String) = nativeReleaseDiagnosticExport(path)

    override fun clearExports(): Long = clearDiagnosticExports()

    override fun flush() = nativeFlushDiagnostics()

    override fun setMusicRoots(roots: List<String>) = setDiagnosticsMusicRoots(roots)

    override fun shutdown() = nativeShutdownDiagnosticsRuntime()

    override fun debugFaultInjectionSupported(): Boolean =
        platformDebugFaultInjectionSupported() && diagnosticsDebugFaultInjectionSupported()

    override fun triggerDebugFault(fault: DiagnosticFaultInjection) {
        check(debugFaultInjectionSupported()) { "Fault injection is disabled in this build" }
        when (fault) {
            DiagnosticFaultInjection.KotlinUncaught -> triggerPlatformKotlinCrash()
            DiagnosticFaultInjection.RustPanic -> debugTriggerRustPanic()
            DiagnosticFaultInjection.AndroidAnr -> triggerPlatformAnr()
            DiagnosticFaultInjection.IncompleteStartupAttempt -> {
                debugMarkStartupIncomplete()
            }
            DiagnosticFaultInjection.RepeatedFatalIncident -> repeat(3) {
                recordInjectedFatal(
                    DiagnosticIncidentType.StartupFailure,
                    "Repeated debug startup failure",
                )
            }
            DiagnosticFaultInjection.DatabaseOpenFailure -> recordInjectedFatal(
                DiagnosticIncidentType.DatabaseOpenFailure,
                "Debug database open failure",
            )
            DiagnosticFaultInjection.DatabaseMigrationFailure -> recordInjectedFatal(
                DiagnosticIncidentType.DatabaseMigrationFailure,
                "Debug database migration failure",
            )
            DiagnosticFaultInjection.PluginBootFailure -> recordInjectedFatal(
                DiagnosticIncidentType.PluginBootFailure,
                "Debug plugin boot failure",
            )
            DiagnosticFaultInjection.PlaybackBackendFailure -> recordInjectedFatal(
                DiagnosticIncidentType.PlaybackBackendFailure,
                "Debug playback backend failure",
            )
        }
    }

    private fun recordInjectedFatal(type: DiagnosticIncidentType, summary: String) {
        recordFatalIncident(
            DiagnosticIncidentDraft(
                type = type,
                severity = DiagnosticIncidentSeverity.Fatal,
                summary = summary,
                detail = "Created by the local debug fault-injection control.",
                fingerprintMaterial = "tidetunes-debug-${type.name}",
                requiresRecovery = true,
            )
        )
    }
}

private inline fun <reified T : Enum<T>> enumAt(ordinal: Int): T = enumValues<T>()[ordinal]

private fun DiagnosticLogLevel.toNative() =
    enumAt<uniffi.tidetunes_backend.DiagnosticLogLevel>(ordinal)

private fun DiagnosticLogCategory.toNative() =
    enumAt<uniffi.tidetunes_backend.DiagnosticLogCategory>(ordinal)

private fun DiagnosticIncidentType.toNative() =
    enumAt<uniffi.tidetunes_backend.IncidentType>(ordinal)

private fun DiagnosticIncidentSeverity.toNative() =
    enumAt<uniffi.tidetunes_backend.IncidentSeverity>(ordinal)

private fun DiagnosticIncidentState.toNative() =
    enumAt<uniffi.tidetunes_backend.IncidentState>(ordinal)

private fun DiagnosticStartupStage.toNative() =
    enumAt<uniffi.tidetunes_backend.StartupStage>(ordinal)

private fun uniffi.tidetunes_backend.DiagnosticLogLevel.toDomain() =
    enumAt<DiagnosticLogLevel>(ordinal)

private fun uniffi.tidetunes_backend.DiagnosticLogCategory.toDomain() =
    enumAt<DiagnosticLogCategory>(ordinal)

private fun uniffi.tidetunes_backend.IncidentType.toDomain() =
    enumAt<DiagnosticIncidentType>(ordinal)

private fun uniffi.tidetunes_backend.IncidentSeverity.toDomain() =
    enumAt<DiagnosticIncidentSeverity>(ordinal)

private fun uniffi.tidetunes_backend.IncidentState.toDomain() =
    enumAt<DiagnosticIncidentState>(ordinal)

private fun uniffi.tidetunes_backend.StartupStage.toDomain() =
    enumAt<DiagnosticStartupStage>(ordinal)

private fun DiagnosticLogFilter.toNative() = uniffi.tidetunes_backend.DiagnosticLogFilter(
    sessionIds = sessionIds.sorted(),
    levels = levels.sortedBy { it.ordinal }.map { it.toNative() },
    categories = categories.sortedBy { it.ordinal }.map { it.toNative() },
    keyword = keyword,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    correlationId = correlationId,
    offset = offset,
    limit = limit,
)

private fun uniffi.tidetunes_backend.DiagnosticLogPage.toDomain() = DiagnosticLogPage(
    entries = entries.map { it.toDomain() },
    offset = offset,
    limit = limit,
    totalMatched = totalMatched,
    hasMore = hasMore,
    warnings = warnings,
)

private fun uniffi.tidetunes_backend.DiagnosticLogEntry.toDomain() = DiagnosticLogEntry(
    schemaVersion = schemaVersion,
    timestampEpochMs = timestampEpochMs,
    level = level.toDomain(),
    category = category.toDomain(),
    target = target,
    message = message,
    detail = detail,
    sessionId = sessionId,
    correlationId = correlationId,
    startupAttemptId = startupAttemptId,
    thread = thread,
    platform = platform,
    fields = fields,
)

private fun uniffi.tidetunes_backend.DiagnosticLogSession.toDomain() = DiagnosticLogSession(
    sessionId = sessionId,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    platform = platform,
    appVersion = appVersion,
    buildInfo = buildInfo,
    gitCommitSha = gitCommitSha,
    processName = processName,
    startupAttemptId = startupAttemptId,
    gracefulShutdown = gracefulShutdown,
    current = current,
    logPaths = logPaths,
    logBytes = logBytes,
)

private fun DiagnosticLogRetentionPolicy.toNative() =
    uniffi.tidetunes_backend.DiagnosticLogRetentionPolicy(
        retentionDays = retentionDays,
        maxSessions = maxSessions,
        maxTotalBytes = maxTotalBytes,
        maxSessionBytes = maxSessionBytes,
    )

private fun uniffi.tidetunes_backend.DiagnosticLogRetentionPolicy.toDomain() =
    DiagnosticLogRetentionPolicy(
        retentionDays = retentionDays,
        maxSessions = maxSessions,
        maxTotalBytes = maxTotalBytes,
        maxSessionBytes = maxSessionBytes,
    )

private fun DiagnosticIncidentFilter.toNative() = IncidentFilter(
    types = types.sortedBy { it.ordinal }.map { it.toNative() },
    severities = severities.sortedBy { it.ordinal }.map { it.toNative() },
    states = states.sortedBy { it.ordinal }.map { it.toNative() },
    requiresRecovery = requiresRecovery,
    offset = offset,
    limit = limit,
)

private fun DiagnosticIncidentDraft.toNative() = IncidentDraft(
    incidentType = type.toNative(),
    severity = severity.toNative(),
    summary = summary,
    detail = detail,
    fingerprintMaterial = fingerprintMaterial,
    requiresRecovery = requiresRecovery,
)

private fun DiagnosticPlatformExit.toNative() = PlatformExitRecord(
    exitKey = exitKey,
    incidentType = type.toNative(),
    severity = severity.toNative(),
    timestampEpochMs = timestampEpochMs,
    processName = processName,
    pid = pid,
    reason = reason,
    status = status,
    importance = importance,
    pssKb = pssKb,
    rssKb = rssKb,
    description = description,
    trace = trace,
    traceTruncated = traceTruncated,
    requiresRecovery = requiresRecovery,
    environmentSummary = environmentSummary,
    startupAttemptId = startupAttemptId,
    startupStage = startupStage?.toNative(),
)

private fun uniffi.tidetunes_backend.DiagnosticIncident.toDomain() = DiagnosticIncident(
    id = id,
    type = incidentType.toDomain(),
    severity = severity.toDomain(),
    state = state.toDomain(),
    detectedAtEpochMs = detectedAtEpochMs,
    lastSeenAtEpochMs = lastSeenAtEpochMs,
    processName = processName,
    sessionId = sessionId,
    startupAttemptId = startupAttemptId,
    startupStage = startupStage?.toDomain(),
    fingerprint = fingerprint,
    summary = summary,
    detail = detail,
    artifactPaths = artifactPaths,
    relatedLogSessionIds = relatedLogSessionIds,
    occurrenceCount = occurrenceCount,
    occurrenceTimestampsEpochMs = occurrenceTimestampsEpochMs,
    requiresRecovery = requiresRecovery,
)

private fun uniffi.tidetunes_backend.StartupAttempt.toDomain() = DiagnosticStartupAttempt(
    attemptId = attemptId,
    startedAtEpochMs = startedAtEpochMs,
    lastUpdatedAtEpochMs = lastUpdatedAtEpochMs,
    lastStage = lastStage.toDomain(),
    stable = stable,
    gracefulShutdown = gracefulShutdown,
    safeMode = safeMode,
    safeModeReason = safeModeReason,
    recoveryAttempted = recoveryAttempted,
    disabledComponents = disabledComponents,
)

private fun uniffi.tidetunes_backend.PendingRecovery.toDomain() = DiagnosticPendingRecovery(
    incidentId = incidentId,
    fingerprint = fingerprint,
    incidentType = incidentType.toDomain(),
    startupAttemptId = startupAttemptId,
    startupStage = startupStage?.toDomain(),
    occurrenceCount = occurrenceCount,
    failedRecoveryAttempts = failedRecoveryAttempts,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun uniffi.tidetunes_backend.DiagnosticsRuntimeState.toDomain() =
    DiagnosticRuntimeSnapshot(
        diagnosticsRoot = diagnosticsRoot,
        sessionId = sessionId,
        startupAttempt = startupAttempt.toDomain(),
        previousStartupAttempt = previousStartupAttempt?.toDomain(),
        pendingRecovery = pendingRecovery?.toDomain(),
        safeModeSuggested = safeModeSuggested,
        safeModeReason = safeModeReason,
    )
