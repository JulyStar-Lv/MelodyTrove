package com.github.tidetunes.service.download.data.scheduler

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.tidetunes.platform.appContext
import com.github.tidetunes.service.download.domain.DownloadStatus
import com.github.tidetunes.service.download.domain.DownloadTask
import com.github.tidetunes.service.download.domain.DownloadTaskId
import com.github.tidetunes.service.download.domain.DownloadTaskRepository
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import com.github.tidetunes.service.download.domain.canTransitionTo
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourcePlaybackResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlin.math.max
import kotlin.time.Clock

internal class AndroidWorkManagerDownloadScheduler(
    private val workManager: WorkManager = WorkManager.getInstance(appContext),
) : DownloadTaskScheduler {
    override suspend fun schedule(task: DownloadTask) {
        val requestBuilder = OneTimeWorkRequestBuilder<AndroidDownloadWorker>()
            .setInputData(
                Data.Builder()
                    .putString(KEY_DOWNLOAD_TASK_ID, task.id.value)
                    .build()
            )

        if (task.mediaId.sourceId != BuiltInSourceIds.Local) {
            requestBuilder.setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
        }

        workManager.enqueueUniqueWork(
            workName(task.id),
            ExistingWorkPolicy.REPLACE,
            requestBuilder.build(),
        )
    }

    override suspend fun pause(id: DownloadTaskId) {
        workManager.cancelUniqueWork(workName(id))
    }

    override suspend fun cancel(id: DownloadTaskId) {
        workManager.cancelUniqueWork(workName(id))
    }
}

internal class AndroidDownloadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {
    private val repository: DownloadTaskRepository by inject()
    private val sourceRegistry: MusicSourceRegistry by inject()
    private val legacyStoragePlaybackResolver: LegacyStoragePlaybackResolver by inject()
    private val nowEpochMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_DOWNLOAD_TASK_ID)
            ?.let(::DownloadTaskId)
            ?: return Result.failure()
        val task = repository.getTask(id) ?: return Result.success()
        return runTask(task)
    }

    private suspend fun runTask(task: DownloadTask): Result {
        if (updateStatus(task.id, DownloadStatus.Resolving) == null) {
            return Result.success()
        }

        val source = sourceRegistry.sourceOrNull(task.mediaId.sourceId)
        if (source == null) {
            markFailed(task.id, "Music source is unavailable")
            return Result.failure()
        }

        val resource = when (val result = source.resolvePlayback(task.mediaId)) {
            is SourcePlaybackResult.Success -> result.resource
            is SourcePlaybackResult.Failure -> {
                markFailed(task.id, "Unable to resolve download resource: ${result.reason}")
                return Result.failure()
            }
        }

        return try {
            downloadResource(task, resource)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            markFailed(task.id, e.message ?: "Download failed")
            Result.failure()
        } finally {
            legacyStoragePlaybackResolver.release(resource.uri)
        }
    }

    private suspend fun downloadResource(
        task: DownloadTask,
        resource: PlaybackResource,
    ) {
        openResource(resource).use { opened ->
            val targetFile = targetFileFor(task, resource)
            val targetDirectory = requireNotNull(targetFile.parentFile)
            val partFile = File(targetDirectory, "${targetFile.name}.part")
            targetDirectory.mkdirs()

            val totalBytes = opened.totalBytes
            updateStatus(task.id, DownloadStatus.Downloading) { current ->
                current.copy(
                    totalBytes = normalizedTotalBytes(totalBytes, current.downloadedBytes),
                    mimeType = resource.mimeType ?: current.mimeType,
                )
            } ?: return

            var downloadedBytes = 0L
            var lastPersistedBytes = 0L
            try {
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = opened.input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read.toLong()
                        if (downloadedBytes - lastPersistedBytes >= PROGRESS_UPDATE_BYTES) {
                            updateDownloadingProgress(
                                id = task.id,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                            )
                            lastPersistedBytes = downloadedBytes
                        }
                    }
                }
            } catch (e: CancellationException) {
                partFile.delete()
                throw e
            }

            updateDownloadingProgress(
                id = task.id,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
            )
            movePartFile(partFile, targetFile)
            updateStatus(task.id, DownloadStatus.Completed) { current ->
                current.copy(
                    downloadedBytes = downloadedBytes,
                    totalBytes = normalizedTotalBytes(totalBytes, downloadedBytes),
                    localPath = targetFile.absolutePath,
                    mimeType = resource.mimeType ?: current.mimeType,
                    errorMessage = null,
                )
            }
        }
    }

    private fun openResource(resource: PlaybackResource): OpenedAndroidDownloadResource {
        val uri = parseUri(resource.uri)
        return when (uri?.scheme?.lowercase()) {
            "http",
            "https" -> openHttp(uri, resource)
            "content" -> openContent(Uri.parse(resource.uri))
            "file" -> openFile(File(uri))
            null,
            "" -> openFile(File(resource.uri))
            else -> {
                if (resource.isLocal) {
                    openFile(File(resource.uri))
                } else {
                    throw IOException("Unsupported download URI scheme: ${uri.scheme}")
                }
            }
        }
    }

    private fun openHttp(
        uri: URI,
        resource: PlaybackResource,
    ): OpenedAndroidDownloadResource {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        resource.headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            val responseCode = connection.responseCode
            connection.disconnect()
            throw IOException("HTTP download failed with status $responseCode")
        }
        return OpenedAndroidDownloadResource(
            input = connection.inputStream,
            totalBytes = connection.contentLengthLong.takeIf { it >= 0 },
            closeAction = { connection.disconnect() },
        )
    }

    private fun openContent(uri: Uri): OpenedAndroidDownloadResource {
        val input = applicationContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open content URI")
        return OpenedAndroidDownloadResource(
            input = input,
            totalBytes = null,
        )
    }

    private fun openFile(file: File): OpenedAndroidDownloadResource {
        if (!file.isFile) {
            throw IOException("Download source file does not exist")
        }
        return OpenedAndroidDownloadResource(
            input = file.inputStream(),
            totalBytes = file.length(),
        )
    }

    private suspend fun updateDownloadingProgress(
        id: DownloadTaskId,
        downloadedBytes: Long,
        totalBytes: Long?,
    ) {
        val current = repository.getTask(id) ?: return
        if (current.status != DownloadStatus.Downloading) return
        repository.updateTask(
            current.copy(
                downloadedBytes = downloadedBytes,
                totalBytes = normalizedTotalBytes(totalBytes, downloadedBytes),
                updatedAtEpochMs = nowEpochMs(),
            )
        )
    }

    private suspend fun markFailed(id: DownloadTaskId, message: String) {
        updateStatus(id, DownloadStatus.Failed) { current ->
            current.copy(errorMessage = message)
        }
    }

    private suspend fun updateStatus(
        id: DownloadTaskId,
        status: DownloadStatus,
        transform: (DownloadTask) -> DownloadTask = { it },
    ): DownloadTask? {
        val current = repository.getTask(id) ?: return null
        if (!current.status.canTransitionTo(status)) return null
        val updated = transform(current).copy(
            status = status,
            updatedAtEpochMs = nowEpochMs(),
        )
        repository.updateTask(updated)
        return updated
    }

    private fun targetFileFor(
        task: DownloadTask,
        resource: PlaybackResource,
    ): File {
        return File(
            File(applicationContext.filesDir, "downloads"),
            "${safeFileName(task.id.value)}${extensionFor(task, resource)}",
        )
    }

    private fun movePartFile(partFile: File, targetFile: File) {
        if (targetFile.exists()) targetFile.delete()
        if (!partFile.renameTo(targetFile)) {
            partFile.copyTo(targetFile, overwrite = true)
            partFile.delete()
        }
    }

    private fun parseUri(value: String): URI? {
        return runCatching { URI(value) }.getOrNull()
    }
}

private class OpenedAndroidDownloadResource(
    val input: InputStream,
    val totalBytes: Long?,
    private val closeAction: () -> Unit = {},
) : Closeable {
    override fun close() {
        try {
            input.close()
        } finally {
            closeAction()
        }
    }
}

private fun workName(id: DownloadTaskId): String {
    return "download:${id.value}"
}

private fun normalizedTotalBytes(totalBytes: Long?, downloadedBytes: Long): Long? {
    return totalBytes?.let { max(it, downloadedBytes) }
}

private fun safeFileName(value: String): String {
    return value
        .map { character ->
            if (character.isLetterOrDigit() || character == '-' || character == '_' || character == '.') {
                character
            } else {
                '_'
            }
        }
        .joinToString("")
        .take(96)
        .ifBlank { "download" }
}

private fun extensionFor(
    task: DownloadTask,
    resource: PlaybackResource,
): String {
    val fromPath = sequenceOf(
        task.mediaId.remoteId,
        task.title,
        resource.uri.substringBefore('?'),
    )
        .mapNotNull(::extensionFromPath)
        .firstOrNull()
    return fromPath ?: extensionFromMimeType(resource.mimeType) ?: ".audio"
}

private fun extensionFromPath(path: String): String? {
    val extension = path
        .substringAfterLast('/', path)
        .substringAfterLast('.', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() && it.length <= 8 }
        ?.lowercase()
        ?: return null
    return ".$extension"
}

private fun extensionFromMimeType(mimeType: String?): String? {
    return when (mimeType) {
        "audio/flac" -> ".flac"
        "audio/mpeg" -> ".mp3"
        "audio/mp4" -> ".m4a"
        "audio/ogg" -> ".ogg"
        "audio/opus" -> ".opus"
        "audio/wav" -> ".wav"
        else -> null
    }
}

private const val KEY_DOWNLOAD_TASK_ID = "download_task_id"
private const val PROGRESS_UPDATE_BYTES = 256 * 1024
