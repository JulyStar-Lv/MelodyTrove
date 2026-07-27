package com.github.tidetunes.source.storage

import com.github.tidetunes.source.api.BuiltInSourceIds
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import uniffi.tidetunes_backend.Storage
import uniffi.tidetunes_backend.StorageId
import uniffi.tidetunes_backend.StorageType

class LegacyStorageTrackMappingTest {
    @Test
    fun mapsRawSourceFieldsToEachBuiltInLegacyStorageMediaId() = runBlocking {
        val cases = listOf(
            StorageType.LOCAL to BuiltInSourceIds.Local,
            StorageType.WEBDAV to BuiltInSourceIds.WebDav,
            StorageType.ONE_DRIVE to BuiltInSourceIds.OneDrive,
            StorageType.SMB to BuiltInSourceIds.Smb,
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
}
