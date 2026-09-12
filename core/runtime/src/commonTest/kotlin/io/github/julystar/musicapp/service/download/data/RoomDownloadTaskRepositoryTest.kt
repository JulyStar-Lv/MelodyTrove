package io.github.julystar.musicapp.service.download.data

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.MediaType
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.service.download.domain.DownloadStatus
import io.github.julystar.musicapp.service.download.domain.DownloadTask
import io.github.julystar.musicapp.service.download.domain.DownloadTaskId
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomDownloadTaskRepositoryTest {
    @Test
    fun mapsDomainTaskToPersistentEntityAndBack() {
        val task = DownloadTask(
            id = DownloadTaskId("download-1"),
            mediaId = MediaId(
                sourceId = SourceId("onedrive"),
                mediaType = MediaType.Track,
                remoteId = "drive-item-1",
            ),
            title = "Moon",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000,
            status = DownloadStatus.Downloading,
            downloadedBytes = 10,
            totalBytes = 20,
            localPath = "/cache/moon.flac",
            mimeType = "audio/flac",
            errorMessage = null,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
        )

        val roundTrip = task.toEntity().toDomain()

        assertEquals(task, roundTrip)
    }
}
