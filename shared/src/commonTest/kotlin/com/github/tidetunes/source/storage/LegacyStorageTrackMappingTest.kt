package com.github.tidetunes.source.storage

import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.source.api.BuiltInSourceIds
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType

class LegacyStorageTrackMappingTest {
    @Test
    fun mapsRoomTrackSourceFieldsToLegacyStorageMediaId() = runBlocking {
        val mediaId = track(
            sourceStorageId = 2,
            sourcePath = "/Music/Song.flac",
        ).toLegacyStorageTrackMediaIdOrNull(
            LegacyStorageLookup { storageId ->
                storage(id = storageId.value, typ = StorageType.WEBDAV)
            }
        )

        assertEquals(BuiltInSourceIds.WebDav, mediaId?.sourceId)
        assertEquals(
            "legacy-storage-track:storage%3A2:%2FMusic%2FSong.flac",
            mediaId?.remoteId,
        )
    }

    @Test
    fun mapsRawSourceFieldsToEachBuiltInLegacyStorageMediaId() = runBlocking {
        val cases = listOf(
            StorageType.LOCAL to BuiltInSourceIds.Local,
            StorageType.WEBDAV to BuiltInSourceIds.WebDav,
            StorageType.ONE_DRIVE to BuiltInSourceIds.OneDrive,
        )

        cases.forEach { (storageType, sourceId) ->
            val mediaId = legacyStorageTrackMediaIdOrNull(
                storageLookup = LegacyStorageLookup { storageId ->
                    storage(id = storageId.value, typ = storageType)
                },
                sourceStorageId = 5,
                sourcePath = "/Music/Song.flac",
            )

            assertEquals(sourceId, mediaId?.sourceId)
            assertEquals(
                "legacy-storage-track:storage%3A5:%2FMusic%2FSong.flac",
                mediaId?.remoteId,
            )
        }
    }

    @Test
    fun missingSourceFieldsDoNotCreateMediaId() = runBlocking {
        assertNull(
            track(
                sourceStorageId = null,
                sourcePath = "/Music/Song.flac",
            ).toLegacyStorageTrackMediaIdOrNull(LegacyStorageLookup { null })
        )
        assertNull(
            track(
                sourceStorageId = 2,
                sourcePath = " ",
            ).toLegacyStorageTrackMediaIdOrNull(LegacyStorageLookup { null })
        )
        assertNull(
            legacyStorageTrackMediaIdOrNull(
                storageLookup = LegacyStorageLookup { null },
                sourceStorageId = 2,
                sourcePath = "/Music/Song.flac",
            )
        )
    }

    private fun storage(
        id: Long,
        typ: StorageType,
    ) = Storage(
        id = StorageId(id),
        addr = "https://example.com",
        alias = "NAS",
        username = "alice",
        password = "",
        isAnonymous = true,
        typ = typ,
        musicCount = 0u,
    )

    private fun track(
        sourceStorageId: Long?,
        sourcePath: String?,
    ) = TrackEntity(
        id = 99,
        remoteFileId = 7,
        sourceStorageId = sourceStorageId,
        sourcePath = sourcePath,
        title = "Song",
        sortTitle = null,
        albumId = null,
        albumArtist = null,
        composer = null,
        comment = null,
        grouping = null,
        durationMs = 123_000,
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
        updatedAt = 2,
    )
}
