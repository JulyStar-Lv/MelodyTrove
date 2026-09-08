package io.github.julystar.musicapp.source.storage

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.source.api.BuiltInSourceIds
import io.github.julystar.musicapp.source.api.LegacyStorageKind
import io.github.julystar.musicapp.source.api.SourceSearchFailureReason
import io.github.julystar.musicapp.source.api.SourceSearchResult
import kotlinx.coroutines.runBlocking
import uniffi.app_backend.ListStorageEntryChildrenResp
import uniffi.app_backend.Storage
import uniffi.app_backend.StorageEntry
import uniffi.app_backend.StorageId
import uniffi.app_backend.StorageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LiveStorageSearchProviderTest {
    @Test
    fun findsMusicFilesByCaseInsensitiveName() = runBlocking {
        val provider = providerWithFiles(1L, mapOf(
            "/" to listOf(
                entry(isDir = true, name = "Music", path = "/Music"),
            ),
            "/Music" to listOf(
                entry(isDir = false, name = "Summer.mp3", path = "/Music/Summer.mp3"),
                entry(isDir = false, name = "Winter Blues.flac", path = "/Music/Winter Blues.flac"),
                entry(isDir = false, name = "Cover.jpg", path = "/Music/Cover.jpg"),
            ),
        ))

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = "blues",
            limit = 10,
            expectedStorageKind = LegacyStorageKind.Local,
            sourceId = BuiltInSourceIds.Local,
        )

        val items = assertIs<SourceSearchResult.Success>(result).items
        assertEquals(1, items.size)
        assertEquals("Winter Blues.flac", items.single().title)
        assertEquals("/Music/Winter Blues.flac", items.single().path)
    }

    @Test
    fun returnsEmptyForNoMatches() = runBlocking {
        val provider = providerWithFiles(1L, mapOf(
            "/" to listOf(
                entry(isDir = false, name = "Song.mp3", path = "/Song.mp3"),
            ),
        ))

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = "zzz_no_match",
            limit = 10,
            expectedStorageKind = LegacyStorageKind.Local,
            sourceId = BuiltInSourceIds.Local,
        )

        assertTrue((result as SourceSearchResult.Success).items.isEmpty()); Unit
    }

    @Test
    fun respectsLimit() = runBlocking {
        val provider = providerWithFiles(1L, mapOf(
            "/" to listOf(
                entry(isDir = false, name = "a_song.mp3", path = "/a_song.mp3"),
                entry(isDir = false, name = "b_song.flac", path = "/b_song.flac"),
                entry(isDir = false, name = "c_song.ogg", path = "/c_song.ogg"),
            ),
        ))

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = "song",
            limit = 2,
            expectedStorageKind = LegacyStorageKind.Local,
            sourceId = BuiltInSourceIds.Local,
        )

        assertEquals(2, (result as SourceSearchResult.Success).items.size)
    }

    @Test
    fun supportsCompleteRemoteAudioSetAndRejectsVideoMp4() = runBlocking {
        val provider = providerWithFiles(1L, mapOf(
            "/" to listOf(
                entry(isDir = false, name = "album.ape", path = "/album.ape"),
                entry(isDir = false, name = "album.wv", path = "/album.wv"),
                entry(isDir = false, name = "album.aiff", path = "/album.aiff"),
                entry(
                    isDir = false,
                    name = "album.mp4",
                    path = "/album.mp4",
                    mimeType = "audio/mp4",
                ),
                entry(
                    isDir = false,
                    name = "video.mp4",
                    path = "/video.mp4",
                    mimeType = "video/mp4",
                ),
            ),
        ))

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = ".",
            limit = 10,
            expectedStorageKind = LegacyStorageKind.Local,
            sourceId = BuiltInSourceIds.Local,
        )

        assertEquals(
            listOf("album.ape", "album.wv", "album.aiff", "album.mp4"),
            (result as SourceSearchResult.Success).items.map { it.title },
        )
    }

    @Test
    fun skipsHiddenDirectories() = runBlocking {
        val provider = providerWithFiles(1L, mapOf(
            "/" to listOf(
                entry(isDir = true, name = ".hidden", path = "/.hidden"),
                entry(isDir = false, name = "visible.mp3", path = "/visible.mp3"),
            ),
            "/.hidden" to listOf(
                entry(isDir = false, name = "secret.mp3", path = "/.hidden/secret.mp3"),
            ),
        ))

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = "mp3",
            limit = 10,
            expectedStorageKind = LegacyStorageKind.Local,
            sourceId = BuiltInSourceIds.Local,
        )

        val items = (result as SourceSearchResult.Success).items
        assertEquals(listOf("visible.mp3"), items.map { it.title })
    }

    @Test
    fun rejectsMismatchedStorageType() = runBlocking {
        val provider = providerWithFiles(1L, emptyMap())

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = "song",
            limit = 10,
            expectedStorageKind = LegacyStorageKind.WebDav,
            sourceId = BuiltInSourceIds.WebDav,
        )

        assertIs<SourceSearchResult.Failure>(result); Unit
    }

    @Test
    fun returnsBlankQueryAsEmpty() = runBlocking {
        val provider = providerWithFiles(1L, emptyMap())

        val result = provider.search(
            accountId = SourceAccountId("storage:1"),
            query = "   ",
            limit = 10,
            expectedStorageKind = LegacyStorageKind.Local,
            sourceId = BuiltInSourceIds.Local,
        )

        assertTrue((result as SourceSearchResult.Success).items.isEmpty()); Unit
    }

    private fun providerWithFiles(
        storageIdValue: Long,
        entriesByPath: Map<String, List<StorageEntry>>,
    ): LiveStorageSearchProvider {
        val directoryLister = object : StorageDirectoryLister {
            override suspend fun listDirectory(
                storageId: StorageId,
                path: String,
            ): ListStorageEntryChildrenResp {
                val normalized = if (path == "/") "/" else path.trimEnd('/')
                val children = entriesByPath[normalized].orEmpty()
                return ListStorageEntryChildrenResp.Ok(children)
            }
        }
        val storageLookup = LiveStorageLookup { id ->
            if (id.value == storageIdValue) {
                Storage(
                    id = StorageId(storageIdValue),
                    addr = "/test",
                    alias = "Test",
                    username = "",
                    password = "",
                    isAnonymous = true,
                    typ = StorageType.LOCAL,
                    musicCount = 0u,
                )
            } else {
                null
            }
        }
        return LiveStorageSearchProvider(directoryLister, storageLookup)
    }

    private fun entry(
        isDir: Boolean,
        name: String,
        path: String,
        mimeType: String? = null,
    ): StorageEntry {
        return StorageEntry(
            storageId = StorageId(1L),
            name = name,
            path = path,
            size = if (isDir) null else 1000uL,
            isDir = isDir,
            remoteId = name,
            parentRemoteId = null,
            mimeType = mimeType,
            etag = null,
            ctag = null,
            createdAt = null,
            modifiedAt = null,
        )
    }
}
