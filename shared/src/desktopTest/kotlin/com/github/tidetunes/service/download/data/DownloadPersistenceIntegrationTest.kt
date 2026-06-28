package com.github.tidetunes.service.download.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.database.MIGRATION_3_4
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.service.download.domain.DownloadStatus
import com.github.tidetunes.service.download.domain.DownloadTask
import com.github.tidetunes.service.download.domain.DownloadTaskId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadPersistenceIntegrationTest {
    @Test
    fun migrationThreeToFourAddsDownloadTaskTable() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            MIGRATION_3_4.migrate(connection)

            val tables = buildSet {
                connection.prepare(
                    "SELECT name FROM sqlite_master WHERE type = 'table'"
                ).use { statement ->
                    while (statement.step()) {
                        add(statement.getText(0))
                    }
                }
            }
            val columns = buildSet {
                connection.prepare("PRAGMA table_info(download_task)").use { statement ->
                    while (statement.step()) {
                        add(statement.getText(1))
                    }
                }
            }

            assertTrue("download_task" in tables)
            assertTrue("sourceId" in columns)
            assertTrue("mediaType" in columns)
            assertTrue("status" in columns)
            assertTrue("localPath" in columns)
        } finally {
            connection.close()
        }
    }

    @Test
    fun repositoryPersistsAndObservesDownloadTasks() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<TideTunesDatabase> {
            TideTunesDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            val repository = RoomDownloadTaskRepository(database.downloadTaskDao())
            val task = DownloadTask(
                id = DownloadTaskId("download-1"),
                mediaId = MediaId(
                    sourceId = SourceId("webdav"),
                    mediaType = MediaType.Track,
                    remoteId = "music/song.flac",
                ),
                title = "Song",
                status = DownloadStatus.Queued,
                downloadedBytes = 0,
                totalBytes = 100,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            )

            repository.upsertTask(task)
            repository.updateTask(
                task.copy(
                    status = DownloadStatus.Downloading,
                    downloadedBytes = 40,
                    updatedAtEpochMs = 2,
                )
            )

            assertEquals(DownloadStatus.Downloading, repository.getTask(task.id)?.status)
            assertEquals(40, repository.getTask(task.id)?.downloadedBytes)
            assertEquals(listOf(task.id), repository.observeActiveTasks().first().map { it.id })
        } finally {
            database.close()
        }
    }
}
