package io.github.julystar.musicapp.diagnostics

import io.github.julystar.musicapp.core.domain.model.DiagnosticLogCategory
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogLevel

object AppLogger {
    fun trace(
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        correlationId: String? = null,
        fields: Map<String, String> = emptyMap(),
    ) = write(DiagnosticLogLevel.Trace, category, target, message, null, correlationId, fields)

    fun debug(
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        correlationId: String? = null,
        fields: Map<String, String> = emptyMap(),
    ) = write(DiagnosticLogLevel.Debug, category, target, message, null, correlationId, fields)

    fun info(
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        correlationId: String? = null,
        fields: Map<String, String> = emptyMap(),
    ) = write(DiagnosticLogLevel.Info, category, target, message, null, correlationId, fields)

    fun warn(
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        detail: String? = null,
        correlationId: String? = null,
        fields: Map<String, String> = emptyMap(),
    ) = write(DiagnosticLogLevel.Warn, category, target, message, detail, correlationId, fields)

    fun error(
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        detail: String? = null,
        correlationId: String? = null,
        fields: Map<String, String> = emptyMap(),
    ) = write(DiagnosticLogLevel.Error, category, target, message, detail, correlationId, fields)

    private fun write(
        level: DiagnosticLogLevel,
        category: DiagnosticLogCategory,
        target: String,
        message: String,
        detail: String?,
        correlationId: String?,
        fields: Map<String, String>,
    ) {
        runCatching {
            RustDiagnosticsRepository.log(
                level = level,
                category = category,
                target = target,
                message = message,
                detail = detail,
                correlationId = correlationId,
                fields = fields,
            )
        }
    }
}
