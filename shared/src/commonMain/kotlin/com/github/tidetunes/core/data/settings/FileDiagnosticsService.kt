package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.domain.model.DiagnosticsExportResult
import com.github.tidetunes.core.domain.model.DiagnosticsReport
import com.github.tidetunes.core.domain.repository.DiagnosticsService
import com.github.tidetunes.core.domain.repository.StorageUsageRepository
import com.github.tidetunes.database.SourceAccountDao
import com.github.tidetunes.database.SourceErrorDao
import com.github.tidetunes.database.TIDE_TUNES_DATABASE_VERSION
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.platform.getAppBuildInfo
import com.github.tidetunes.platform.getAppDocumentDir
import com.github.tidetunes.platform.getAppGitCommitSha
import com.github.tidetunes.platform.getAppVersion
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.playback.domain.PlaybackController
import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

class FileDiagnosticsService(
    private val sourceAccountDao: SourceAccountDao,
    private val sourceErrorDao: SourceErrorDao,
    private val trackDao: TrackDao,
    private val librarySyncController: LibrarySyncController,
    private val playbackController: PlaybackController,
    private val storageUsageRepository: StorageUsageRepository,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : DiagnosticsService {

    override suspend fun collectDiagnostics(): DiagnosticsReport {
        val recentTasks = librarySyncController.recentTasks.first()
        val recentErrors = sourceErrorDao.listRecent(DIAGNOSTIC_ERROR_LIMIT)
            .map { error -> error.message.redactSensitiveData() }
        val playerState = playbackController.state.value
        return DiagnosticsReport(
            generatedAtEpochMs = currentTimeMillis(),
            appVersion = getAppVersion(),
            buildInfo = getAppBuildInfo(),
            gitCommitSha = getAppGitCommitSha(),
            platformInfo = getAppBuildInfo(),
            databaseVersion = TIDE_TUNES_DATABASE_VERSION,
            sourceCount = sourceAccountDao.listAll().size,
            trackCount = trackDao.count(),
            recentScanSummary = recentTasks.firstOrNull()?.let { task ->
                "${task.status}: total=${task.scannedCount}, imported=${task.importedCount}, failed=${task.failedCount}"
            },
            playerStateSummary = "${playerState.status}, queue=${playbackController.queue.value.items.size}",
            storageUsage = storageUsageRepository.loadUsage(),
            recentErrors = recentErrors,
        )
    }

    override suspend fun exportDiagnostics(): DiagnosticsExportResult {
        return runCatching {
            val report = collectDiagnostics()
            val directory = getAppDocumentDir().toPath() / "diagnostics"
            fileSystem.createDirectories(directory)
            val path = directory / "tidetunes-diagnostics-${report.generatedAtEpochMs}.txt"
            val sink = fileSystem.sink(path).buffer()
            try {
                sink.writeUtf8(report.toText())
            } finally {
                sink.close()
            }
            DiagnosticsExportResult.Success(path.toString())
        }.getOrElse { error ->
            DiagnosticsExportResult.Failure(error.message ?: "Unknown diagnostics export error")
        }
    }
}

private fun DiagnosticsReport.toText(): String = buildString {
    appendLine("TideTunes diagnostics")
    appendLine("generatedAtEpochMs=$generatedAtEpochMs")
    appendLine("appVersion=$appVersion")
    appendLine("buildInfo=${buildInfo.redactSensitiveData()}")
    appendLine("gitCommitSha=$gitCommitSha")
    appendLine("platform=${platformInfo.redactSensitiveData()}")
    appendLine("databaseVersion=$databaseVersion")
    appendLine("sourceCount=$sourceCount")
    appendLine("trackCount=$trackCount")
    appendLine("recentScan=${recentScanSummary?.redactSensitiveData() ?: "none"}")
    appendLine("player=$playerStateSummary")
    appendLine("audioCacheBytes=${storageUsage.audioBytes ?: -1}")
    appendLine("imageCacheBytes=${storageUsage.imageBytes ?: -1}")
    appendLine("downloadBytes=${storageUsage.downloadBytes ?: -1}")
    appendLine("databaseBytes=${storageUsage.databaseBytes ?: -1}")
    appendLine("logBytes=${storageUsage.logBytes ?: -1}")
    appendLine("totalBytes=${storageUsage.totalBytes ?: -1}")
    appendLine("recentErrors:")
    if (recentErrors.isEmpty()) appendLine("- none")
    else recentErrors.forEach { error -> appendLine("- ${error.redactSensitiveData()}") }
}

internal fun String.redactSensitiveData(): String {
    return replace(URL_CREDENTIAL_REGEX, "$1***:***@")
        .replace(SENSITIVE_QUERY_REGEX, "$1=***")
        .replace(AUTHORIZATION_REGEX, "$1 ***")
}

private val URL_CREDENTIAL_REGEX = Regex("(https?://)[^/@\\s:]+:[^/@\\s]+@", RegexOption.IGNORE_CASE)
private val SENSITIVE_QUERY_REGEX = Regex(
    "(?i)(token|access_token|refresh_token|password|passwd|secret|code)=([^&\\s]+)"
)
private val AUTHORIZATION_REGEX = Regex("(?i)(authorization:|bearer|basic)\\s+[^\\s,]+")
private const val DIAGNOSTIC_ERROR_LIMIT = 20
