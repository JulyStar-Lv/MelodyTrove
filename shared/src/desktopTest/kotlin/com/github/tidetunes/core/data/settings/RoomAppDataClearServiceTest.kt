package com.github.tidetunes.core.data.settings

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.database.LyricsEntity
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.SourceAccountEntity
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.database.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomAppDataClearServiceTest {
    @Test
    fun clearsDatabaseContentInOneOperation() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<TideTunesDatabase> {
            TideTunesDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            database.sourceAccountDao().upsert(
                SourceAccountEntity(
                    id = 1,
                    providerType = ProviderTypes.WebDav,
                    displayName = "WebDAV",
                    endpoint = "https://example.invalid/dav",
                    externalAccountId = null,
                    credentialRef = "storage-1",
                    priority = 0,
                    enabled = true,
                    createdAt = 1,
                    updatedAt = 1,
                )
            )
            database.trackDao().upsertAll(listOf(track()))
            database.metadataDao().upsertLyrics(
                listOf(
                    LyricsEntity(
                        trackId = 7,
                        format = "LRC",
                        language = null,
                        synchronized = false,
                        content = "lyrics",
                        sourcePath = null,
                        updatedAt = 1,
                    )
                )
            )

            database.clearAllAppData()

            assertTrue(database.sourceAccountDao().listAll().isEmpty())
            assertNull(database.trackDao().get(7))
            assertNull(database.metadataDao().getLyrics(7))
            assertTrue(database.trackFtsDao().searchFts("Track*", 10).isEmpty())
        } finally {
            database.close()
        }
    }
}

private fun track() = TrackEntity(
    id = 7,
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
