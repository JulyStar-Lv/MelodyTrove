package com.github.tidetunes.source.smb

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.LegacyStorageConnectionRequest
import com.github.tidetunes.source.api.LegacyStorageConnectionTester
import com.github.tidetunes.source.api.LegacyStorageDirectoryLister
import com.github.tidetunes.source.api.LegacyStorageKind
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.LegacyStorageSearchProvider
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.SmbSourceConfiguration
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchResult
import com.github.tidetunes.source.api.legacyStorageTrackMediaId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SmbMusicSourceTest {
    @Test
    fun configurationBuildsCredentialFreeCanonicalAddress() {
        val address = configuration().copy(
            rootPath = "音乐/Hi Res",
            domain = "MY WORKGROUP",
            requireSigning = true,
            requireEncryption = true,
        ).toSmbAddress()

        assertEquals(
            "smb://nas.local/Music/%E9%9F%B3%E4%B9%90/Hi%20Res" +
                "?domain=MY%20WORKGROUP&signing=true&encryption=true",
            address,
        )
        assertTrue("alice" !in address)
        assertTrue("secret" !in address)
    }

    @Test
    fun authenticateAndListUseSmbStorageKind() = runTest {
        var request: LegacyStorageConnectionRequest? = null
        var listedKind: LegacyStorageKind? = null
        val source = source(
            tester = LegacyStorageConnectionTester {
                request = it
                SourceAuthResult.Success
            },
            lister = LegacyStorageDirectoryLister { _, _, kind ->
                listedKind = kind
                SourceListResult.Success(emptyList())
            },
        )

        assertIs<SourceAuthResult.Success>(source.authenticate(configuration()))
        source.list(SourceAccountId("storage:7"))

        assertEquals(LegacyStorageKind.Smb, request?.kind)
        assertEquals(LegacyStorageKind.Smb, listedKind)
    }

    @Test
    fun guestAuthenticationClearsCredentialsAndDebugOutputRedactsPassword() = runTest {
        var request: LegacyStorageConnectionRequest? = null
        val source = source(
            tester = LegacyStorageConnectionTester {
                request = it
                SourceAuthResult.Success
            },
        )
        val configuration = configuration().copy(
            port = 1445,
            username = "alice",
            password = "must-not-leak",
            isGuest = true,
        )

        source.authenticate(configuration)

        assertEquals("", request?.username)
        assertEquals("", request?.password)
        assertEquals(true, request?.isAnonymous)
        assertEquals("smb://nas.local:1445/Music", request?.address)
        assertFalse(configuration.toString().contains("must-not-leak"))
    }

    @Test
    fun searchIsScopedToSmbAndPlaybackRejectsOtherSourceId() = runTest {
        var searchKind: LegacyStorageKind? = null
        val source = source(
            search = LegacyStorageSearchProvider { _, _, _, kind, _ ->
                searchKind = kind
                SourceSearchResult.Success(emptyList())
            },
        )

        source.search(SourceAccountId("storage:7"), "track")
        assertEquals(LegacyStorageKind.Smb, searchKind)

        val result = source.resolvePlayback(
            MediaId(
                sourceId = BuiltInSourceIds.WebDav,
                mediaType = MediaType.Track,
                remoteId = "anything",
            )
        )
        assertEquals(
            SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaId),
            result,
        )
    }

    @Test
    fun playbackDecodesControlledMediaIdAndRegistryContainsSmb() = runTest {
        var resolvedPath: String? = null
        var resolvedKind: LegacyStorageKind? = null
        val source = source(
            playback = object : LegacyStoragePlaybackResolver {
                override suspend fun resolve(
                    accountId: SourceAccountId,
                    path: String,
                    expectedStorageKind: LegacyStorageKind,
                ): SourcePlaybackResult {
                    assertEquals(SourceAccountId("storage:7"), accountId)
                    resolvedPath = path
                    resolvedKind = expectedStorageKind
                    return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unknown)
                }

                override suspend fun release(uri: String) = Unit
                override suspend fun releaseAll() = Unit
            },
        )
        val registry = MusicSourceRegistry(listOf(source))

        source.resolvePlayback(
            legacyStorageTrackMediaId(
                sourceId = BuiltInSourceIds.Smb,
                accountId = SourceAccountId("storage:7"),
                path = "/音乐/Track.flac",
            )
        )

        assertEquals(source, registry.source(BuiltInSourceIds.Smb))
        assertEquals("/音乐/Track.flac", resolvedPath)
        assertEquals(LegacyStorageKind.Smb, resolvedKind)
    }

    private fun source(
        tester: LegacyStorageConnectionTester = LegacyStorageConnectionTester {
            SourceAuthResult.Success
        },
        lister: LegacyStorageDirectoryLister = LegacyStorageDirectoryLister { _, _, _ ->
            SourceListResult.Success(emptyList())
        },
        search: LegacyStorageSearchProvider = LegacyStorageSearchProvider { _, _, _, _, _ ->
            SourceSearchResult.Success(emptyList())
        },
        playback: LegacyStoragePlaybackResolver = object : LegacyStoragePlaybackResolver {
            override suspend fun resolve(
                accountId: SourceAccountId,
                path: String,
                expectedStorageKind: LegacyStorageKind,
            ): SourcePlaybackResult {
                return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unknown)
            }

            override suspend fun release(uri: String) = Unit
            override suspend fun releaseAll() = Unit
        },
    ): SmbMusicSource {
        return SmbMusicSource(
            connectionTester = tester,
            directoryLister = lister,
            playbackResolver = playback,
            searchProvider = search,
        )
    }

    private fun configuration(): SmbSourceConfiguration {
        return SmbSourceConfiguration(
            alias = "NAS",
            host = "nas.local",
            share = "Music",
            username = "alice",
            password = "secret",
        )
    }
}
