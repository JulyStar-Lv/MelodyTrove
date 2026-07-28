package com.github.tidetunes.diagnostics

import com.github.tidetunes.core.domain.model.DiagnosticIncident
import com.github.tidetunes.core.domain.model.DiagnosticIncidentFilter
import com.github.tidetunes.core.domain.model.DiagnosticIncidentState
import com.github.tidetunes.core.domain.model.DiagnosticRuntimeSnapshot
import com.github.tidetunes.core.domain.model.DiagnosticStartupStage
import com.github.tidetunes.core.domain.recovery.IncidentOccurrence
import com.github.tidetunes.core.domain.recovery.SafeModePolicyInput
import com.github.tidetunes.core.domain.recovery.StartupMode
import com.github.tidetunes.core.domain.recovery.StartupPlan
import com.github.tidetunes.core.domain.recovery.StartupRecoveryPlanner
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.platform.getAppBuildInfo
import com.github.tidetunes.platform.getAppCacheDir
import com.github.tidetunes.platform.getAppDocumentDir
import com.github.tidetunes.platform.getAppGitCommitSha
import com.github.tidetunes.platform.getAppVersion
import com.github.tidetunes.platform.getPlatformName
import com.github.tidetunes.platform.getProcessName
import uniffi.tidetunes_backend.DiagnosticsRuntimeInit
import uniffi.tidetunes_backend.initializeDiagnosticsRuntime

data class DiagnosticsBootstrapState(
    val snapshot: DiagnosticRuntimeSnapshot,
    val startupPlan: StartupPlan,
    val pendingIncidents: List<DiagnosticIncident>,
) {
    val safeMode: Boolean
        get() = startupPlan.mode == StartupMode.SafeMode

    fun recoveryIncidentIds(): List<String> = pendingIncidents
        .filter { incident ->
            incident.requiresRecovery || incident.id == startupPlan.primaryIncidentId
        }
        .map { it.id }

    fun beginAutomaticDegradedRecovery(): List<String> {
        if (safeMode || startupPlan.disabledComponents.isEmpty()) return emptyList()
        RustDiagnosticsRepository.beginRecovery(startupPlan.disabledComponents)
        return recoveryIncidentIds().onEach { incidentId ->
            RustDiagnosticsRepository.markRecoveryAttempted(
                incidentId,
                startupPlan.disabledComponents,
            )
        }
    }
}

/**
 * Process-global startup state. Platform entry points call this before creating
 * the full Koin graph, database, backend, player, workers, or Compose content.
 */
object DiagnosticsBootstrap {
    private val planner = StartupRecoveryPlanner()
    private var current: DiagnosticsBootstrapState? = null
    private var userForcedSafeMode: Boolean = false

    val state: DiagnosticsBootstrapState
        get() = checkNotNull(current) { "DiagnosticsBootstrap has not been initialized" }

    fun initialize(userForcedSafeMode: Boolean = false): DiagnosticsBootstrapState {
        current?.let { return it }
        this.userForcedSafeMode = userForcedSafeMode
        initializeDiagnosticsRuntime(
            DiagnosticsRuntimeInit(
                appDocumentDir = getAppDocumentDir(),
                appCacheDir = getAppCacheDir(),
                platform = getPlatformName(),
                appVersion = getAppVersion(),
                buildInfo = getAppBuildInfo(),
                gitCommitSha = getAppGitCommitSha(),
                processName = getProcessName(),
                userForcedSafeMode = userForcedSafeMode,
            )
        )
        return replan()
    }

    /**
     * Called after platform-specific historical exits are imported, before full DI.
     */
    fun finishPlatformExitCollection(): DiagnosticsBootstrapState {
        RustDiagnosticsRepository.updateStartupStage(DiagnosticStartupStage.PlatformExitsCollected)
        return replan()
    }

    private fun replan(): DiagnosticsBootstrapState {
        val snapshot = RustDiagnosticsRepository.snapshot()
        val incidents = RustDiagnosticsRepository.listIncidents(
            DiagnosticIncidentFilter(
                states = unresolvedIncidentStates,
                limit = 500,
            )
        ).incidents
        val pending = snapshot.pendingRecovery
        val previous = snapshot.previousStartupAttempt
        val plan = planner.plan(
            SafeModePolicyInput(
                pendingIncidents = incidents,
                lastStartupAttempt = previous,
                occurrences = incidents.flatMap { incident ->
                    val fingerprint = incident.fingerprint ?: return@flatMap emptyList()
                    val timestamps = incident.occurrenceTimestampsEpochMs.ifEmpty {
                        listOf(incident.lastSeenAtEpochMs)
                    }
                    timestamps.map { timestamp ->
                        IncidentOccurrence(
                            fingerprint = fingerprint,
                            incidentType = incident.type,
                            severity = incident.severity,
                            timestampEpochMs = timestamp,
                            startupStage = incident.startupStage,
                        )
                    }
                },
                currentTimeEpochMs = currentTimeMillis(),
                userForcedSafeMode = userForcedSafeMode ||
                    snapshot.startupAttempt.safeModeReason == "User requested safe mode",
                previousRecoveryFailedAtSameStage =
                    pending?.failedRecoveryAttempts?.let { it > 0 } == true ||
                        (
                            previous?.recoveryAttempted == true &&
                                !previous.stable &&
                                pending?.startupStage == previous.lastStage
                            ),
            )
        )
        RustDiagnosticsRepository.setStartupMode(
            safeMode = plan.mode == StartupMode.SafeMode,
            reason = plan.reason,
            disabledComponents = plan.disabledComponents,
        )
        return DiagnosticsBootstrapState(
            snapshot = RustDiagnosticsRepository.snapshot(),
            startupPlan = plan,
            pendingIncidents = incidents,
        ).also { current = it }
    }

    private val unresolvedIncidentStates = setOf(
        DiagnosticIncidentState.Detected,
        DiagnosticIncidentState.PendingReview,
        DiagnosticIncidentState.Acknowledged,
        DiagnosticIncidentState.Exported,
        DiagnosticIncidentState.RecoveryAttempted,
    )
}
