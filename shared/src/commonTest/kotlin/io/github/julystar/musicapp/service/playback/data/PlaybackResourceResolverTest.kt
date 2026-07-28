package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.database.ProviderTypes
import io.github.julystar.musicapp.database.MetadataRefreshCandidate
import io.github.julystar.musicapp.database.SourceAccountEntity
import io.github.julystar.musicapp.database.SourceItemEntity
import io.github.julystar.musicapp.database.SourceItemTypes
import io.github.julystar.musicapp.database.TrackSourcePlaybackCandidate
import io.github.julystar.musicapp.database.TrackSourceRefDao
import io.github.julystar.musicapp.database.TrackSourceRefEntity
import io.github.julystar.musicapp.source.api.BuiltInSourceIds
import io.github.julystar.musicapp.source.api.MusicSource
import io.github.julystar.musicapp.source.api.MusicSourceDescriptor
import io.github.julystar.musicapp.source.api.MusicSourceRegistry
import io.github.julystar.musicapp.source.api.PlaybackResource
import io.github.julystar.musicapp.source.api.SourceAuthResult
import io.github.julystar.musicapp.source.api.SourceCapability
import io.github.julystar.musicapp.source.api.SourceConfiguration
import io.github.julystar.musicapp.source.api.SourceListResult
import io.github.julystar.musicapp.source.api.SourcePlaybackFailureReason
import io.github.julystar.musicapp.source.api.SourcePlaybackResult
import io.github.julystar.musicapp.source.api.LegacyStorageKind
import io.github.julystar.musicapp.source.api.LegacyStoragePlaybackResolver
import io.github.julystar.musicapp.source.api.legacyStorageTrackMediaId
import io.github.julystar.musicapp.source.storage.LegacyStorageLookup
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.app_backend.Music
import uniffi.app_backend.MusicId
import uniffi.app_backend.MusicMeta
import uniffi.app_backend.Storage
import uniffi.app_backend.StorageEntryLoc
import uniffi.app_backend.StorageId
import uniffi.app_backend.StorageType

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
            trackSourceRefDao = fakeTrackSourceRefDao(),
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
    fun resolvesRoomSourceCandidateBeforeLegacyLocation() = runBlocking {
        var capturedMediaId: MediaId? = null
        val source = fakeMusicSource(BuiltInSourceIds.WebDav) { mediaId ->
            capturedMediaId = mediaId
            SourcePlaybackResult.Success(PlaybackResource(uri = "http://127.0.0.1/candidate.flac"))
        }
        val resolver = PlaybackResourceResolver(
            storageLookup = LegacyStorageLookup { null },
            trackSourceRefDao = fakeTrackSourceRefDao(
                candidate(path = "/Music/Candidate.flac"),
            ),
            sourceRegistry = MusicSourceRegistry(listOf(source)),
            legacyStoragePlaybackResolver = unusedPlaybackResolver(),
        )

        val result = resolver.resolve(music(storageId = 1, path = "/Legacy/Track.flac"))

        assertEquals(
            SourcePlaybackResult.Success(PlaybackResource(uri = "http://127.0.0.1/candidate.flac")),
            result,
        )
        assertEquals(
            legacyStorageTrackMediaId(
                sourceId = BuiltInSourceIds.WebDav,
                accountId = SourceAccountId("storage:42"),
                path = "/Music/Candidate.flac",
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
            trackSourceRefDao = fakeTrackSourceRefDao(),
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
            trackSourceRefDao = fakeTrackSourceRefDao(),
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

    private fun fakeTrackSourceRefDao(
        vararg candidates: TrackSourcePlaybackCandidate,
    ) = object : TrackSourceRefDao {
        override suspend fun findByTrackId(trackId: Long): List<TrackSourceRefEntity> {
            return emptyList()
        }

        override suspend fun findBySourceItemIds(sourceItemIds: List<Long>): List<TrackSourceRefEntity> {
            return emptyList()
        }

        override suspend fun webDavMetadataCandidatesForTrack(trackId: Long) = emptyList<MetadataRefreshCandidate>()

        override suspend fun metadataResetCandidateForTrack(trackId: Long): MetadataRefreshCandidate? = null

        override suspend fun webDavMetadataCandidatesForAlbum(albumId: Long) = emptyList<MetadataRefreshCandidate>()

        override suspend fun missingWebDavMetadataCandidates(target: String) = emptyList<MetadataRefreshCandidate>()

        override suspend fun countForTrack(trackId: Long): Int {
            return 0
        }

        override suspend fun hasSourceAccount(trackId: Long, sourceAccountId: Long) = false

        override suspend fun upsertAll(refs: List<TrackSourceRefEntity>) = Unit

        override suspend fun updateEmbeddedArtworkPresence(
            sourceItemId: Long,
            hasEmbeddedArtwork: Boolean,
            now: Long,
        ) = Unit

        override suspend fun markAvailableBySourceItemIds(sourceItemIds: List<Long>, now: Long) = Unit

        override suspend fun markUnavailableBySourceItemIds(sourceItemIds: List<Long>, now: Long) = Unit

        override suspend fun markUnavailableForDeletedSourceItems(libraryRootId: Long, now: Long) = Unit

        override suspend fun playbackCandidates(trackId: Long): List<TrackSourcePlaybackCandidate> {
            return candidates.toList()
        }

        override suspend fun playbackCandidatesForTracks(trackIds: List<Long>): List<TrackSourcePlaybackCandidate> {
            return candidates.filter { candidate -> candidate.ref.trackId in trackIds }
        }
    }

    private fun candidate(
        path: String,
    ) = TrackSourcePlaybackCandidate(
        ref = TrackSourceRefEntity(
            trackId = 7,
            sourceItemId = 100,
            role = "primary",
            matchMethod = "source_identity",
            matchConfidence = 100,
            isPreferred = true,
            isAvailable = true,
            isDownloaded = false,
            playable = true,
            downloadable = true,
            codec = null,
            container = null,
            bitRate = null,
            sampleRate = null,
            bitsPerSample = null,
            channels = null,
            lossless = null,
            createdAt = 1,
            updatedAt = 2,
        ),
        item = SourceItemEntity(
            id = 100,
            sourceAccountId = 42,
            libraryRootId = 2,
            itemType = SourceItemTypes.Track,
            providerItemId = "item-100",
            parentProviderItemId = null,
            canonicalPath = path,
            displayPath = path,
            displayName = path.substringAfterLast('/'),
            mimeType = "audio/flac",
            sizeBytes = 100,
            etag = null,
            revision = null,
            createdAtRemote = null,
            modifiedAtRemote = null,
            contentHash = null,
            audioFingerprint = null,
            isDeleted = false,
            firstSyncedAt = 1,
            lastSyncedAt = 2,
            lastSeenScanId = "scan-1",
        ),
        account = SourceAccountEntity(
            id = 42,
            providerType = ProviderTypes.WebDav,
            displayName = "NAS",
            endpoint = null,
            externalAccountId = null,
            credentialRef = null,
            priority = 0,
            enabled = true,
            createdAt = 1,
            updatedAt = 2,
        ),
    )
}
