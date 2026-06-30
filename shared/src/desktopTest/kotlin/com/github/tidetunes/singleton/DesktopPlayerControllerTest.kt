package com.github.tidetunes.singleton

import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.core.data.ToastRepositoryImpl
import com.github.tidetunes.core.domain.repository.ToastRepository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.core.data.security.CredentialStore
import com.github.tidetunes.core.domain.model.StoredCredential
import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.database.PlaylistEntity
import com.github.tidetunes.database.PlaylistTrackCrossRef
import com.github.tidetunes.database.StorageEntity
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.core.data.datastore.AppPreferencesRepository
import com.github.tidetunes.core.data.datastore.createAppDataStore
import com.github.tidetunes.service.playback.data.PlaybackResourceResolver
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.LegacyStorageKind
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.MusicSourceDescriptor
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListFailureReason
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.storage.LegacyStorageLookup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.PlaylistId
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPlayerControllerTest {
    @Test
    fun readyEngineStartsPlaybackAndReleasesResourceOnStop() = withHarness(
        sourceResult = SourcePlaybackResult.Success(TEST_RESOURCE),
        engine = RecordingDesktopPlaybackEngine(PlaybackEngineLoadResult.Ready),
    ) { harness ->
        harness.controller.play(MusicId(TRACK_ID), PlaylistId(PLAYLIST_ID))

        awaitUntil {
            harness.playerRepository.playing.value &&
                harness.playerRepository.music.value?.meta?.id?.value == TRACK_ID
        }

        assertEquals(listOf(TEST_RESOURCE.uri), harness.engine.loadedRequests.map { it.resource.uri })
        assertEquals(listOf(TRACK_TITLE), harness.engine.loadedRequests.map { it.item.title })
        assertEquals(1, harness.engine.playCalls)
        assertEquals(TRACK_TITLE, harness.playerRepository.music.value?.meta?.title)
        assertEquals(PLAYLIST_ID, harness.playerRepository.playlist.value?.abstr?.meta?.id?.value)
        assertEquals(listOf(TEST_RESOURCE.uri), harness.source.resolvedUris)
        assertEquals(123_000L, harness.controller.getDuration())

        harness.controller.seek(5_000UL)
        harness.controller.pause()
        harness.controller.resume()
        harness.controller.stop()

        awaitUntil { TEST_RESOURCE.uri in harness.playbackResolver.releasedUris }
        assertEquals(listOf(5_000L), harness.engine.seekCalls)
        assertEquals(1, harness.engine.pauseCalls)
        assertEquals(2, harness.engine.playCalls)
        assertTrue(harness.engine.stopCalls >= 1)
        assertFalse(harness.playerRepository.playing.value)
        assertNull(harness.playerRepository.music.value)
    }

    @Test
    fun unsupportedEngineReleasesResolvedResourceAndKeepsPlayerIdle() = withHarness(
        sourceResult = SourcePlaybackResult.Success(TEST_RESOURCE),
        engine = RecordingDesktopPlaybackEngine(PlaybackEngineLoadResult.Unsupported()),
    ) { harness ->
        harness.controller.play(MusicId(TRACK_ID), PlaylistId(PLAYLIST_ID))

        awaitUntil {
            harness.engine.loadedRequests.isNotEmpty() &&
                TEST_RESOURCE.uri in harness.playbackResolver.releasedUris
        }

        assertEquals(listOf(TEST_RESOURCE.uri), harness.engine.loadedRequests.map { it.resource.uri })
        assertFalse(harness.playerRepository.loading.value)
        assertFalse(harness.playerRepository.playing.value)
        assertNull(harness.playerRepository.music.value)
    }

    @Test
    fun resolveFailureDoesNotLoadEngineAndClearsLoadingState() = withHarness(
        sourceResult = SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable),
        engine = RecordingDesktopPlaybackEngine(PlaybackEngineLoadResult.Ready),
    ) { harness ->
        harness.controller.play(MusicId(TRACK_ID), PlaylistId(PLAYLIST_ID))

        awaitUntil {
            harness.source.resolveCalls == 1 &&
                !harness.playerRepository.loading.value
        }

        assertEquals(emptyList(), harness.engine.loadedRequests)
        assertEquals(emptyList(), harness.playbackResolver.releasedUris)
        assertFalse(harness.playerRepository.playing.value)
        assertNull(harness.playerRepository.music.value)
    }

    private fun withHarness(
        sourceResult: SourcePlaybackResult,
        engine: RecordingDesktopPlaybackEngine,
        block: suspend (DesktopPlaybackHarness) -> Unit,
    ) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val preferencesFile = File.createTempFile("tidetunes-player-", ".preferences_pb").apply {
            delete()
        }
        val database = Room.inMemoryDatabaseBuilder<TideTunesDatabase> {
            TideTunesDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            seedLibrary(database)
            val roomLibraryStore = RoomLibraryStore(
                database = database,
                trackDao = database.trackDao(),
                remoteFileDao = database.remoteFileDao(),
                playlistDao = database.playlistDao(),
                metadataDao = database.metadataDao(),
            )
            val toastRepository = ToastRepositoryImpl(scope)
            val storageRepository = StorageRepositoryImpl(
                bridge = Bridge(
                    appDocumentDir = preferencesFile.parentFile.absolutePath,
                    appCacheDir = preferencesFile.parentFile.absolutePath,
                    toastRepository = toastRepository,
                ),
                scope = scope,
                storageDao = database.storageDao(),
                credentialStore = InMemoryCredentialStore(),
            )
            val playerRepository = PlayerRepository(
                roomLibraryStore = roomLibraryStore,
                appPreferencesRepository = AppPreferencesRepository(
                    createAppDataStore { preferencesFile.absolutePath.toPath() }
                ),
                _scope = scope,
                storageLookup = LegacyStorageLookup {
                    storage(id = STORAGE_ID, type = StorageType.WEBDAV)
                },
            )
            val source = RecordingMusicSource(sourceResult)
            val playbackResolver = RecordingLegacyPlaybackResolver()
            val playbackResourceResolver = PlaybackResourceResolver(
                storageLookup = LegacyStorageLookup {
                    storage(id = STORAGE_ID, type = StorageType.WEBDAV)
                },
                sourceRegistry = MusicSourceRegistry(listOf(source)),
                legacyStoragePlaybackResolver = playbackResolver,
            )
            val controller = DesktopPlayerController(
                playerRepository = playerRepository,
                toastRepository = toastRepository,
                playlistRepository = PlaylistRepositoryImpl(
                    storageRepository = storageRepository,
                    _scope = scope,
                    playlistDao = database.playlistDao(),
                    roomLibraryStore = roomLibraryStore,
                    storageLookup = LegacyStorageLookup {
                        storage(id = STORAGE_ID, type = StorageType.WEBDAV)
                    },
                ),
                storageRepository = storageRepository,
                roomLibraryStore = roomLibraryStore,
                playbackResourceResolver = playbackResourceResolver,
                playbackEngine = engine,
                scope = scope,
            )

            block(
                DesktopPlaybackHarness(
                    controller = controller,
                    playerRepository = playerRepository,
                    source = source,
                    engine = engine,
                    playbackResolver = playbackResolver,
                )
            )
        } finally {
            scope.cancel()
            database.close()
            preferencesFile.delete()
        }
    }

    private suspend fun seedLibrary(database: TideTunesDatabase) {
        database.storageDao().upsert(
            StorageEntity(
                id = STORAGE_ID,
                type = StorageType.WEBDAV.name,
                displayName = "WebDAV",
                baseUrl = "https://example.invalid/dav",
                credentialRef = "storage-$STORAGE_ID",
                username = "",
                isAnonymous = true,
                musicCount = 1,
                createdAt = 1,
                updatedAt = 1,
            )
        )
        database.trackDao().upsertAll(
            listOf(
                TrackEntity(
                    id = TRACK_ID,
                    remoteFileId = null,
                    sourceStorageId = STORAGE_ID,
                    sourcePath = TRACK_PATH,
                    title = TRACK_TITLE,
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
                    updatedAt = 1,
                    artist = "Luna",
                )
            )
        )
        database.playlistDao().upsert(
            PlaylistEntity(
                id = PLAYLIST_ID,
                title = "Queue",
                artworkId = null,
                createdAt = 1,
                updatedAt = 1,
                sortOrder = 0,
            )
        )
        database.playlistDao().upsertTracks(
            listOf(
                PlaylistTrackCrossRef(
                    playlistId = PLAYLIST_ID,
                    trackId = TRACK_ID,
                    sortOrder = 0,
                    addedAt = 1,
                )
            )
        )
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) {
                delay(10)
            }
        }
    }

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
        musicCount = 1u,
    )
}

private data class DesktopPlaybackHarness(
    val controller: DesktopPlayerController,
    val playerRepository: PlayerRepository,
    val source: RecordingMusicSource,
    val engine: RecordingDesktopPlaybackEngine,
    val playbackResolver: RecordingLegacyPlaybackResolver,
)

private class RecordingDesktopPlaybackEngine(
    private val loadResult: PlaybackEngineLoadResult,
) : DesktopPlaybackEngine {
    val loadedRequests = mutableListOf<PlaybackEngineLoadRequest>()
    val seekCalls = mutableListOf<Long>()
    var playCalls = 0
        private set
    var pauseCalls = 0
        private set
    var stopCalls = 0
        private set

    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        loadedRequests += request
        return loadResult
    }

    override fun play() {
        playCalls += 1
    }

    override fun pause() {
        pauseCalls += 1
    }

    override fun stop() {
        stopCalls += 1
    }

    override fun seekTo(positionMs: Long) {
        seekCalls += positionMs
    }

    override fun readPosition(): PlaybackPosition {
        return PlaybackPosition(
            positionMs = 1_000L,
            bufferedMs = 2_000L,
            durationMs = 123_000L,
        )
    }

    override fun release() = Unit
}

private class RecordingMusicSource(
    private val result: SourcePlaybackResult,
) : MusicSource {
    override val descriptor = MusicSourceDescriptor(
        id = BuiltInSourceIds.WebDav,
        displayName = "WebDAV",
    )
    override val capabilities = setOf(SourceCapability.Stream)
    val resolvedUris = mutableListOf<String>()
    var resolveCalls = 0
        private set

    override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
        return SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration)
    }

    override suspend fun list(
        accountId: SourceAccountId,
        directoryId: String?,
    ): SourceListResult {
        return SourceListResult.Failure(SourceListFailureReason.UnsupportedAccount)
    }

    override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
        resolveCalls += 1
        val uri = (result as? SourcePlaybackResult.Success)?.resource?.uri
        if (uri != null) {
            resolvedUris += uri
        }
        return result
    }
}

private class RecordingLegacyPlaybackResolver : LegacyStoragePlaybackResolver {
    val releasedUris = mutableListOf<String>()

    override suspend fun resolve(
        accountId: SourceAccountId,
        path: String,
        expectedStorageKind: LegacyStorageKind,
    ): SourcePlaybackResult {
        return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)
    }

    override suspend fun release(uri: String) {
        releasedUris += uri
    }

    override suspend fun releaseAll() = Unit
}

private class InMemoryCredentialStore : CredentialStore {
    private val values = mutableMapOf<Long, StoredCredential>()

    override suspend fun load(storageId: Long): StoredCredential? {
        return values[storageId]
    }

    override suspend fun save(storageId: Long, credential: StoredCredential) {
        values[storageId] = credential
    }

    override suspend fun delete(storageId: Long) {
        values.remove(storageId)
    }
}

private const val STORAGE_ID = 2L
private const val TRACK_ID = 7L
private const val TRACK_TITLE = "Moon"
private const val TRACK_PATH = "/Music/Moon.flac"
private const val PLAYLIST_ID = 3L

private val TEST_RESOURCE = PlaybackResource(
    uri = "http://127.0.0.1/track.flac",
    mimeType = "audio/flac",
)
