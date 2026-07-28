package io.github.julystar.musicapp.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentSeverity
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentType
import io.github.julystar.musicapp.core.domain.model.DiagnosticPlatformExit
import io.github.julystar.musicapp.platform.appContext
import io.github.julystar.musicapp.platform.getAppBuildInfo
import io.github.julystar.musicapp.platform.getAppVersion
import java.io.InputStream

private const val MaxExitTraceBytes = 1024 * 1024
private const val MaxHistoricalExitCount = 32

fun collectAndroidHistoricalExitInfo() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val activityManager = appContext.getSystemService(ActivityManager::class.java) ?: return
    activityManager
        .getHistoricalProcessExitReasons(appContext.packageName, 0, MaxHistoricalExitCount)
        .forEach { exit ->
            exit.toDiagnosticPlatformExit()?.let { record ->
                runCatching { RustDiagnosticsRepository.importPlatformExit(record) }
            }
        }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.toDiagnosticPlatformExit(): DiagnosticPlatformExit? {
    val (type, severity) = mapExitReason(reason) ?: return null
    val previousAttempt = RustDiagnosticsRepository.snapshot().previousStartupAttempt
    val previousStage = previousAttempt?.lastStage
    val traceResult = readLimitedTrace()
    return DiagnosticPlatformExit(
        exitKey = historicalExitKey(timestamp, pid, reason, status, processName.orEmpty()),
        type = type,
        severity = severity,
        timestampEpochMs = timestamp,
        processName = processName.orEmpty().ifBlank { appContext.packageName },
        pid = pid.toLong(),
        reason = reason.toLong(),
        status = status.toLong(),
        importance = importance.toLong(),
        pssKb = pss,
        rssKb = rss,
        description = description,
        trace = traceResult.bytes,
        traceTruncated = traceResult.truncated,
        requiresRecovery = severity == DiagnosticIncidentSeverity.Fatal &&
            previousStage?.isBeforeStable == true,
        environmentSummary = buildString {
            append("appVersion=")
            append(getAppVersion())
            append(", build=")
            append(getAppBuildInfo())
            append(", android=")
            append(Build.VERSION.RELEASE)
            append(" (API ")
            append(Build.VERSION.SDK_INT)
            append(')')
            append(", device=")
            append(Build.MANUFACTURER)
            append(' ')
            append(Build.MODEL)
            append(", currentPid=")
            append(Process.myPid())
        },
        startupAttemptId = previousAttempt?.attemptId,
        startupStage = previousStage,
    )
}

internal data class LimitedTrace(val bytes: ByteArray?, val truncated: Boolean)

@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.readLimitedTrace(): LimitedTrace {
    val stream = runCatching { traceInputStream }.getOrNull() ?: return LimitedTrace(null, false)
    return stream.use { readLimitedTrace(it, MaxExitTraceBytes) }
}

internal fun historicalExitInfoSupported(apiLevel: Int): Boolean =
    apiLevel >= Build.VERSION_CODES.R

internal fun historicalExitKey(
    timestamp: Long,
    pid: Int,
    reason: Int,
    status: Int,
    processName: String,
): String = "$timestamp|$pid|$reason|$status|$processName"

internal fun mapExitReason(
    reason: Int,
): Pair<DiagnosticIncidentType, DiagnosticIncidentSeverity>? = when (reason) {
    ApplicationExitInfo.REASON_ANR ->
        DiagnosticIncidentType.AndroidAnr to DiagnosticIncidentSeverity.Warning
    ApplicationExitInfo.REASON_CRASH ->
        DiagnosticIncidentType.KotlinUncaught to DiagnosticIncidentSeverity.Fatal
    ApplicationExitInfo.REASON_CRASH_NATIVE ->
        DiagnosticIncidentType.NativeCrash to DiagnosticIncidentSeverity.Fatal
    ApplicationExitInfo.REASON_LOW_MEMORY ->
        DiagnosticIncidentType.OutOfMemory to DiagnosticIncidentSeverity.Warning
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE ->
        DiagnosticIncidentType.UnknownAbnormalExit to DiagnosticIncidentSeverity.Warning
    else -> null
}

internal fun readLimitedTrace(input: InputStream, maxBytes: Int): LimitedTrace {
    require(maxBytes > 0)
    return input.use {
        val output = ByteArray(maxBytes)
        val buffer = ByteArray(8 * 1024)
        var written = 0
        var truncated = false
        while (true) {
            val count = it.read(buffer)
            if (count <= 0) break
            val remaining = maxBytes - written
            if (count > remaining) {
                buffer.copyInto(output, destinationOffset = written, endIndex = remaining)
                written += remaining
                truncated = true
                break
            }
            buffer.copyInto(output, destinationOffset = written, endIndex = count)
            written += count
        }
        LimitedTrace(
            bytes = output.copyOf(written).takeIf(ByteArray::isNotEmpty),
            truncated = truncated || it.read() != -1,
        )
    }
}
