package com.github.tidetune.singleton

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_STOP
import androidx.media3.session.MediaController
import com.github.tidetune.core.BuildMediaContext
import com.github.tidetune.core.playUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.tidetune_core.Playlist
import uniffi.tidetune_core.tidetuneError
import uniffi.tidetune_core.tidetuneLog
import uniffi.tidetune_core.MusicId
import uniffi.tidetune_core.PlaylistId
import kotlin.math.max

class PlayerControllerRepository(
    private val playerRepository: PlayerRepository,
    private val toastRepository: ToastRepository,
    private val playlistRepository: PlaylistRepository,
    private val storageRepository: StorageRepository,
    private val bridge: Bridge,
    private val roomLibraryStore: RoomLibraryStore,
    private val _scope: CoroutineScope
) : PlayerController {
    private var _mediaController: MediaController? = null
    private val _playlist = playerRepository.playlist
    private val _music = playerRepository.music
    private val _sleep = MutableStateFlow(SleepModeState())

    private var _sleepJob: Job? = null
    private val nextMusic = playerRepository.nextMusic
    private val previousMusic = playerRepository.previousMusic

    override val sleepState = _sleep.asStateFlow()

    init {
        _scope.launch(Dispatchers.Main) {
            playlistRepository.preRemovePlaylistEvent.collect { id ->
                if (_playlist.value?.abstr?.meta?.id == id) {
                    stop()
                }
            }
        }
        _scope.launch(Dispatchers.Main) {
            playlistRepository.preRemoveMusicEvent.collect { arg ->
                if (_playlist.value?.abstr?.meta?.id == arg.playlistId && _music.value?.meta?.id == arg.musicId) {
                    stop()
                }
            }
        }
        _scope.launch(Dispatchers.Main) {
            storageRepository.preRemoveStorageEvent.collect { id ->
                if (_music.value?.loc?.storageId == id) {
                    stop()
                }
            }
        }
    }

    fun setupMediaController(mediaController: MediaController) {
        _mediaController = mediaController

        mediaController.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                _scope.launch {
                    toastRepository.emitToast(error.toString())
                }
            }
        })
        _scope.launch {
            playerRepository.reload()
        }
        tidetuneLog("media controller setup")
    }

    fun destroyMediaController() {
        _mediaController?.release()
        _mediaController = null

        tidetuneLog("media controller destroy")
    }

    override fun getCurrentPosition(): Long {
        return _mediaController?.currentPosition ?: 0
    }

    override fun getBufferedPosition(): Long {
        return _mediaController?.bufferedPosition ?: 0
    }

    override fun getDuration(): Long {
        return (_mediaController?.duration ?: 0).coerceAtLeast(0)
    }

    override fun play(id: MusicId, playlistId: PlaylistId) {
        val mediaController = _mediaController ?: return

        if (_music.value?.meta?.id == id && _playlist.value?.abstr?.meta?.id == playlistId) {
            resume()
            return
        }

        _scope.launch(Dispatchers.Main) {
            stop()

            val music = roomLibraryStore.getMusic(id)
            val playlist = roomLibraryStore.getPlaylist(playlistId)
            val inPlaylist = music != null && playlist != null && playlist.musics.find { music -> music.meta.id == id }.let { it -> it != null }

            if (inPlaylist) {
                playerRepository.setCurrent(music!!, playlist!!)

                playUtil(BuildMediaContext(bridge = bridge, scope = _scope), music, mediaController)
            } else {
                playerRepository.resetCurrent()
            }
        }
    }

    override fun resume() {
        val mediaController = _mediaController ?: return

        if (mediaController.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
            mediaController.play()
        } else {
            tidetuneError("media controller resume failed, command COMMAND_PLAY_PAUSE is unavailable")
        }
    }

    override fun pause() {
        val mediaController = _mediaController ?: return

        if (mediaController.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
            mediaController.pause()
        } else {
            tidetuneError("media controller pause failed, command COMMAND_PLAY_PAUSE is unavailable")
        }
    }

    override fun stop() {
        val mediaController = _mediaController ?: return

        if (mediaController.isCommandAvailable(COMMAND_STOP)) {
            mediaController.stop()
        } else {
            tidetuneError("media controller stop failed, command COMMAND_STOP is unavailable")
        }

        playerRepository.resetCurrent()
    }

    override fun playNext() {
        val m = nextMusic.value
        val p = _playlist.value
        if (m != null && p != null) {
            play(m.meta.id, p.abstr.meta.id)
        }
    }

    override fun playPrevious() {
        val m = previousMusic.value
        val p = _playlist.value
        if (m != null && p != null) {
            play(m.meta.id, p.abstr.meta.id)
        }
    }

    override fun seek(ms: ULong) {
        val mediaController = _mediaController ?: return

        if (mediaController.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            mediaController.seekTo(ms.toLong())
        } else {
            tidetuneError("media controller seek failed, command COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM is unavailable")
        }
    }

    override fun scheduleSleep(newExpiredMs: Long) {
        _sleepJob?.cancel()

        val delayMs = max(newExpiredMs - System.currentTimeMillis(), 0)
        _sleepJob = _scope.launch {
            _sleep.update { state -> state.copy(enabled = true, expiredMs = newExpiredMs) }
            tidetuneLog("schedule sleep")
            delay(delayMs)
            tidetuneLog("sleep scheduled")
            playerRepository.emitPauseRequest()
            _sleep.update { state -> state.copy(enabled = false, expiredMs = 0) }
        }
    }

    override fun refreshPlaylistIfMatch(playlist: Playlist) {
        playerRepository.refreshPlaylistIfMatch(playlist)
    }

    override fun cancelSleep() {
        _sleepJob?.cancel()
        _sleepJob = null
        _sleep.update { state -> state.copy(enabled = false, expiredMs = 0) }
    }

    fun remove() {
        val m = _music.value
        val p = _playlist.value
        _scope.launch {
            if (m != null && p != null) {
                playlistRepository.removeMusic(p.abstr.meta.id, m.meta.id)
            }
        }
    }
}
