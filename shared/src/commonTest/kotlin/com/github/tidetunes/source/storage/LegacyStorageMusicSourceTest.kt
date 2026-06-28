package com.github.tidetunes.source.storage

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.LocalSourceConfiguration
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.OneDriveSourceConfiguration
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceListFailureReason
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourceNodeType
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchResult
import com.github.tidetunes.source.api.WebDavSourceConfiguration
import com.github.tidetunes.source.local.LocalMusicSource
import com.github.tidetunes.source.onedrive.OneDriveMusicSource
import com.github.tidetunes.source.webdav.WebDavMusicSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import uniffi.tidetunes_core.ArgUpsertStorage
import uniffi.tidetunes_core.ListStorageEntryChildrenResp
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageConnectionTestResult
import uniffi.tidetunes_core.StorageEntry
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType

class LegacyStorageMusicSourceTest {
    @Test
    fun webDavAuthenticationUsesLegacyStorageConnectionTest() = runBlocking {
        var captured: ArgUpsertStorage? = null
        val source = WebDavMusicSource(
            connectionTester = LegacyStorageConnectionTester { arg ->
                captured = arg
                StorageConnectionTestResult.SUCCESS
            },
            directoryLister = unusedDirectoryLister(),
            playbackResolver = unusedPlaybackResolver(),
        )

        val result = source.authenticate(
            WebDavSourceConfiguration(
                accountId = SourceAccountId("account-webdav"),
                alias = "NAS",
                address = "https://dav.example.com/music",
                username = "alice",
                password = "secret-password",
                isAnonymous = false,
            )
        )

        assertEquals(SourceAuthResult.Success, result)
        val arg = assertNotNull(captured)
        assertNull(arg.id)
        assertEquals("https://dav.example.com/music", arg.addr)
        assertEquals("NAS", arg.alias)
        assertEquals("alice", arg.username)
        assertEquals("secret-password", arg.password)
        assertFalse(arg.isAnonymous)
        assertEquals(StorageType.WEBDAV, arg.typ)
    }

    @Test
    fun oneDriveAuthenticationMapsLegacyFailure() = runBlocking {
        var captured: ArgUpsertStorage? = null
        val source = OneDriveMusicSource(
            connectionTester = LegacyStorageConnectionTester { arg ->
                captured = arg
                StorageConnectionTestResult.UNAUTHORIZED
            },
            directoryLister = unusedDirectoryLister(),
            playbackResolver = unusedPlaybackResolver(),
        )

        val result = source.authenticate(
            OneDriveSourceConfiguration(
                accountId = SourceAccountId("account-onedrive"),
                alias = "OneDrive",
                driveId = "drive-123",
                refreshToken = "refresh-token",
            )
        )

        assertEquals(
            SourceAuthResult.Failure(SourceAuthFailureReason.Unauthorized),
            result,
        )
        val arg = assertNotNull(captured)
        assertNull(arg.id)
        assertEquals("drive-123", arg.addr)
        assertEquals("OneDrive", arg.alias)
        assertEquals("", arg.username)
        assertEquals("refresh-token", arg.password)
        assertFalse(arg.isAnonymous)
        assertEquals(StorageType.ONE_DRIVE, arg.typ)
    }

    @Test
    fun localAuthenticationAcceptsOnlyLocalConfiguration() = runBlocking {
        val source = LocalMusicSource(unusedDirectoryLister(), unusedPlaybackResolver())

        assertEquals(
            SourceAuthResult.Success,
            source.authenticate(LocalSourceConfiguration(alias = "Device")),
        )
        assertEquals(
            SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration),
            source.authenticate(
                WebDavSourceConfiguration(
                    alias = "NAS",
                    address = "https://dav.example.com/music",
                    username = "alice",
                    password = "secret-password",
                    isAnonymous = false,
                )
            ),
        )
    }

    @Test
    fun sourceConfigurationStringDoesNotExposeSecrets() {
        val webDavConfiguration = WebDavSourceConfiguration(
            alias = "NAS",
            address = "https://dav.example.com/music",
            username = "alice",
            password = "secret-password",
            isAnonymous = false,
        )
        val oneDriveConfiguration = OneDriveSourceConfiguration(
            alias = "OneDrive",
            driveId = "drive-123",
            refreshToken = "refresh-token",
        )

        assertFalse(webDavConfiguration.toString().contains("secret-password"))
        assertFalse(oneDriveConfiguration.toString().contains("refresh-token"))
    }

    @Test
    fun musicSourcesListWithExpectedLegacyStorageTypes() = runBlocking {
        val accountId = SourceAccountId("storage:42")
        val captured = mutableListOf<StorageType>()
        val directoryLister = LegacyStorageDirectoryLister { _, _, expectedType ->
            captured += expectedType
            SourceListResult.Success(emptyList())
        }

        LocalMusicSource(directoryLister, unusedPlaybackResolver()).list(accountId, "/")
        WebDavMusicSource(
            unusedConnectionTester(),
            directoryLister,
            unusedPlaybackResolver(),
        ).list(accountId, "/Music")
        OneDriveMusicSource(
            unusedConnectionTester(),
            directoryLister,
            unusedPlaybackResolver(),
        ).list(accountId, "Albums")

        assertEquals(
            listOf(StorageType.LOCAL, StorageType.WEBDAV, StorageType.ONE_DRIVE),
            captured,
        )
    }

    @Test
    fun builtInStorageSourcesAdvertiseSearchAndDelegateToLegacySearchProvider() = runBlocking {
        val accountId = SourceAccountId("storage:42")
        val captured = mutableListOf<Pair<StorageType, String>>()
        val searchProvider = LegacyStorageSearchProvider { _, query, limit, expectedType, sourceId ->
            assertEquals("moon", query)
            assertEquals(5, limit)
            captured += expectedType to sourceId.value
            SourceSearchResult.Success(emptyList())
        }
        val sources: List<MusicSource> = listOf(
            LocalMusicSource(
                unusedDirectoryLister(),
                unusedPlaybackResolver(),
                searchProvider,
            ),
            WebDavMusicSource(
                unusedConnectionTester(),
                unusedDirectoryLister(),
                unusedPlaybackResolver(),
                searchProvider,
            ),
            OneDriveMusicSource(
                unusedConnectionTester(),
                unusedDirectoryLister(),
                unusedPlaybackResolver(),
                searchProvider,
            ),
        )

        sources.forEach { source ->
            assertTrue(SourceCapability.Search in source.capabilities)
            assertEquals(
                SourceSearchResult.Success(emptyList()),
                source.search(accountId, query = "moon", limit = 5),
            )
        }
        assertEquals(
            listOf(
                StorageType.LOCAL to BuiltInSourceIds.Local.value,
                StorageType.WEBDAV to BuiltInSourceIds.WebDav.value,
                StorageType.ONE_DRIVE to BuiltInSourceIds.OneDrive.value,
            ),
            captured,
        )
    }

    @Test
    fun musicSourcesResolvePlaybackWithExpectedLegacyStorageTypes() = runBlocking {
        val accountId = SourceAccountId("storage:42")
        val captured = mutableListOf<Triple<SourceAccountId, String, StorageType>>()
        val playbackResolver = object : LegacyStoragePlaybackResolver {
            override suspend fun resolve(
                accountId: SourceAccountId,
                path: String,
                expectedStorageType: StorageType,
            ): SourcePlaybackResult {
                captured += Triple(accountId, path, expectedStorageType)
                return SourcePlaybackResult.Success(
                    PlaybackResource(
                        uri = "http://127.0.0.1/${expectedStorageType.name}",
                        isLocal = expectedStorageType == StorageType.LOCAL,
                    )
                )
            }

            override suspend fun release(uri: String) = Unit

            override suspend fun releaseAll() = Unit
        }

        val localResult = LocalMusicSource(
            unusedDirectoryLister(),
            playbackResolver,
        ).resolvePlayback(
            legacyStorageTrackMediaId(BuiltInSourceIds.Local, accountId, "/Local.flac")
        )
        WebDavMusicSource(
            unusedConnectionTester(),
            unusedDirectoryLister(),
            playbackResolver,
        ).resolvePlayback(
            legacyStorageTrackMediaId(BuiltInSourceIds.WebDav, accountId, "/Music/WebDAV.mp3")
        )
        OneDriveMusicSource(
            unusedConnectionTester(),
            unusedDirectoryLister(),
            playbackResolver,
        ).resolvePlayback(
            legacyStorageTrackMediaId(BuiltInSourceIds.OneDrive, accountId, "/Cloud/OneDrive.m4a")
        )

        assertEquals(
            listOf(
                Triple(accountId, "/Local.flac", StorageType.LOCAL),
                Triple(accountId, "/Music/WebDAV.mp3", StorageType.WEBDAV),
                Triple(accountId, "/Cloud/OneDrive.m4a", StorageType.ONE_DRIVE),
            ),
            captured,
        )
        assertTrue((localResult as SourcePlaybackResult.Success).resource.isLocal)
    }

    @Test
    fun musicSourcesRejectUnsupportedPlaybackMediaIdsWithoutCallingResolver() = runBlocking {
        var resolveCalls = 0
        val playbackResolver = object : LegacyStoragePlaybackResolver {
            override suspend fun resolve(
                accountId: SourceAccountId,
                path: String,
                expectedStorageType: StorageType,
            ): SourcePlaybackResult {
                resolveCalls += 1
                return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)
            }

            override suspend fun release(uri: String) = Unit

            override suspend fun releaseAll() = Unit
        }
        val source = WebDavMusicSource(
            unusedConnectionTester(),
            unusedDirectoryLister(),
            playbackResolver,
        )

        assertEquals(
            SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaId),
            source.resolvePlayback(
                MediaId(
                    sourceId = BuiltInSourceIds.OneDrive,
                    mediaType = MediaType.Track,
                    remoteId = "legacy-storage-track:storage%3A42:%2FTrack.mp3",
                )
            ),
        )
        assertEquals(
            SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaType),
            source.resolvePlayback(
                MediaId(
                    sourceId = BuiltInSourceIds.WebDav,
                    mediaType = MediaType.Album,
                    remoteId = "legacy-storage-track:storage%3A42:%2FTrack.mp3",
                )
            ),
        )
        assertEquals(0, resolveCalls)
    }

    @Test
    fun retainedLegacyPlaybackResolverKeepsSessionUntilReleased() = runBlocking {
        val session = FakeLegacyPlaybackSession("http://127.0.0.1:1234/stream.flac")
        val resolver = RetainedLegacyStoragePlaybackResolver(
            storageLookup = LegacyStorageLookup { storageId ->
                if (storageId == StorageId(42)) {
                    storage(id = 42, typ = StorageType.WEBDAV)
                } else {
                    null
                }
            },
            sessionFactory = LegacyPlaybackSessionFactory { storage, path ->
                assertEquals(StorageId(42), storage.id)
                assertEquals("/Music/Track.flac", path)
                session
            },
        )

        val result = resolver.resolve(
            accountId = SourceAccountId("storage:42"),
            path = "/Music/Track.flac",
            expectedStorageType = StorageType.WEBDAV,
        )
        val resource = (result as SourcePlaybackResult.Success).resource

        assertEquals("http://127.0.0.1:1234/stream.flac", resource.uri)
        assertEquals("audio/flac", resource.mimeType)
        assertFalse(resource.isLocal)
        assertEquals(0, session.shutdownCalls)

        resolver.release(resource.uri)

        assertEquals(1, session.shutdownCalls)
    }

    @Test
    fun retainedLegacyPlaybackResolverRejectsWrongStorageTypeBeforeCreatingSession() = runBlocking {
        var createCalls = 0
        val resolver = RetainedLegacyStoragePlaybackResolver(
            storageLookup = LegacyStorageLookup {
                storage(id = 42, typ = StorageType.LOCAL)
            },
            sessionFactory = LegacyPlaybackSessionFactory { _, _ ->
                createCalls += 1
                FakeLegacyPlaybackSession("http://127.0.0.1:1234/stream.mp3")
            },
        )

        assertEquals(
            SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedAccount),
            resolver.resolve(
                accountId = SourceAccountId("storage:42"),
                path = "/Music/Track.mp3",
                expectedStorageType = StorageType.WEBDAV,
            ),
        )
        assertEquals(0, createCalls)
    }

    @Test
    fun legacyDirectoryResponseMapsToSourceNodes() {
        val accountId = SourceAccountId("storage:42")

        val result = ListStorageEntryChildrenResp.Ok(
            listOf(
                storageEntry(
                    name = "Albums",
                    path = "/Albums",
                    isDir = true,
                    remoteId = "folder-1",
                    parentRemoteId = "root",
                    ctag = "folder-ctag",
                ),
                storageEntry(
                    name = "Song.FLAC",
                    path = "/Albums/Song.FLAC",
                    isDir = false,
                    size = 123u,
                    remoteId = "track-1",
                    parentRemoteId = "folder-1",
                    mimeType = "audio/flac",
                    etag = "track-etag",
                    modifiedAt = 1_000,
                ),
                storageEntry(
                    name = "WebDAV.mp3",
                    path = "/Albums/WebDAV.mp3",
                    isDir = false,
                ),
            )
        ).toSourceListResult(accountId)

        val nodes = (result as SourceListResult.Success).nodes
        assertEquals(3, nodes.size)
        assertEquals(SourceNodeType.Folder, nodes[0].type)
        assertEquals("folder-1", nodes[0].nodeId)
        assertEquals("folder-1", nodes[0].remoteId)
        assertEquals("root", nodes[0].parentNodeId)
        assertEquals("folder-ctag", nodes[0].ctag)
        assertEquals(SourceNodeType.Track, nodes[1].type)
        assertEquals("track-1", nodes[1].nodeId)
        assertEquals("track-1", nodes[1].remoteId)
        assertEquals("audio/flac", nodes[1].mimeType)
        assertEquals("track-etag", nodes[1].etag)
        assertEquals(1_000, nodes[1].modifiedAtEpochMs)
        assertEquals("/Albums/WebDAV.mp3", nodes[2].nodeId)
        assertNull(nodes[2].remoteId)
    }

    @Test
    fun legacyDirectoryFailuresMapToSourceListFailures() {
        assertEquals(
            SourceListResult.Failure(SourceListFailureReason.Unauthorized),
            ListStorageEntryChildrenResp.AuthenticationFailed.toSourceListResult(
                SourceAccountId("storage:1")
            ),
        )
        assertEquals(
            SourceListResult.Failure(SourceListFailureReason.Timeout),
            ListStorageEntryChildrenResp.Timeout.toSourceListResult(SourceAccountId("storage:1")),
        )
        assertEquals(
            SourceListResult.Failure(SourceListFailureReason.Unknown),
            ListStorageEntryChildrenResp.Unknown.toSourceListResult(SourceAccountId("storage:1")),
        )
    }

    private fun unusedConnectionTester() = LegacyStorageConnectionTester {
        StorageConnectionTestResult.OTHER_ERROR
    }

    private fun unusedDirectoryLister() = LegacyStorageDirectoryLister { _, _, _ ->
        SourceListResult.Failure(SourceListFailureReason.Unavailable)
    }

    private fun unusedPlaybackResolver() = object : LegacyStoragePlaybackResolver {
        override suspend fun resolve(
            accountId: SourceAccountId,
            path: String,
            expectedStorageType: StorageType,
        ): SourcePlaybackResult {
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)
        }

        override suspend fun release(uri: String) = Unit

        override suspend fun releaseAll() = Unit
    }

    private fun storage(
        id: Long,
        typ: StorageType,
    ) = Storage(
        id = StorageId(id),
        addr = "",
        alias = "Storage",
        username = "",
        password = "",
        isAnonymous = true,
        typ = typ,
        musicCount = 0u,
    )

    private class FakeLegacyPlaybackSession(
        override val url: String,
    ) : LegacyPlaybackSession {
        var shutdownCalls = 0
            private set

        override fun shutdown() {
            shutdownCalls += 1
        }
    }

    private fun storageEntry(
        name: String,
        path: String,
        isDir: Boolean,
        size: ULong? = null,
        remoteId: String? = null,
        parentRemoteId: String? = null,
        mimeType: String? = null,
        etag: String? = null,
        ctag: String? = null,
        createdAt: Long? = null,
        modifiedAt: Long? = null,
    ) = StorageEntry(
        storageId = uniffi.tidetunes_core.StorageId(42),
        name = name,
        path = path,
        size = size,
        isDir = isDir,
        remoteId = remoteId,
        parentRemoteId = parentRemoteId,
        mimeType = mimeType,
        etag = etag,
        ctag = ctag,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )
}
