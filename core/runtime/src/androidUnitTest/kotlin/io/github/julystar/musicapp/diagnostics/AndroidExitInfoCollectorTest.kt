package io.github.julystar.musicapp.diagnostics

import android.app.ApplicationExitInfo
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncident
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentSeverity
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentState
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentType
import io.github.julystar.musicapp.core.domain.model.DiagnosticStartupStage
import io.github.julystar.musicapp.core.domain.recovery.SafeModePolicy
import io.github.julystar.musicapp.core.domain.recovery.SafeModePolicyInput
import io.github.julystar.musicapp.core.domain.recovery.StartupMode
import io.github.julystar.musicapp.core.domain.recovery.StartupPlan
import io.github.julystar.musicapp.core.domain.recovery.allowsNormalApplicationInitialization
import io.github.julystar.musicapp.platform.isDiagnosticExportPathAllowed
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidExitInfoCollectorTest {
    @Test
    fun mapsSupportedExitReasons() {
        assertEquals(
            DiagnosticIncidentType.AndroidAnr,
            mapExitReason(ApplicationExitInfo.REASON_ANR)?.first,
        )
        assertEquals(
            DiagnosticIncidentType.NativeCrash,
            mapExitReason(ApplicationExitInfo.REASON_CRASH_NATIVE)?.first,
        )
        assertNull(mapExitReason(ApplicationExitInfo.REASON_USER_REQUESTED))
    }

    @Test
    fun recognizesUserRequestedExitAsIntentional() {
        assertTrue(isUserRequestedExitReason(ApplicationExitInfo.REASON_USER_REQUESTED))
        assertFalse(isUserRequestedExitReason(ApplicationExitInfo.REASON_CRASH))
    }

    @Test
    fun exitKeyIsStable() {
        val first = historicalExitKey(10, 20, 30, 40, "process")
        val second = historicalExitKey(10, 20, 30, 40, "process")
        assertEquals(first, second)
    }

    @Test
    fun traceIsTruncatedAtLimit() {
        val result = readLimitedTrace(ByteArrayInputStream(ByteArray(12) { it.toByte() }), 8)
        assertTrue(result.truncated)
        assertContentEquals(ByteArray(8) { it.toByte() }, result.bytes)
    }

    @Test
    fun preAndroidElevenIsUnsupported() {
        assertFalse(historicalExitInfoSupported(29))
        assertTrue(historicalExitInfoSupported(30))
    }

    @Test
    fun fileProviderOnlyAcceptsGeneratedDiagnosticExports() {
        val root = Files.createTempDirectory("musicapp-provider-").toFile()
        try {
            val export = File(root, "diagnostics/exports/report.zip").apply {
                checkNotNull(parentFile).mkdirs()
                writeBytes(byteArrayOf(1))
            }
            val unrelated = File(root, "database/musicapp.db").apply {
                checkNotNull(parentFile).mkdirs()
                writeBytes(byteArrayOf(1))
            }

            assertTrue(isDiagnosticExportPathAllowed(export, root))
            assertFalse(isDiagnosticExportPathAllowed(unrelated, root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun startupPlanGuardsFullApplicationInitialization() {
        assertFalse(
            StartupPlan(StartupMode.SafeMode).allowsNormalApplicationInitialization(),
        )
        assertTrue(
            StartupPlan(StartupMode.NormalStartup).allowsNormalApplicationInitialization(),
        )
        assertTrue(
            StartupPlan(
                StartupMode.NormalStartupWithPluginsDisabled,
            ).allowsNormalApplicationInitialization(),
        )
    }

    @Test
    fun jvmCrashDuringStartupEntersSafeModeOnNextLaunch() {
        val (type, severity) =
            checkNotNull(mapExitReason(ApplicationExitInfo.REASON_CRASH))

        assertEquals(
            StartupMode.SafeMode,
            SafeModePolicy().decide(policyInput(type, severity)).mode,
        )
    }

    @Test
    fun androidAnrDuringStartupCreatesARecoveryPlanOnNextLaunch() {
        val (type, severity) =
            checkNotNull(mapExitReason(ApplicationExitInfo.REASON_ANR))

        assertEquals(
            StartupMode.SafeMode,
            SafeModePolicy().decide(policyInput(type, severity)).mode,
        )
    }

    private fun policyInput(
        type: DiagnosticIncidentType,
        severity: DiagnosticIncidentSeverity,
    ) = SafeModePolicyInput(
        pendingIncidents = listOf(
            DiagnosticIncident(
                id = "android-exit",
                type = type,
                severity = severity,
                state = DiagnosticIncidentState.PendingReview,
                detectedAtEpochMs = 1,
                lastSeenAtEpochMs = 1,
                processName = "test",
                sessionId = "session",
                startupAttemptId = "attempt",
                startupStage = DiagnosticStartupStage.BackendCreating,
                fingerprint = "android-exit",
                summary = "historical exit",
                detail = null,
                artifactPaths = emptyList(),
                relatedLogSessionIds = emptyList(),
                occurrenceCount = 1,
                occurrenceTimestampsEpochMs = listOf(1),
                requiresRecovery = true,
            ),
        ),
        lastStartupAttempt = null,
        occurrences = emptyList(),
        currentTimeEpochMs = 2,
    )
}
