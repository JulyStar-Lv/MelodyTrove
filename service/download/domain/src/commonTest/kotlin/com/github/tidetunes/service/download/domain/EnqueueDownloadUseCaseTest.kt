package com.github.tidetunes.service.download.domain

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class EnqueueDownloadUseCaseTest {
    @Test
    fun createsStableDownloadTaskFromRequest() = runBlocking {
        val controller = RecordingDownloadController()
        val useCase = EnqueueDownloadUseCase(
            downloadController = controller,
            nowEpochMs = { 42 },
        )
        val mediaId = MediaId(
            sourceId = SourceId("webdav"),
            mediaType = MediaType.Track,
            remoteId = "legacy-storage-track:storage%3A2:%2FMusic%2FSong.flac",
        )

        val id = useCase(
            DownloadRequest(
                mediaId = mediaId,
                title = "Song",
                artist = "Artist",
                album = "Album",
                durationMs = 180_000,
            )
        )

        val task = controller.enqueued.single()
        assertEquals(id, task.id)
        assertEquals(DownloadTaskId("media:webdav:Track:legacy-storage-track:storage%3A2:%2FMusic%2FSong.flac"), id)
        assertEquals(mediaId, task.mediaId)
        assertEquals("Song", task.title)
        assertEquals("Artist", task.artist)
        assertEquals("Album", task.album)
        assertEquals(180_000, task.durationMs)
        assertEquals(DownloadStatus.Queued, task.status)
        assertEquals(42, task.createdAtEpochMs)
        assertEquals(42, task.updatedAtEpochMs)
    }
}

private class RecordingDownloadController : DownloadController {
    override val tasks: Flow<List<DownloadTask>> = MutableStateFlow(emptyList())
    val enqueued = mutableListOf<DownloadTask>()

    override suspend fun enqueue(task: DownloadTask) {
        enqueued += task
    }

    override suspend fun pause(id: DownloadTaskId) = Unit
    override suspend fun resume(id: DownloadTaskId) = Unit
    override suspend fun cancel(id: DownloadTaskId) = Unit
    override suspend fun cancelAll() = Unit
    override suspend fun retry(id: DownloadTaskId) = Unit
}
