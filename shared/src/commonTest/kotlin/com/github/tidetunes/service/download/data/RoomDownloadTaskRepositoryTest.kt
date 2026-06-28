package com.github.tidetunes.service.download.data

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.service.download.domain.DownloadStatus
import com.github.tidetunes.service.download.domain.DownloadTask
import com.github.tidetunes.service.download.domain.DownloadTaskId
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
