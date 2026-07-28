package com.github.tidetunes.core.domain.recovery

import com.github.tidetunes.core.domain.model.DiagnosticIncident
import com.github.tidetunes.core.domain.model.DiagnosticIncidentSeverity
import com.github.tidetunes.core.domain.model.DiagnosticIncidentState
import com.github.tidetunes.core.domain.model.DiagnosticIncidentType
import com.github.tidetunes.core.domain.model.DiagnosticStartupStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafeModePolicyTest {
    private val policy = SafeModePolicy()
    private val now = 1_000_000L

    @Test
    fun startupKotlinFatalEntersSafeMode() {
        val result = policy.decide(
            input(incident(type = DiagnosticIncidentType.KotlinUncaught)),
        )
        assertEquals(StartupMode.SafeMode, result.mode)
        assertTrue("application_backend" in result.disabledComponents)
    }

    @Test
    fun singleStableAnrOnlyWarns() {
        val result = policy.decide(
            input(
                incident(
                    type = DiagnosticIncidentType.AndroidAnr,
                    severity = DiagnosticIncidentSeverity.Error,
                    stage = DiagnosticStartupStage.StartupStable,
                    requiresRecovery = false,
                ),
            ),
        )
        assertEquals(StartupMode.NormalStartupWithWarning, result.mode)
    }

    @Test
    fun singleStableFatalOnlyWarns() {
        val result = policy.decide(
            input(
                incident(
                    type = DiagnosticIncidentType.KotlinUncaught,
                    stage = DiagnosticStartupStage.StartupStable,
                    requiresRecovery = false,
                ),
            ),
        )

        assertEquals(StartupMode.NormalStartupWithWarning, result.mode)
    }

    @Test
    fun repeatedAnrEntersSafeMode() {
        val repeated = incident(
            type = DiagnosticIncidentType.AndroidAnr,
            severity = DiagnosticIncidentSeverity.Warning,
            stage = DiagnosticStartupStage.StartupStable,
        ).copy(occurrenceCount = 2)
        assertEquals(StartupMode.SafeMode, policy.decide(input(repeated)).mode)
    }

    @Test
    fun repeatedFatalWithinTenMinutesEntersSafeMode() {
        val incident = incident(
            type = DiagnosticIncidentType.PlaybackBackendFailure,
            severity = DiagnosticIncidentSeverity.Fatal,
            stage = DiagnosticStartupStage.StartupStable,
            fingerprint = "same",
        )
        val result = policy.decide(
            SafeModePolicyInput(
                pendingIncidents = listOf(incident),
                lastStartupAttempt = null,
                occurrences = listOf(
                    occurrence("same", now - 1_000),
                    occurrence("same", now - 2_000),
                ),
                currentTimeEpochMs = now,
            ),
        )
        assertEquals(StartupMode.SafeMode, result.mode)
    }

    @Test
    fun threeMatchingFatalsWithinTwentyFourHoursEnterSafeMode() {
        val matching = incident(
            type = DiagnosticIncidentType.PlaybackBackendFailure,
            severity = DiagnosticIncidentSeverity.Fatal,
            stage = DiagnosticStartupStage.StartupStable,
            fingerprint = "long-window",
        )
        val result = policy.decide(
            SafeModePolicyInput(
                pendingIncidents = listOf(matching),
                lastStartupAttempt = null,
                occurrences = listOf(
                    occurrence("long-window", now - 11 * 60_000),
                    occurrence("long-window", now - 12 * 60_000),
                    occurrence("long-window", now - 13 * 60_000),
                ),
                currentTimeEpochMs = now,
            ),
        )

        assertEquals(StartupMode.SafeMode, result.mode)
    }

    @Test
    fun userRequestAlwaysEntersSafeMode() {
        val result = policy.decide(
            SafeModePolicyInput(
                pendingIncidents = emptyList(),
                lastStartupAttempt = null,
                occurrences = emptyList(),
                currentTimeEpochMs = now,
                userForcedSafeMode = true,
            ),
        )

        assertEquals(StartupMode.SafeMode, result.mode)
    }

    @Test
    fun failedRecoveryAtSameStageEntersSafeMode() {
        val result = policy.decide(
            SafeModePolicyInput(
                pendingIncidents = listOf(
                    incident(type = DiagnosticIncidentType.PlaybackBackendFailure),
                ),
                lastStartupAttempt = null,
                occurrences = emptyList(),
                currentTimeEpochMs = now,
                previousRecoveryFailedAtSameStage = true,
            ),
        )

        assertEquals(StartupMode.SafeMode, result.mode)
    }

    @Test
    fun pluginBootFailureUsesLocalDegradation() {
        val result = policy.decide(
            input(incident(type = DiagnosticIncidentType.PluginBootFailure)),
        )
        assertEquals(StartupMode.NormalStartupWithPluginsDisabled, result.mode)
        assertEquals(setOf("third_party_plugins"), result.disabledComponents)
    }

    @Test
    fun automaticScanFailureUsesLocalDegradation() {
        val result = policy.decide(
            input(
                incident(
                    type = DiagnosticIncidentType.StartupFailure,
                    stage = DiagnosticStartupStage.SourceTasksScheduling,
                ),
            ),
        )

        assertEquals(StartupMode.NormalStartupWithAutoScanDisabled, result.mode)
        assertEquals(
            setOf("automatic_scan", "background_sync"),
            result.disabledComponents,
        )
    }

    @Test
    fun playbackRestoreFailureSkipsPreviousQueue() {
        val result = policy.decide(
            input(
                incident(
                    type = DiagnosticIncidentType.StartupFailure,
                    stage = DiagnosticStartupStage.PlaybackRestoring,
                ),
            ),
        )

        assertEquals(StartupMode.NormalStartupWithWarning, result.mode)
        assertEquals(setOf("playback_restore"), result.disabledComponents)
    }

    @Test
    fun repeatedUnknownExitDuringStartupEntersSafeMode() {
        val repeated = incident(
            type = DiagnosticIncidentType.UnknownAbnormalExit,
            severity = DiagnosticIncidentSeverity.Warning,
            stage = DiagnosticStartupStage.DatabaseOpening,
            requiresRecovery = false,
        ).copy(occurrenceCount = 2)

        assertEquals(StartupMode.SafeMode, policy.decide(input(repeated)).mode)
    }

    @Test
    fun databaseFailureAlwaysEntersSafeMode() {
        val result = policy.decide(
            input(incident(type = DiagnosticIncidentType.DatabaseOpenFailure)),
        )
        assertEquals(StartupMode.SafeMode, result.mode)
    }

    private fun input(vararg incidents: DiagnosticIncident) = SafeModePolicyInput(
        pendingIncidents = incidents.toList(),
        lastStartupAttempt = null,
        occurrences = emptyList(),
        currentTimeEpochMs = now,
    )

    private fun occurrence(fingerprint: String, timestamp: Long) = IncidentOccurrence(
        fingerprint = fingerprint,
        incidentType = DiagnosticIncidentType.PlaybackBackendFailure,
        severity = DiagnosticIncidentSeverity.Fatal,
        timestampEpochMs = timestamp,
        startupStage = DiagnosticStartupStage.StartupStable,
    )

    private fun incident(
        type: DiagnosticIncidentType,
        severity: DiagnosticIncidentSeverity = DiagnosticIncidentSeverity.Fatal,
        stage: DiagnosticStartupStage = DiagnosticStartupStage.BackendCreating,
        fingerprint: String = type.name,
        requiresRecovery: Boolean = true,
    ) = DiagnosticIncident(
        id = "incident-${type.name}",
        type = type,
        severity = severity,
        state = DiagnosticIncidentState.PendingReview,
        detectedAtEpochMs = now,
        lastSeenAtEpochMs = now,
        processName = "test",
        sessionId = "session",
        startupAttemptId = "attempt",
        startupStage = stage,
        fingerprint = fingerprint,
        summary = type.name,
        detail = null,
        artifactPaths = emptyList(),
        relatedLogSessionIds = listOf("session"),
        occurrenceCount = 1,
        occurrenceTimestampsEpochMs = listOf(now),
        requiresRecovery = requiresRecovery,
    )
}
