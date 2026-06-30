package com.github.tidetunes.service.playback.data

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.MusicSourceDescriptor
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.LegacyStorageKind
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.legacyStorageTrackMediaId
import com.github.tidetunes.source.storage.LegacyStorageLookup
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.tidetunes_core.Music
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.MusicMeta
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType

class PlaybackResourceResolverTest {
    @Test
    fun resolvesLegacyMusicLocationThroughMatchingSource() = runBlocking {
        var capturedMediaId: MediaId? = null
        val source = fakeMusicSource(BuiltInSourceIds.WebDav) { mediaId ->
            capturedMediaId = mediaId
            SourcePlaybackResult.Success(PlaybackResource(uri = "http://127.0.0.1/track.flac"))
        }
        val resolver = PlaybackResourceResolver(
            storageLookup = LegacyStorageLookup {
                storage(id = 42, typ = StorageType.WEBDAV)
            },
            sourceRegistry = MusicSourceRegistry(listOf(source)),
            legacyStoragePlaybackResolver = unusedPlaybackResolver(),
        )

        val result = resolver.resolve(music(storageId = 42, path = "/Music/Track.flac"))

        assertEquals(
            SourcePlaybackResult.Success(PlaybackResource(uri = "http://127.0.0.1/track.flac")),
            result,
        )
        assertEquals(
            legacyStorageTrackMediaId(
                sourceId = BuiltInSourceIds.WebDav,
                accountId = SourceAccountId("storage:42"),
                path = "/Music/Track.flac",
            ),
            capturedMediaId,
        )
    }

    @Test
    fun missingStorageFailsBeforeCallingSource() = runBlocking {
        var sourceCalls = 0
        val source = fakeMusicSource(BuiltInSourceIds.Local) {
            sourceCalls += 1
            SourcePlaybackResult.Success(PlaybackResource(uri = "http://127.0.0.1/local.wav"))
        }
        val resolver = PlaybackResourceResolver(
            storageLookup = LegacyStorageLookup { null },
            sourceRegistry = MusicSourceRegistry(listOf(source)),
            legacyStoragePlaybackResolver = unusedPlaybackResolver(),
        )

        assertEquals(
            SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedAccount),
            resolver.resolve(music(storageId = 1, path = "/Missing.wav")),
        )
        assertEquals(0, sourceCalls)
    }

    @Test
    fun releaseDelegatesToRetainedLegacyResolver() = runBlocking {
        val released = mutableListOf<String>()
        val resolver = PlaybackResourceResolver(
            storageLookup = LegacyStorageLookup { null },
            sourceRegistry = MusicSourceRegistry(emptyList()),
            legacyStoragePlaybackResolver = object : LegacyStoragePlaybackResolver {
                override suspend fun resolve(
                    accountId: SourceAccountId,
                    path: String,
                    expectedStorageKind: LegacyStorageKind,
                ): SourcePlaybackResult {
                    return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)
                }

                override suspend fun release(uri: String) {
                    released += uri
                }

                override suspend fun releaseAll() = Unit
            },
        )

        resolver.release(PlaybackResource(uri = "http://127.0.0.1/release.mp3"))

        assertEquals(listOf("http://127.0.0.1/release.mp3"), released)
    }

    private fun fakeMusicSource(
        sourceId: SourceId,
        resolve: suspend (MediaId) -> SourcePlaybackResult,
    ) = object : MusicSource {
        override val descriptor = MusicSourceDescriptor(sourceId, sourceId.value)
        override val capabilities = setOf(SourceCapability.Stream)

        override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
            return SourceAuthResult.Success
        }

        override suspend fun list(
            accountId: SourceAccountId,
            directoryId: String?,
        ): SourceListResult {
            return SourceListResult.Success(emptyList())
        }

        override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
            return resolve(mediaId)
        }
    }

    private fun unusedPlaybackResolver() = object : LegacyStoragePlaybackResolver {
        override suspend fun resolve(
            accountId: SourceAccountId,
            path: String,
            expectedStorageKind: LegacyStorageKind,
        ): SourcePlaybackResult {
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)
        }

        override suspend fun release(uri: String) = Unit

        override suspend fun releaseAll() = Unit
    }

    private fun music(
        storageId: Long,
        path: String,
    ) = Music(
        meta = MusicMeta(
            id = MusicId(7),
            title = "Track",
            duration = null,
            order = emptyList(),
        ),
        loc = StorageEntryLoc(
            storageId = StorageId(storageId),
            path = path,
        ),
        cover = null,
        lyric = null,
    )

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
}
