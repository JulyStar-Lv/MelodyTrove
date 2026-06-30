package com.github.tidetunes.source.storage

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.database.RemoteFileEntity
import com.github.tidetunes.database.SelectedFolderEntity
import com.github.tidetunes.database.StorageEntity
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.LegacyStorageKind
import com.github.tidetunes.source.api.SourceSearchFailureReason
import com.github.tidetunes.source.api.SourceSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class RoomLegacyStorageSearchProviderIntegrationTest {
    @Test
    fun searchesIndexedTracksForExpectedStorageOnly() = withDatabase { database ->
        val webDavFolderId = seedStorageAndFolder(database, storageId = 1, type = StorageType.WEBDAV)
        val oneDriveFolderId = seedStorageAndFolder(database, storageId = 2, type = StorageType.ONE_DRIVE)
        database.remoteFileDao().upsertAll(
            listOf(
                remoteFile(
                    id = 101,
                    storageId = 1,
                    folderId = webDavFolderId,
                    path = "/Music/Moon.flac",
                    isDeleted = false,
                ),
                remoteFile(
                    id = 102,
                    storageId = 1,
                    folderId = webDavFolderId,
                    path = "/Music/Sun.flac",
                    isDeleted = false,
                ),
                remoteFile(
                    id = 103,
                    storageId = 1,
                    folderId = webDavFolderId,
                    path = "/Music/Deleted Moon.flac",
                    isDeleted = true,
                ),
                remoteFile(
                    id = 201,
                    storageId = 2,
                    folderId = oneDriveFolderId,
                    path = "/Cloud/Moon.flac",
                    isDeleted = false,
                ),
            )
        )
        database.trackDao().upsertAll(
            listOf(
                track(id = 1, remoteFileId = 101, sourceStorageId = 1, title = "Moonlight"),
                track(id = 2, remoteFileId = 102, sourceStorageId = 1, title = "Sunrise"),
                track(id = 3, remoteFileId = 103, sourceStorageId = 1, title = "Deleted Moon"),
                track(id = 4, remoteFileId = 201, sourceStorageId = 2, title = "Moon Cloud"),
            )
        )
        val provider = RoomLegacyStorageSearchProvider(
            storageLookup = { storageId ->
                when (storageId.value) {
                    1L -> storage(storageId.value, StorageType.WEBDAV)
                    2L -> storage(storageId.value, StorageType.ONE_DRIVE)
                    else -> null
                }
            },
            trackDao = database.trackDao(),
        )

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = "moon",
            limit = 10,
            expectedStorageKind = LegacyStorageKind.WebDav,
            sourceId = BuiltInSourceIds.WebDav,
        )

        val items = assertIs<SourceSearchResult.Success>(result).items
        assertEquals(listOf("Moonlight"), items.map { it.title })
        val item = items.single()
        assertEquals(SourceAccountId("storage:1"), item.accountId)
        assertEquals(BuiltInSourceIds.WebDav, item.mediaId.sourceId)
        assertEquals(MediaType.Track, item.mediaId.mediaType)
        assertEquals("/Music/Moon.flac", item.path)
        assertEquals("Luna", item.artist)
        assertEquals(180_000, item.durationMs)
    }

    @Test
    fun rejectsAccountsWithUnexpectedStorageType() = withDatabase { database ->
        val provider = RoomLegacyStorageSearchProvider(
            storageLookup = { storage(1, StorageType.LOCAL) },
            trackDao = database.trackDao(),
        )

        assertEquals(
            SourceSearchResult.Failure(SourceSearchFailureReason.UnsupportedAccount),
            provider.search(
                accountId = SourceAccountId("storage:1"),
                query = "moon",
                limit = 10,
                expectedStorageKind = LegacyStorageKind.WebDav,
                sourceId = BuiltInSourceIds.WebDav,
            ),
        )
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

    private suspend fun seedStorageAndFolder(
        database: TideTunesDatabase,
        storageId: Long,
        type: StorageType,
    ): Long {
        database.storageDao().upsert(
            StorageEntity(
                id = storageId,
                type = type.name,
                displayName = "Storage $storageId",
                baseUrl = "",
                driveId = null,
                credentialRef = "storage-$storageId",
                username = "",
                isAnonymous = true,
                musicCount = 0,
                createdAt = 1,
                updatedAt = 1,
            )
        )
        val path = "/Root-$storageId"
        database.selectedFolderDao().upsert(
            SelectedFolderEntity(
                storageId = storageId,
                remoteId = "folder-$storageId",
                canonicalPath = path,
                displayPath = path,
                deltaLink = null,
                lastSyncAt = null,
                syncStatus = "COMPLETED",
            )
        )
        return assertNotNull(database.selectedFolderDao().findByPath(storageId, path)).id
    }

    private fun remoteFile(
        id: Long,
        storageId: Long,
        folderId: Long,
        path: String,
        isDeleted: Boolean,
    ) = RemoteFileEntity(
        id = id,
        storageId = storageId,
        selectedFolderId = folderId,
        remoteId = "item-$id",
        parentRemoteId = "folder-$storageId",
        canonicalPath = path,
        displayPath = path,
        fileName = path.substringAfterLast('/'),
        extension = path.substringAfterLast('.', missingDelimiterValue = ""),
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
        remoteFileId: Long,
        sourceStorageId: Long,
        title: String,
    ) = TrackEntity(
        id = id,
        remoteFileId = remoteFileId,
        sourceStorageId = sourceStorageId,
        sourcePath = null,
        title = title,
        sortTitle = null,
        albumId = null,
        albumArtist = null,
        composer = null,
        comment = null,
        grouping = null,
        durationMs = 180_000,
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
        artist = "Luna",
    )

    private fun storage(
        id: Long,
        type: StorageType,
    ) = Storage(
        id = StorageId(id),
        addr = "",
        alias = "Storage $id",
        username = "",
        password = "",
        isAnonymous = true,
        typ = type,
        musicCount = 0u,
    )
}
