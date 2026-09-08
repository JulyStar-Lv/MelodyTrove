package io.github.julystar.musicapp.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSError
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun fetchRemoteImageBytes(
    url: String,
    maxBytes: Long,
): ByteArray? = suspendCancellableCoroutine { continuation ->
    val remoteUrl = NSURL.URLWithString(url)
    if (remoteUrl == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }
    val delegate = RemoteImageDownloadDelegate(continuation, maxBytes)
    val session = NSURLSession.sessionWithConfiguration(
        configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration,
        delegate = delegate,
        delegateQueue = NSOperationQueue.mainQueue,
    )
    val task = session.downloadTaskWithURL(remoteUrl)
    delegate.attach(session, task)
    continuation.invokeOnCancellation {
        task.cancel()
        session.invalidateAndCancel()
    }
    task.resume()
}

@OptIn(ExperimentalForeignApi::class)
private class RemoteImageDownloadDelegate(
    private val continuation: CancellableContinuation<ByteArray?>,
    private val maxBytes: Long,
) : NSObject(), NSURLSessionDownloadDelegateProtocol, NSURLSessionTaskDelegateProtocol {
    private lateinit var session: NSURLSession
    private lateinit var task: NSURLSessionDownloadTask
    private var completed = false

    fun attach(session: NSURLSession, task: NSURLSessionDownloadTask) {
        this.session = session
        this.task = task
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long,
    ) {
        if (totalBytesWritten > maxBytes || totalBytesExpectedToWrite > maxBytes) {
            task.cancel()
            complete(null)
        }
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL,
    ) {
        val path = didFinishDownloadingToURL.path?.toPath() ?: return complete(null)
        val metadata = FileSystem.SYSTEM.metadataOrNull(path) ?: return complete(null)
        if (!metadata.isRegularFile || (metadata.size ?: 0L) > maxBytes) return complete(null)
        val bytes = try {
            FileSystem.SYSTEM.read(path) { readByteArray() }
        } catch (_: Exception) {
            null
        }
        complete(bytes)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        if (didCompleteWithError != null) complete(null)
    }

    private fun complete(bytes: ByteArray?) {
        if (completed) return
        completed = true
        if (continuation.isActive) continuation.resume(bytes)
        session.finishTasksAndInvalidate()
    }
}
