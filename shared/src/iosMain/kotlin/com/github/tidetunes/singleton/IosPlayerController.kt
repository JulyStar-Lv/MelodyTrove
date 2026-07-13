package com.github.tidetunes.singleton

import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.core.data.ToastRepositoryImpl

import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.service.playback.domain.SleepModeState
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.service.playback.data.PlaybackResourceResolver
import com.github.tidetunes.service.playback.data.PlaybackPreparationResult
import com.github.tidetunes.service.playback.data.preparePlayback
import com.github.tidetunes.core.domain.repository.NetworkStatusProvider
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.source.api.PlaybackResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.tidetunes_backend.MusicId
import uniffi.tidetunes_backend.Playlist
import uniffi.tidetunes_backend.PlaylistId
import uniffi.tidetunes_backend.tidetunesError
import kotlin.math.max

class IosPlayerController internal constructor(
    private val playerRepository: PlayerRepository,
    private val toastRepository: ToastRepositoryImpl,
    private val playlistRepository: PlaylistRepositoryImpl,
    private val storageRepository: StorageRepositoryImpl,
    private val roomLibraryStore: RoomLibraryStore,
    private val playbackResourceResolver: PlaybackResourceResolver,
    private val scope: CoroutineScope,
    private val playbackEngine: IosPlaybackEngine,
    private val mainDispatcher: CoroutineDispatcher,
    private val settingsRepository: SettingsRepository? = null,
    private val networkStatusProvider: NetworkStatusProvider? = null,
) : PlayerController {
    private val sleep = MutableStateFlow(SleepModeState())
    private var sleepJob: Job? = null
    private var playbackJob: Job? = null
    private var playbackResource: PlaybackResource? = null
    private var pendingNetworkRecovery: Pair<MusicId, PlaylistId>? = null

    override val sleepState = sleep.asStateFlow()

    constructor(
        playerRepository: PlayerRepository,
        toastRepository: ToastRepositoryImpl,
        playlistRepository: PlaylistRepositoryImpl,
        storageRepository: StorageRepositoryImpl,
        roomLibraryStore: RoomLibraryStore,
        playbackResourceResolver: PlaybackResourceResolver,
        scope: CoroutineScope,
        settingsRepository: SettingsRepository,
        networkStatusProvider: NetworkStatusProvider,
    ) : this(
        playerRepository = playerRepository,
        toastRepository = toastRepository,
        playlistRepository = playlistRepository,
        storageRepository = storageRepository,
        roomLibraryStore = roomLibraryStore,
        playbackResourceResolver = playbackResourceResolver,
        scope = scope,
        playbackEngine = AvPlayerIosPlaybackEngine(),
        mainDispatcher = Dispatchers.Main,
        settingsRepository = settingsRepository,
        networkStatusProvider = networkStatusProvider,
    )

    init {
        scope.launch {
            playerRepository.pauseRequest.collect { pause() }
        }
        scope.launch {
            playlistRepository.preRemovePlaylistEvent.collect { id ->
                if (playerRepository.playlist.value?.abstr?.meta?.id == id) {
                    stop()
                }
            }
        }
        scope.launch {
            playlistRepository.preRemoveMusicEvent.collect { arg ->
                if (
                    playerRepository.playlist.value?.abstr?.meta?.id == arg.playlistId &&
                    playerRepository.music.value?.meta?.id == arg.musicId
                ) {
                    stop()
                }
            }
        }
        scope.launch {
            storageRepository.preRemoveStorageEvent.collect { id ->
                if (playerRepository.music.value?.loc?.storageId == id) {
                    stop()
                }
            }
        }
        networkStatusProvider?.let { provider ->
            scope.launch {
                provider.status.collect { network ->
                    val pending = pendingNetworkRecovery ?: return@collect
                    val settings = settingsRepository?.settings?.first() ?: return@collect
                    if (network.isOnline && settings.resumePlaybackAfterNetworkRecovery) {
                        pendingNetworkRecovery = null
                        play(pending.first, pending.second)
                    }
                }
            }
        }
    }

    override fun getCurrentPosition(): Long = playbackEngine.readPosition().positionMs

    override fun getBufferedPosition(): Long = playbackEngine.readPosition().bufferedMs

    override fun getDuration(): Long = playbackEngine.readPosition().durationMs

    override fun play(id: MusicId, playlistId: PlaylistId) {
        if (
            playerRepository.music.value?.meta?.id == id &&
            playerRepository.playlist.value?.abstr?.meta?.id == playlistId
        ) {
            resume()
            return
        }

        playbackJob?.cancel()
        playbackJob = scope.launch(mainDispatcher) {
            playerRepository.setIsLoading(true)
            try {
                playbackEngine.stop()
                releasePlaybackResource()
                playerRepository.setIsPlaying(false)
                playerRepository.resetCurrent()
                val music = roomLibraryStore.getMusic(id)
                val playlist = roomLibraryStore.getPlaylist(playlistId)
                val belongsToPlaylist = playlist?.musics?.any { it.meta.id == id } == true
                if (music == null || playlist == null || !belongsToPlaylist) {
                    return@launch
                }

                when (
                    val preparation = preparePlayback(
                        music = music,
                        playlistId = playlist.abstr.meta.id.value,
                        playbackResourceResolver = playbackResourceResolver,
                        playbackEngine = playbackEngine,
                        settingsRepository = settingsRepository,
                        networkStatusProvider = networkStatusProvider,
                    )
                ) {
                    is PlaybackPreparationResult.Ready -> {
                        playbackResource = preparation.resource
                        pendingNetworkRecovery = null
                    }
                    PlaybackPreparationResult.NetworkBlocked,
                    PlaybackPreparationResult.Failed -> {
                        pendingNetworkRecovery = id to playlistId
                        toastRepository.emitToast("Unable to open audio stream")
                        playerRepository.resetCurrent()
                        return@launch
                    }
                }

                playerRepository.setCurrent(music, playlist)
                playbackEngine.play()
                playerRepository.setIsPlaying(true)
                playerRepository.notifyDurationChanged()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val resource = playbackResource
                if (resource != null) {
                    playbackResource = null
                    playbackResourceResolver.release(resource)
                }
                tidetunesError("iOS playback failed: $error")
                toastRepository.emitToast(error.toString())
                playerRepository.resetCurrent()
                playerRepository.setIsPlaying(false)
            } finally {
                playerRepository.setIsLoading(false)
            }
        }
    }

    override fun resume() {
        playbackEngine.play()
        playerRepository.setIsPlaying(true)
    }

    override fun pause() {
        playbackEngine.pause()
        playerRepository.setIsPlaying(false)
    }

    override fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        playbackEngine.stop()
        releasePlaybackResourceAsync()
        playerRepository.setIsPlaying(false)
        playerRepository.resetCurrent()
    }

    override fun playNext() {
        val music = playerRepository.nextMusic.value
        val playlist = playerRepository.playlist.value
        if (music != null && playlist != null) {
            play(music.meta.id, playlist.abstr.meta.id)
        }
    }

    override fun playPrevious() {
        val music = playerRepository.previousMusic.value
        val playlist = playerRepository.playlist.value
        if (music != null && playlist != null) {
            play(music.meta.id, playlist.abstr.meta.id)
        }
    }

    override fun seek(ms: ULong) {
        playbackEngine.seekTo(ms.coerceAtMost(Long.MAX_VALUE.toULong()).toLong())
    }

    override fun scheduleSleep(newExpiredMs: Long) {
        sleepJob?.cancel()
        sleepJob = scope.launch {
            sleep.update { it.copy(enabled = true, expiredMs = newExpiredMs) }
            delay(max(newExpiredMs - currentTimeMillis(), 0L))
            pause()
            sleep.update { it.copy(enabled = false, expiredMs = 0L) }
        }
    }

    override fun refreshPlaylistIfMatch(playlist: Playlist) {
        playerRepository.refreshPlaylistIfMatch(playlist)
    }

    override fun cancelSleep() {
        sleepJob?.cancel()
        sleepJob = null
        sleep.update { it.copy(enabled = false, expiredMs = 0L) }
    }

    private suspend fun releasePlaybackResource() {
        val resource = playbackResource ?: return
        playbackResource = null
        playbackResourceResolver.release(resource)
    }

    private fun releasePlaybackResourceAsync() {
        val resource = playbackResource ?: return
        playbackResource = null
        scope.launch {
            playbackResourceResolver.release(resource)
        }
    }
}
