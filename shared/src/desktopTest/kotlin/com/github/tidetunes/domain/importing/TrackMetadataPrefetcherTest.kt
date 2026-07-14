package com.github.tidetunes.domain.importing

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.core.domain.model.MetadataRefreshTarget
import com.github.tidetunes.core.domain.model.MetadataScanOptions
import com.github.tidetunes.core.domain.model.toOptions
import com.github.tidetunes.database.ArtworkEntity
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshController
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshRequest
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshResult
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackMetadataPrefetcherTest {
    @Test
    fun missingArtworkAndLyricsUseOneMinimumRefresh() = runBlocking {
        withDatabase { database ->
            database.trackDao().upsertAll(listOf(track()))
            val controller = RecordingMetadataRefreshController()
            val prefetcher = TrackMetadataPrefetcher(
                database.metadataDao(),
                database.trackDao(),
                controller,
            )

            assertTrue(prefetcher.prefetch(TRACK_ID))

            val request = controller.requests.single()
            assertEquals(MetadataRefreshScope.Track(TRACK_ID), request.scope)
            assertEquals(MetadataRefreshTarget.ArtworkAndLyrics, request.target)
            assertEquals(
                MetadataScanOptions(true, true, false),
                request.target.toOptions(),
            )
        }
    }

    @Test
    fun existingArtworkFileOnlyRefreshesLyrics() = runBlocking {
        withDatabase { database ->
            database.trackDao().upsertAll(listOf(track()))
            val artworkFile = FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
                "tidetunes-prefetch-${Random.nextLong()}.jpg"
            FileSystem.SYSTEM.write(artworkFile) { writeByte(1) }
            try {
                database.metadataDao().upsertArtwork(
                    listOf(
                        ArtworkEntity(
                            trackId = TRACK_ID,
                            albumId = null,
                            contentHash = "cached-artwork",
                            localPath = artworkFile.toString(),
                            thumbnailPath = null,
                            width = null,
                            height = null,
                            mimeType = "image/jpeg",
                            pictureType = "CoverFront",
                        )
                    )
                )
                val controller = RecordingMetadataRefreshController()
                val prefetcher = TrackMetadataPrefetcher(
                    database.metadataDao(),
                    database.trackDao(),
                    controller,
                )

                assertTrue(prefetcher.prefetch(TRACK_ID))

                assertEquals(MetadataRefreshTarget.Lyrics, controller.requests.single().target)
            } finally {
                FileSystem.SYSTEM.delete(artworkFile, mustExist = false)
            }
        }
    }

    @Test
    fun concurrentRequestsForTheSameTrackAreDeduplicated() = runBlocking {
        withDatabase { database ->
            database.trackDao().upsertAll(listOf(track()))
            val gate = CompletableDeferred<Unit>()
            val controller = RecordingMetadataRefreshController(gate)
            val prefetcher = TrackMetadataPrefetcher(
                database.metadataDao(),
                database.trackDao(),
                controller,
            )

            val first = async { prefetcher.prefetch(TRACK_ID) }
            withTimeout(5_000) {
                while (controller.requests.isEmpty()) yield()
            }
            val second = async { prefetcher.prefetch(TRACK_ID) }
            yield()
            gate.complete(Unit)

            assertTrue(first.await())
            assertTrue(second.await())
            assertEquals(1, controller.requests.size)
        }
    }
}

private class RecordingMetadataRefreshController(
    private val gate: CompletableDeferred<Unit>? = null,
) : MetadataRefreshController {
    val requests = mutableListOf<MetadataRefreshRequest>()

    override suspend fun refresh(request: MetadataRefreshRequest): MetadataRefreshResult {
        requests += request
        gate?.await()
        return MetadataRefreshResult(
            requestedCount = 1,
            refreshedCount = 1,
            failedCount = 0,
            metadataRequestCount = 1,
            metadataFetchedBytes = 128,
            metadataElapsedMs = 1,
            artworkCachedBytes = 64,
        )
    }
}

private suspend fun withDatabase(block: suspend (TideTunesDatabase) -> Unit) {
    val database = Room.inMemoryDatabaseBuilder<TideTunesDatabase> {
        TideTunesDatabaseConstructor.initialize()
    }
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
    try {
        block(database)
    } finally {
        database.close()
    }
}

private fun track() = TrackEntity(
    id = TRACK_ID,
    title = "Track",
    sortTitle = null,
    albumId = null,
    albumArtist = null,
    composer = null,
    comment = null,
    grouping = null,
    durationMs = 1_000,
    discNumber = null,
    discTotal = null,
    trackNumber = null,
    trackTotal = null,
    year = null,
    date = null,
    sampleRate = null,
    bitRate = null,
    bitsPerSample = null,
    channels = null,
    channelLayout = null,
    codec = null,
    container = null,
    lossless = null,
    createdAt = 1,
    updatedAt = 1,
)

private const val TRACK_ID = 7L
