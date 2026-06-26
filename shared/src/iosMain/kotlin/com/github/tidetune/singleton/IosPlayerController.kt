package com.github.tidetune.singleton

import com.github.tidetune.platform.currentTimeMillis
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.*
import uniffi.tidetune_core.MusicId
import uniffi.tidetune_core.PlaybackSession
import uniffi.tidetune_core.Playlist
import uniffi.tidetune_core.PlaylistId
import uniffi.tidetune_core.ctCreatePlaybackSession
import uniffi.tidetune_core.tidetuneError
import uniffi.tidetune_core.tidetuneLog
import kotlin.math.max

@OptIn(ExperimentalForeignApi::class)
class IosPlayerController(
    private val playerRepository: PlayerRepository,
    private val toastRepository: ToastRepository,
    private val playlistRepository: PlaylistRepository,
    private val storageRepository: StorageRepository,
    private val bridge: Bridge,
    private val roomLibraryStore: RoomLibraryStore,
    private val scope: CoroutineScope,
) : PlayerController {
    private val player = AVPlayer()
    private val sleep = MutableStateFlow(SleepModeState())
    private var sleepJob: Job? = null
    private var playbackJob: Job? = null
    private var playbackSession: PlaybackSession? = null

    override val sleepState = sleep.asStateFlow()

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

    override fun getCurrentPosition(): Long {
        val seconds = CMTimeGetSeconds(player.currentTime())
        return if (seconds.isFinite() && seconds >= 0.0) (seconds * 1_000.0).toLong() else 0L
    }

    override fun getBufferedPosition(): Long = getCurrentPosition()

    override fun getDuration(): Long {
        val seconds = player.currentItem?.let { CMTimeGetSeconds(it.duration) } ?: return 0L
        return if (seconds.isFinite() && seconds >= 0.0) (seconds * 1_000.0).toLong() else 0L
    }

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
                    player.pause()
                    closePlaybackSession()
                    val music = roomLibraryStore.getMusic(id)
                    val playlist = roomLibraryStore.getPlaylist(playlistId)
                    val belongsToPlaylist = playlist?.musics?.any { it.meta.id == id } == true
                    if (music == null || playlist == null || !belongsToPlaylist) {
                        playerRepository.resetCurrent()
                        return@launch
                    }

                    val storage = storageRepository.storageForRust(music.loc.storageId)
                    val session = if (storage == null) {
                        null
                    } else {
                        bridge.run { ctCreatePlaybackSession(it, storage, music.loc) }
                    }
                val url = session?.url()?.let { NSURL.URLWithString(it) }
                if (session == null || url == null) {
                    session?.shutdown()
                    toastRepository.emitToast("Unable to open audio stream")
                    return@launch
                }

                playbackSession = session
                playerRepository.setCurrent(music, playlist)
                val item = AVPlayerItem.playerItemWithURL(url)
                player.replaceCurrentItemWithPlayerItem(item)
                player.play()
                playerRepository.setIsPlaying(true)
                playerRepository.notifyDurationChanged()
            } catch (error: Exception) {
                tidetuneError("iOS playback failed: $error")
                toastRepository.emitToast(error.toString())
                playerRepository.resetCurrent()
            } finally {
                playerRepository.setIsLoading(false)
            }
        }
    }

    override fun resume() {
        player.play()
        playerRepository.setIsPlaying(true)
    }

    override fun pause() {
        player.pause()
        playerRepository.setIsPlaying(false)
    }

    override fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        closePlaybackSession()
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
        player.seekToTime(CMTimeMake(value = ms.toLong(), timescale = 1_000))
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

    private fun closePlaybackSession() {
        playbackSession?.let { session ->
            val stats = session.stats()
            tidetuneLog(
                "iOS playback range stats: requests=${stats.remoteRequests}, " +
                    "bytes=${stats.remoteBytes}"
            )
            session.shutdown()
        }
        playbackSession = null
    }
}
