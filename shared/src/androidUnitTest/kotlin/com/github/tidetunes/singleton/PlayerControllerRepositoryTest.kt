package com.github.tidetunes.singleton

import com.github.tidetunes.core.data.ToastRepositoryImpl

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.database.TrackSourcePlaybackCandidate
import com.github.tidetunes.database.TrackSourceRefDao
import com.github.tidetunes.database.TrackSourceRefEntity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import uniffi.tidetunes_backend.ArgRemoveMusicFromPlaylist
import uniffi.tidetunes_backend.Music
import uniffi.tidetunes_backend.MusicAbstract
import uniffi.tidetunes_backend.MusicId
import uniffi.tidetunes_backend.MusicMeta
import uniffi.tidetunes_backend.Playlist
import uniffi.tidetunes_backend.PlaylistAbstract
import uniffi.tidetunes_backend.PlaylistId
import uniffi.tidetunes_backend.PlaylistMeta
import uniffi.tidetunes_backend.Storage
import uniffi.tidetunes_backend.StorageEntryLoc
import uniffi.tidetunes_backend.StorageId
import uniffi.tidetunes_backend.StorageType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class PlayerControllerRepositoryTest {
    @Test
    fun readyEngineStartsPlaybackAndReleasesResourceOnStop() = withHarness(
        sourceResult = SourcePlaybackResult.Success(TEST_RESOURCE),
        engine = RecordingAndroidPlaybackEngine(PlaybackEngineLoadResult.Ready),
    ) { harness ->
        harness.controller.play(MusicId(TRACK_ID), PlaylistId(PLAYLIST_ID))

        awaitUntil {
            harness.playerState.playing.value &&
                harness.playerState.music.value?.meta?.id?.value == TRACK_ID
        }

        assertEquals(listOf(TEST_RESOURCE.uri), harness.engine.loadedRequests.map { it.resource.uri })
        assertEquals(listOf(TRACK_TITLE), harness.engine.loadedRequests.map { it.item.title })
        assertEquals(0, harness.engine.playCalls)
        assertEquals(TRACK_TITLE, harness.playerState.music.value?.meta?.title)
        assertEquals(PLAYLIST_ID, harness.playerState.playlist.value?.abstr?.meta?.id?.value)
        assertEquals(listOf(TEST_RESOURCE.uri), harness.source.resolvedUris)
        assertEquals(123_000L, harness.controller.getDuration())

        harness.controller.seek(5_000UL)
        harness.controller.pause()
        harness.controller.resume()
        harness.controller.stop()

        awaitUntil { TEST_RESOURCE.uri in harness.playbackResolver.releasedUris }
        assertEquals(listOf(5_000L), harness.engine.seekCalls)
        assertEquals(1, harness.engine.pauseCalls)
        assertEquals(1, harness.engine.playCalls)
        assertFalse(harness.playerState.playing.value)
        assertNull(harness.playerState.music.value)
    }

    @Test
    fun unsupportedEngineReleasesResolvedResourceAndKeepsPlayerIdle() = withHarness(
        sourceResult = SourcePlaybackResult.Success(TEST_RESOURCE),
        engine = RecordingAndroidPlaybackEngine(PlaybackEngineLoadResult.Unsupported()),
    ) { harness ->
        harness.controller.play(MusicId(TRACK_ID), PlaylistId(PLAYLIST_ID))

        awaitUntil {
            harness.engine.loadedRequests.isNotEmpty() &&
                TEST_RESOURCE.uri in harness.playbackResolver.releasedUris
        }

        assertEquals(listOf(TEST_RESOURCE.uri), harness.engine.loadedRequests.map { it.resource.uri })
        assertFalse(harness.playerState.loading.value)
        assertFalse(harness.playerState.playing.value)
        assertNull(harness.playerState.music.value)
    }

    @Test
    fun resolveFailureDoesNotLoadEngineAndClearsLoadingState() = withHarness(
        sourceResult = SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable),
        engine = RecordingAndroidPlaybackEngine(PlaybackEngineLoadResult.Ready),
    ) { harness ->
        harness.controller.play(MusicId(TRACK_ID), PlaylistId(PLAYLIST_ID))

        awaitUntil {
            harness.source.resolveCalls == 1 &&
                !harness.playerState.loading.value
        }

        assertEquals(emptyList(), harness.engine.loadedRequests)
        assertEquals(emptyList(), harness.playbackResolver.releasedUris)
        assertFalse(harness.playerState.playing.value)
        assertNull(harness.playerState.music.value)
    }

    private fun withHarness(
        sourceResult: SourcePlaybackResult,
        engine: RecordingAndroidPlaybackEngine,
        block: suspend (AndroidPlaybackHarness) -> Unit,
    ) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".")
        try {
            val music = music(id = TRACK_ID, title = TRACK_TITLE)
            val playlist = playlist(
                id = PLAYLIST_ID,
                musics = listOf(musicAbstract(id = TRACK_ID, title = TRACK_TITLE)),
            )
            val playerState = FakeAndroidPlayerStateStore()
            val source = RecordingMusicSource(sourceResult)
            val playbackResolver = RecordingLegacyPlaybackResolver()
            val playbackResourceResolver = PlaybackResourceResolver(
                storageLookup = LegacyStorageLookup {
                    storage(id = STORAGE_ID, type = StorageType.WEBDAV)
                },
                trackSourceRefDao = EmptyTrackSourceRefDao,
                sourceRegistry = MusicSourceRegistry(listOf(source)),
                legacyStoragePlaybackResolver = playbackResolver,
            )
            val toastRepository = ToastRepositoryImpl(scope)
            val controller = PlayerControllerRepository(
                playerState = playerState,
                toastRepository = toastRepository,
                removalEvents = FakeAndroidPlaybackRemovalEvents(),
                bridge = Bridge(
                    appDocumentDir = tempDir.absolutePath,
                    appCacheDir = tempDir.absolutePath,
                    toastRepository = toastRepository,
                ),
                playbackLibrary = FakeAndroidPlaybackLibrary(
                    music = music,
                    playlist = playlist,
                ),
                playbackResourceResolver = playbackResourceResolver,
                _scope = scope,
                initialPlaybackEngine = engine,
                mainDispatcher = Dispatchers.Default,
            )

            block(
                AndroidPlaybackHarness(
                    controller = controller,
                    playerState = playerState,
                    source = source,
                    engine = engine,
                    playbackResolver = playbackResolver,
                )
            )
        } finally {
            scope.cancel()
        }
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) {
                delay(10)
            }
        }
    }

    private fun music(
        id: Long,
        title: String,
    ): Music {
        return Music(
            meta = musicMeta(id = id, title = title),
            loc = StorageEntryLoc(
                storageId = StorageId(STORAGE_ID),
                path = TRACK_PATH,
            ),
            cover = null,
            lyric = null,
        )
    }

    private fun musicAbstract(
        id: Long,
        title: String,
    ): MusicAbstract {
        return MusicAbstract(
            meta = musicMeta(id = id, title = title),
            cover = null,
        )
    }

    private fun musicMeta(
        id: Long,
        title: String,
    ): MusicMeta {
        return MusicMeta(
            id = MusicId(id),
            title = title,
            duration = 123_000.milliseconds,
            order = listOf(id.toUInt()),
        )
    }

    private fun playlist(
        id: Long,
        musics: List<MusicAbstract>,
    ): Playlist {
        return Playlist(
            abstr = PlaylistAbstract(
                meta = PlaylistMeta(
                    id = PlaylistId(id),
                    title = "Queue",
                    cover = null,
                    showCover = null,
                    createdTime = 0.milliseconds,
                    order = listOf(id.toUInt()),
                ),
                musicCount = musics.size.toULong(),
                duration = null,
            ),
            musics = musics,
        )
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

private data class AndroidPlaybackHarness(
    val controller: PlayerControllerRepository,
    val playerState: FakeAndroidPlayerStateStore,
    val source: RecordingMusicSource,
    val engine: RecordingAndroidPlaybackEngine,
    val playbackResolver: RecordingLegacyPlaybackResolver,
)

private class FakeAndroidPlayerStateStore : AndroidPlayerStateStore {
    override val playlist = MutableStateFlow<Playlist?>(null)
    override val music = MutableStateFlow<Music?>(null)
    override val nextMusic = MutableStateFlow<MusicAbstract?>(null)
    override val previousMusic = MutableStateFlow<MusicAbstract?>(null)
    override val pauseRequest: Flow<Unit> = MutableSharedFlow()
    val loading = MutableStateFlow(false)
    val playing = MutableStateFlow(false)
    var durationChangedCount = 0

    override fun setIsLoading(loading: Boolean) {
        this.loading.value = loading
    }

    override fun setIsPlaying(playing: Boolean) {
        this.playing.value = playing
    }

    override fun setCurrent(music: Music, playlist: Playlist) {
        this.music.value = music
        this.playlist.value = playlist
    }

    override fun resetCurrent() {
        music.value = null
        playlist.value = null
    }

    override fun notifyDurationChanged() {
        durationChangedCount += 1
    }

    override fun refreshPlaylistIfMatch(playlist: Playlist) {
        if (this.playlist.value?.abstr?.meta?.id == playlist.abstr.meta.id) {
            this.playlist.value = playlist
        }
    }

    override fun emitPauseRequest() = Unit

    override fun reload() = Unit
}

private class FakeAndroidPlaybackRemovalEvents : AndroidPlaybackRemovalEvents {
    override val preRemovePlaylistEvent = MutableSharedFlow<PlaylistId>()
    override val preRemoveMusicEvent = MutableSharedFlow<ArgRemoveMusicFromPlaylist>()
    override val preRemoveStorageEvent = MutableSharedFlow<StorageId>()
    val removedMusic = mutableListOf<Pair<Long, Long>>()

    override suspend fun removeMusic(playlistId: Long, musicId: Long) {
        removedMusic += playlistId to musicId
    }
}

private class FakeAndroidPlaybackLibrary(
    private val music: Music,
    private val playlist: Playlist,
) : AndroidPlaybackLibrary {
    override suspend fun getMusic(id: MusicId): Music? {
        return music.takeIf { it.meta.id == id }
    }

    override suspend fun getPlaylist(id: PlaylistId): Playlist? {
        return playlist.takeIf { it.abstr.meta.id == id }
    }
}

private class RecordingAndroidPlaybackEngine(
    private val loadResult: PlaybackEngineLoadResult,
) : AndroidPlaybackEngine {
    val loadedRequests = mutableListOf<PlaybackEngineLoadRequest>()
    val seekCalls = mutableListOf<Long>()
    var playCalls = 0
        private set
    var pauseCalls = 0
        private set
    var stopCalls = 0
        private set
    var releaseCalls = 0
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

    override fun release() {
        releaseCalls += 1
    }
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

private object EmptyTrackSourceRefDao : TrackSourceRefDao {
    override suspend fun findByTrackId(trackId: Long): List<TrackSourceRefEntity> {
        return emptyList()
    }

    override suspend fun findBySourceItemIds(sourceItemIds: List<Long>): List<TrackSourceRefEntity> {
        return emptyList()
    }

    override suspend fun countForTrack(trackId: Long): Int {
        return 0
    }

    override suspend fun webDavMetadataCandidatesForTrack(trackId: Long) =
        emptyList<com.github.tidetunes.database.MetadataRefreshCandidate>()

    override suspend fun metadataResetCandidateForTrack(
        trackId: Long,
    ): com.github.tidetunes.database.MetadataRefreshCandidate? = null

    override suspend fun webDavMetadataCandidatesForAlbum(albumId: Long) =
        emptyList<com.github.tidetunes.database.MetadataRefreshCandidate>()

    override suspend fun missingWebDavMetadataCandidates(target: String) =
        emptyList<com.github.tidetunes.database.MetadataRefreshCandidate>()

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
        return emptyList()
    }

    override suspend fun playbackCandidatesForTracks(trackIds: List<Long>): List<TrackSourcePlaybackCandidate> {
        return emptyList()
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
