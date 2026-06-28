package com.github.tidetunes.service.download.domain

import com.github.tidetunes.core.domain.model.MediaId
import kotlin.time.Clock

data class DownloadRequest(
    val mediaId: MediaId,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
) {
    init {
        require(title.isNotBlank()) { "DownloadRequest title cannot be blank" }
    }
}

class EnqueueDownloadUseCase(
    private val downloadController: DownloadController,
    private val nowEpochMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    suspend operator fun invoke(request: DownloadRequest): DownloadTaskId {
        val now = nowEpochMs()
        val taskId = request.mediaId.toDownloadTaskId()
        downloadController.enqueue(
            DownloadTask(
                id = taskId,
                mediaId = request.mediaId,
                title = request.title,
                artist = request.artist,
                album = request.album,
                durationMs = request.durationMs,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
        )
        return taskId
    }
}

internal fun MediaId.toDownloadTaskId(): DownloadTaskId {
    return DownloadTaskId("media:${sourceId.value}:${mediaType.name}:$remoteId")
}
