package com.github.tidetunes.service.download.data.scheduler

import com.github.tidetunes.platform.getAppCacheDir
import com.github.tidetunes.service.download.domain.DownloadStatus
import com.github.tidetunes.service.download.domain.DownloadTask
import com.github.tidetunes.service.download.domain.DownloadTaskId
import com.github.tidetunes.service.download.domain.DownloadTaskRepository
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import com.github.tidetunes.service.download.domain.canTransitionTo
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.SourcePlaybackResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskDelegateProtocol
import platform.Foundation.setValue
import platform.darwin.NSObject
import kotlin.math.max
import kotlin.time.Clock

@OptIn(ExperimentalForeignApi::class)
internal class IosUrlSessionDownloadScheduler(
    private val repository: DownloadTaskRepository,
    private val sourceRegistry: MusicSourceRegistry,
    private val legacyStoragePlaybackResolver: LegacyStoragePlaybackResolver,
    private val scope: CoroutineScope,
    private val downloadDirectoryProvider: () -> String = {
        "${getAppCacheDir()}/downloads"
    },
    private val nowEpochMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : DownloadTaskScheduler {
    private val activeResources = mutableMapOf<String, PlaybackResource>()
    private val backgroundCompletionHandlers = mutableMapOf<String, () -> Unit>()
    private val delegate = IosDownloadSessionDelegate(this)
    private val sessionIdentifier = "com.github.tidetunes.downloads"
    private val session: NSURLSession by lazy {
        val configuration =
            NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(sessionIdentifier)
        configuration.sessionSendsLaunchEvents = true
        configuration.allowsCellularAccess = true
        NSURLSession.sessionWithConfiguration(
            configuration = configuration,
            delegate = delegate,
            delegateQueue = NSOperationQueue.mainQueue,
        )
    }

    override suspend fun schedule(task: DownloadTask) {
        session.getTasksWithCompletionHandler { _, _, downloadTasks ->
            downloadTasks
                ?.filterIsInstance<NSURLSessionDownloadTask>()
                ?.filter { it.taskDescription == task.id.value }
                ?.forEach { it.cancel() }
        }
        scope.launch {
            runTask(task)
        }
    }

    override suspend fun pause(id: DownloadTaskId) {
        cancelSessionTask(id)
    }

    override suspend fun cancel(id: DownloadTaskId) {
        cancelSessionTask(id)
    }

    fun setBackgroundCompletionHandler(
        identifier: String,
        completionHandler: () -> Unit,
    ) {
        backgroundCompletionHandlers[identifier] = completionHandler
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
            val url = NSURL.URLWithString(resource.uri)
            if (url == null) {
                markFailed(task.id, "Download resource URL is invalid")
                legacyStoragePlaybackResolver.release(resource.uri)
                return
            }

            updateStatus(task.id, DownloadStatus.Downloading) { current ->
                current.copy(mimeType = resource.mimeType ?: current.mimeType)
            } ?: return

            val request = NSMutableURLRequest.requestWithURL(url)
            resource.headers.forEach { (key, value) ->
                request.setValue(value, forHTTPHeaderField = key)
            }
            activeResources[task.id.value] = resource
            session.downloadTaskWithRequest(request).apply {
                taskDescription = task.id.value
                resume()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            activeResources.remove(task.id.value)?.let { resource ->
                legacyStoragePlaybackResolver.release(resource.uri)
            }
            markFailed(task.id, error.message ?: "Download failed")
        }
    }

    private fun cancelSessionTask(id: DownloadTaskId) {
        session.getTasksWithCompletionHandler { _, _, downloadTasks ->
            downloadTasks
                ?.filterIsInstance<NSURLSessionDownloadTask>()
                ?.filter { it.taskDescription == id.value }
                ?.forEach { it.cancel() }
        }
    }

    private fun handleProgress(
        id: DownloadTaskId,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long,
    ) {
        scope.launch {
            val current = repository.getTask(id) ?: return@launch
            if (current.status != DownloadStatus.Downloading) return@launch
            repository.updateTask(
                current.copy(
                    downloadedBytes = totalBytesWritten,
                    totalBytes = totalBytesExpectedToWrite
                        .takeIf { it >= 0 }
                        ?.let { max(it, totalBytesWritten) },
                    updatedAtEpochMs = nowEpochMs(),
                )
            )
        }
    }

    private fun handleFinished(
        id: DownloadTaskId,
        location: NSURL,
    ) {
        scope.launch {
            val current = repository.getTask(id) ?: return@launch
            if (current.status != DownloadStatus.Downloading) return@launch

            val resource = activeResources[id.value]
            val targetPath = targetPathFor(current, resource)
            val targetUrl = NSURL.fileURLWithPath(targetPath)
            val fileManager = NSFileManager.defaultManager
            fileManager.createDirectoryAtPath(
                path = targetPath.substringBeforeLast('/'),
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            fileManager.removeItemAtURL(targetUrl, error = null)
            val moved = fileManager.moveItemAtURL(
                srcURL = location,
                toURL = targetUrl,
                error = null,
            )
            if (!moved) {
                markFailed(id, "Unable to move downloaded file")
                return@launch
            }

            val totalBytes = current.totalBytes
                ?.let { max(it, current.downloadedBytes) }
                ?: current.downloadedBytes
            updateStatus(id, DownloadStatus.Completed) { task ->
                task.copy(
                    totalBytes = totalBytes,
                    localPath = targetPath,
                    mimeType = resource?.mimeType ?: task.mimeType,
                    errorMessage = null,
                )
            }
        }
    }

    private fun handleCompleted(
        id: DownloadTaskId,
        error: NSError?,
    ) {
        scope.launch {
            val resource = activeResources.remove(id.value)
            if (error != null) {
                val current = repository.getTask(id)
                if (
                    current?.status != DownloadStatus.Paused &&
                    current?.status != DownloadStatus.Cancelled
                ) {
                    markFailed(id, error.localizedDescription)
                }
            }
            if (resource != null) {
                legacyStoragePlaybackResolver.release(resource.uri)
            }
        }
    }

    private fun handleEventsFinished(session: NSURLSession) {
        val completionHandler = backgroundCompletionHandlers.remove(session.configuration.identifier)
        completionHandler?.invoke()
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

    private fun targetPathFor(
        task: DownloadTask,
        resource: PlaybackResource?,
    ): String {
        return "${downloadDirectoryProvider()}/" +
            "${safeFileName(task.id.value)}${extensionFor(task, resource)}"
    }

    private class IosDownloadSessionDelegate(
        private val scheduler: IosUrlSessionDownloadScheduler,
    ) : NSObject(),
        NSURLSessionDownloadDelegateProtocol,
        NSURLSessionTaskDelegateProtocol {
        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didWriteData: Long,
            totalBytesWritten: Long,
            totalBytesExpectedToWrite: Long,
        ) {
            val id = downloadTask.taskDescription?.let(::DownloadTaskId) ?: return
            scheduler.handleProgress(id, totalBytesWritten, totalBytesExpectedToWrite)
        }

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didFinishDownloadingToURL: NSURL,
        ) {
            val id = downloadTask.taskDescription?.let(::DownloadTaskId) ?: return
            scheduler.handleFinished(id, didFinishDownloadingToURL)
        }

        override fun URLSession(
            session: NSURLSession,
            task: NSURLSessionTask,
            didCompleteWithError: NSError?,
        ) {
            val id = task.taskDescription?.let(::DownloadTaskId) ?: return
            scheduler.handleCompleted(id, didCompleteWithError)
        }

        override fun URLSessionDidFinishEventsForBackgroundURLSession(
            session: NSURLSession,
        ) {
            scheduler.handleEventsFinished(session)
        }
    }
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
    resource: PlaybackResource?,
): String {
    val fromPath = sequenceOf(
        task.mediaId.remoteId,
        task.title,
        resource?.uri?.substringBefore('?').orEmpty(),
    )
        .mapNotNull(::extensionFromPath)
        .firstOrNull()
    return fromPath ?: extensionFromMimeType(resource?.mimeType) ?: ".audio"
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
