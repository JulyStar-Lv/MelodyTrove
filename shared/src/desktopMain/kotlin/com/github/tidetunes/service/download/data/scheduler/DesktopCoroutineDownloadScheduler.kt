package com.github.tidetunes.service.download.data.scheduler

import com.github.tidetunes.platform.getAppCacheDir
import com.github.tidetunes.service.download.domain.DownloadStatus
import com.github.tidetunes.service.download.domain.DownloadTask
import com.github.tidetunes.service.download.domain.DownloadTaskId
import com.github.tidetunes.service.download.domain.DownloadTaskRepository
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import com.github.tidetunes.service.download.domain.canTransitionTo
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.storage.LegacyStoragePlaybackResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.time.Clock

internal class DesktopCoroutineDownloadScheduler(
    private val repository: DownloadTaskRepository,
    private val sourceRegistry: MusicSourceRegistry,
    private val legacyStoragePlaybackResolver: LegacyStoragePlaybackResolver,
    private val scope: CoroutineScope,
    private val downloadDirectoryProvider: () -> File = {
        File(getAppCacheDir(), "downloads")
    },
    private val resourceOpener: DesktopDownloadResourceOpener = JvmDesktopDownloadResourceOpener,
    private val maxConcurrentTasks: Int = 2,
    private val nowEpochMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : DownloadTaskScheduler {
    private val activeJobs = ConcurrentHashMap<DownloadTaskId, Job>()
    private val semaphore = Semaphore(maxConcurrentTasks)

    init {
        require(maxConcurrentTasks > 0) {
            "Desktop download scheduler concurrency must be positive"
        }
    }

    override suspend fun schedule(task: DownloadTask) {
        activeJobs.remove(task.id)?.cancelAndJoin()
        val job = scope.launch {
            semaphore.withPermit {
                runTask(task)
            }
        }
        activeJobs[task.id] = job
        job.invokeOnCompletion {
            activeJobs.remove(task.id, job)
        }
    }

    override suspend fun pause(id: DownloadTaskId) {
        activeJobs.remove(id)?.cancel()
    }

    override suspend fun cancel(id: DownloadTaskId) {
        activeJobs.remove(id)?.cancel()
    }

    private suspend fun runTask(task: DownloadTask) {
        if (updateStatus(task.id, DownloadStatus.Resolving) == null) return

        val source = sourceRegistry.sourceOrNull(task.mediaId.sourceId)
        if (source == null) {
            markFailed(task.id, "Music source is unavailable")
            return
        }

        val resource = when (val result = source.resolvePlayback(task.mediaId)) {
            is SourcePlaybackResult.Success -> result.resource
            is SourcePlaybackResult.Failure -> {
                markFailed(task.id, "Unable to resolve download resource: ${result.reason}")
                return
            }
        }

        try {
            downloadResource(task, resource)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            markFailed(task.id, e.message ?: "Download failed")
        } finally {
            legacyStoragePlaybackResolver.release(resource.uri)
        }
    }

    private suspend fun downloadResource(
        task: DownloadTask,
        resource: PlaybackResource,
    ) {
        withContext(Dispatchers.IO) {
            resourceOpener.open(resource).use { opened ->
                val targetFile = targetFileFor(task, resource)
                val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
                targetFile.parentFile.mkdirs()

                val totalBytes = opened.totalBytes
                updateStatus(task.id, DownloadStatus.Downloading) { current ->
                    current.copy(
                        totalBytes = normalizedTotalBytes(totalBytes, current.downloadedBytes),
                        mimeType = resource.mimeType ?: current.mimeType,
                    )
                } ?: return@withContext

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
        val directory = downloadDirectoryProvider()
        return File(
            directory,
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
}

internal interface DesktopDownloadResourceOpener {
    fun open(resource: PlaybackResource): OpenedDesktopDownloadResource
}

internal class OpenedDesktopDownloadResource(
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

internal object JvmDesktopDownloadResourceOpener : DesktopDownloadResourceOpener {
    override fun open(resource: PlaybackResource): OpenedDesktopDownloadResource {
        val uri = parseUri(resource.uri)
        return when (uri?.scheme?.lowercase()) {
            "http",
            "https" -> openHttp(uri, resource)
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
    ): OpenedDesktopDownloadResource {
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
        return OpenedDesktopDownloadResource(
            input = connection.inputStream,
            totalBytes = connection.contentLengthLong.takeIf { it >= 0 },
            closeAction = { connection.disconnect() },
        )
    }

    private fun openFile(file: File): OpenedDesktopDownloadResource {
        if (!file.isFile) {
            throw IOException("Download source file does not exist")
        }
        return OpenedDesktopDownloadResource(
            input = file.inputStream(),
            totalBytes = file.length(),
        )
    }

    private fun parseUri(value: String): URI? {
        return runCatching { URI(value) }.getOrNull()
    }
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

private const val PROGRESS_UPDATE_BYTES = 256 * 1024
