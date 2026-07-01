package com.github.tidetunes.feature.search.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.database.RemoteFileEntity
import com.github.tidetunes.database.SelectedFolderEntity
import com.github.tidetunes.database.StorageEntity
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.source.storage.LegacyStorageLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomSearchRepositoryIntegrationTest {
    @Test
    fun searchLocalLibraryMatchesTrackFieldsAndSkipsDeletedRemoteFiles() = withDatabase { database ->
        val folderId = seedStorageAndFolder(database)
        database.remoteFileDao().upsertAll(
            listOf(
                remoteFile(id = 10, folderId = folderId, path = "/Music/moon.flac"),
                remoteFile(id = 11, folderId = folderId, path = "/Music/deleted-moon.flac", isDeleted = true),
            ),
        )
        database.trackDao().upsertAll(
            listOf(
                track(id = 1, remoteFileId = 10, title = "Moon", artist = "Luna"),
                track(id = 2, remoteFileId = null, title = "Moonlight Sonata", composer = "Beethoven"),
                track(id = 3, remoteFileId = 11, title = "Deleted Moon", artist = "Luna"),
            ),
        )

        val results = repository(database).searchLocalLibrary("moon")

        assertEquals(listOf(1L, 2L), results.tracks.map { it.id })
        assertEquals(listOf("Moon", "Moonlight Sonata"), results.tracks.map { it.title })
        assertEquals(listOf("Luna", "Beethoven"), results.tracks.map { it.artist })
    }

    @Test
    fun searchLocalLibraryTreatsSqlWildcardCharactersLiterally() = withDatabase { database ->
        database.trackDao().upsertAll(
            listOf(
                track(id = 1, remoteFileId = null, title = "100% Real"),
                track(id = 2, remoteFileId = null, title = "100x Real"),
                track(id = 3, remoteFileId = null, title = "100_ Real"),
            ),
        )

        val results = repository(database).searchLocalLibrary("100%")

        assertEquals(listOf(1L), results.tracks.map { it.id })
    }

    @Test
    fun suggestLocalLibraryUsesTrackFieldsAndSkipsDeletedRemoteFiles() = withDatabase { database ->
        val folderId = seedStorageAndFolder(database)
        database.remoteFileDao().upsertAll(
            listOf(
                remoteFile(id = 20, folderId = folderId, path = "/Music/moon.flac"),
                remoteFile(id = 21, folderId = folderId, path = "/Music/deleted-moon.flac", isDeleted = true),
            ),
        )
        database.trackDao().upsertAll(
            listOf(
                track(id = 1, remoteFileId = 20, title = "Moon", artist = "Luna"),
                track(id = 2, remoteFileId = null, title = "Moonlight Sonata", composer = "Momo"),
                track(id = 3, remoteFileId = 21, title = "Moon Deleted"),
                track(id = 4, remoteFileId = null, title = "Sun", artist = "Moon"),
            ),
        )

        val suggestions = repository(database).suggestLocalLibrary("mo", limit = 10)

        assertEquals(listOf("Moon", "Moonlight Sonata", "Momo"), suggestions)
    }

    private fun withDatabase(block: suspend (TideTunesDatabase) -> Unit) = runBlocking {
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

    private fun repository(database: TideTunesDatabase): RoomSearchRepository {
        return RoomSearchRepository(
            trackDao = database.trackDao(),
            trackFtsDao = database.trackFtsDao(),
            storageLookup = LegacyStorageLookup { null },
        )
    }

    private suspend fun seedStorageAndFolder(database: TideTunesDatabase): Long {
        database.storageDao().upsert(
            StorageEntity(
                id = 1,
                type = "WEBDAV",
                displayName = "Test",
                baseUrl = "https://example.invalid/dav",
                credentialRef = "test-credential",
                username = "",
                isAnonymous = true,
                musicCount = 0,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        database.selectedFolderDao().upsert(
            SelectedFolderEntity(
                storageId = 1,
                remoteId = "folder-1",
                canonicalPath = "/Music",
                displayPath = "/Music",
                deltaLink = null,
                lastSyncAt = null,
                syncStatus = "RUNNING",
            ),
        )
        return requireNotNull(database.selectedFolderDao().findByPath(1, "/Music")).id
    }

    private fun remoteFile(
        id: Long,
        folderId: Long,
        path: String,
        isDeleted: Boolean = false,
    ) = RemoteFileEntity(
        id = id,
        storageId = 1,
        selectedFolderId = folderId,
        remoteId = "item-$id",
        parentRemoteId = "folder-1",
        canonicalPath = path,
        displayPath = path,
        fileName = path.substringAfterLast('/'),
        extension = "flac",
        mimeType = "audio/flac",
        size = 1_000,
        etag = "\"etag-$id\"",
        ctag = null,
        createdAt = 1,
        modifiedAt = 1,
        contentHash = null,
        isDeleted = isDeleted,
        lastSeenScanId = "scan-1",
    )

    private fun track(
        id: Long,
        remoteFileId: Long?,
        title: String,
        artist: String? = null,
        composer: String? = null,
    ) = TrackEntity(
        id = id,
        remoteFileId = remoteFileId,
        title = title,
        sortTitle = null,
        albumId = null,
        albumArtist = null,
        composer = composer,
        comment = null,
        grouping = null,
        durationMs = 180_000,
        discNumber = 1,
        discTotal = 1,
        trackNumber = null,
        trackTotal = null,
        year = 2026,
        date = "2026",
        sampleRate = 48_000,
        bitRate = 900_000,
        bitsPerSample = 24,
        channels = 2,
        channelLayout = null,
        codec = "FLAC",
        container = "FLAC",
        lossless = true,
        createdAt = 1,
        updatedAt = 1,
        artist = artist,
    )
}
