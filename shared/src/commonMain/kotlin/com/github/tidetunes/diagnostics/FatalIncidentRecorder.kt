package com.github.tidetunes.diagnostics

import com.github.tidetunes.core.domain.model.DiagnosticIncidentDraft
import com.github.tidetunes.core.domain.model.DiagnosticIncidentSeverity
import com.github.tidetunes.core.domain.model.DiagnosticIncidentType

fun recordKotlinUncaughtException(threadName: String, throwable: Throwable) {
    runCatching {
        val snapshot = RustDiagnosticsRepository.snapshot()
        RustDiagnosticsRepository.recordFatalIncident(
            DiagnosticIncidentDraft(
                type = DiagnosticIncidentType.KotlinUncaught,
                severity = DiagnosticIncidentSeverity.Fatal,
                summary = "${throwable::class.simpleName ?: "Throwable"}: ${throwable.message.orEmpty()}",
                detail = buildString {
                    appendLine("thread=$threadName")
                    appendLine("startupStage=${snapshot.startupAttempt.lastStage}")
                    append(throwable.stackTraceToString())
                },
                fingerprintMaterial = throwable.stackTraceToString(),
                requiresRecovery = snapshot.startupAttempt.lastStage.isBeforeStable,
            )
        )
        RustDiagnosticsRepository.flush()
    }
}
