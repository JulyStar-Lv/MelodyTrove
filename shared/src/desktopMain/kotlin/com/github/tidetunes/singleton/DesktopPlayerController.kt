package com.github.tidetunes.singleton

import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.core.data.ToastRepositoryImpl

import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.service.playback.domain.SleepModeState
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.service.playback.data.PlaybackResourceResolver
import com.github.tidetunes.service.playback.data.toPlayableItem
import com.github.tidetunes.service.playback.data.toPlaybackEngineResource
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourcePlaybackResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.Playlist
import uniffi.tidetunes_core.PlaylistId
import kotlin.math.max

class DesktopPlayerController(
    private val playerRepository: PlayerRepository,
    private val toastRepository: ToastRepositoryImpl,
    private val playlistRepository: PlaylistRepositoryImpl,
    private val storageRepository: StorageRepositoryImpl,
    private val roomLibraryStore: RoomLibraryStore,
    private val playbackResourceResolver: PlaybackResourceResolver,
    private val playbackEngine: DesktopPlaybackEngine,
    private val scope: CoroutineScope,
) : PlayerController {
    private val sleep = MutableStateFlow(SleepModeState())
    private var sleepJob: Job? = null
    private var playbackJob: Job? = null
    private var playbackResource: PlaybackResource? = null

    override val sleepState: StateFlow<SleepModeState> = sleep.asStateFlow()

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
        playbackJob = scope.launch(Dispatchers.Main) {
            playerRepository.setIsLoading(true)
            try {
                stopForPlayback()

                val music = roomLibraryStore.getMusic(id)
                val playlist = roomLibraryStore.getPlaylist(playlistId)
                val belongsToPlaylist = playlist?.musics?.any { it.meta.id == id } == true
                if (music == null || playlist == null || !belongsToPlaylist) {
                    playerRepository.resetCurrent()
                    return@launch
                }

                val resource = when (val result = playbackResourceResolver.resolve(music)) {
                    is SourcePlaybackResult.Success -> result.resource
                    is SourcePlaybackResult.Failure -> {
                        toastRepository.emitToast("Unable to open audio stream")
                        playerRepository.resetCurrent()
                        return@launch
                    }
                }
                playbackResource = resource

                when (playbackEngine.load(
                    PlaybackEngineLoadRequest(
                        item = music.toPlayableItem(playlist.abstr.meta.id.value),
                        resource = resource.toPlaybackEngineResource(),
                    )
                )) {
                    PlaybackEngineLoadResult.Ready -> {
                        playerRepository.setCurrent(music, playlist)
                        playbackEngine.play()
                        playerRepository.setIsPlaying(true)
                        playerRepository.notifyDurationChanged()
                    }
                    is PlaybackEngineLoadResult.Unsupported -> {
                        releasePlaybackResource()
                        toastRepository.emitToast("Desktop playback engine cannot load this audio stream")
                        playerRepository.resetCurrent()
                    }
                    is PlaybackEngineLoadResult.Failure -> {
                        releasePlaybackResource()
                        toastRepository.emitToast("Unable to open audio stream")
                        playerRepository.resetCurrent()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                releasePlaybackResource()
                toastRepository.emitToast(error.message?.takeIf { it.isNotBlank() } ?: error.toString())
                playerRepository.resetCurrent()
                playerRepository.setIsPlaying(false)
            } finally {
                playerRepository.setIsLoading(false)
            }
        }
    }

    override fun resume() {
        if (playbackResource == null) return
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
        if (playbackResource == null) return
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

    private suspend fun stopForPlayback() {
        playbackEngine.stop()
        releasePlaybackResource()
        playerRepository.setIsPlaying(false)
        playerRepository.resetCurrent()
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
