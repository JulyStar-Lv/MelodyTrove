package com.github.tidetunes.service.playback.data

import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import com.github.tidetunes.service.playback.domain.PlaybackQueue
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.domain.PlayerState
import com.github.tidetunes.service.playback.domain.RepeatMode
import com.github.tidetunes.service.playback.data.PlayerController as LegacyPlayerController
import com.github.tidetunes.service.playback.data.PlayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import uniffi.tidetunes_backend.Music
import uniffi.tidetunes_backend.MusicAbstract
import uniffi.tidetunes_backend.MusicId
import uniffi.tidetunes_backend.PlayMode
import uniffi.tidetunes_backend.Playlist
import uniffi.tidetunes_backend.PlaylistId

class LegacyPlaybackController(
    private val playerRepository: PlayerRepository,
    private val legacyController: LegacyPlayerController,
    private val scope: CoroutineScope,
    private val positionPollMillis: Long = 100,
) : PlaybackController {
    private val immediatePositionRefreshes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
    )

    override val state: StateFlow<PlayerState> = combine(
        playerRepository.music,
        playerRepository.playing,
        playerRepository.loading,
        playerRepository.playMode,
    ) { music, playing, loading, playMode ->
        legacyPlayerState(
            music = music,
            playing = playing,
            loading = loading,
            playMode = playMode,
            playlistId = playerRepository.playlist.value?.abstr?.meta?.id?.value,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlayerState())

    override val position: StateFlow<PlaybackPosition> = merge(
        flow {
            while (true) {
                emit(Unit)
                delay(positionPollMillis.coerceAtLeast(100))
            }
        },
        playerRepository.durationChanged,
        immediatePositionRefreshes,
    ).map {
        readPosition()
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5_000),
        PlaybackPosition.Zero,
    )

    override val queue: StateFlow<PlaybackQueue> = combine(
        playerRepository.playlist,
        playerRepository.music,
    ) { playlist, music ->
        legacyPlaybackQueue(
            playlist = playlist,
            currentMusic = music,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlaybackQueue.Empty)

    override suspend fun play(
        items: List<PlayableItem>,
        startIndex: Int,
    ) {
        val item = items.getOrNull(startIndex) ?: return
        val musicId = item.libraryTrackId ?: return
        val playlistId = item.libraryPlaylistId
            ?: playerRepository.playlist.value?.abstr?.meta?.id?.value
            ?: return
        legacyController.play(MusicId(musicId), PlaylistId(playlistId))
    }

    override fun play() {
        legacyController.resume()
    }

    override fun pause() {
        legacyController.pause()
    }

    override fun togglePlayPause() {
        if (state.value.status == PlaybackStatus.Playing) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        legacyController.seek(positionMs.coerceAtLeast(0).toULong())
        immediatePositionRefreshes.tryEmit(Unit)
    }

    override fun skipNext() {
        legacyController.playNext()
    }

    override fun skipPrevious() {
        legacyController.playPrevious()
    }

    override fun setShuffle(enabled: Boolean) = Unit

    override fun setRepeatMode(mode: RepeatMode) {
        playerRepository.setPlayMode(
            mode.toLegacyPlayMode(queue.value.items.size)
        )
    }

    override fun moveQueueItem(from: Int, to: Int) = Unit

    override fun removeQueueItem(index: Int) {
        if (index == queue.value.currentIndex) {
            legacyController.stop()
            immediatePositionRefreshes.tryEmit(Unit)
        }
    }

    override fun clearQueue() {
        legacyController.stop()
        immediatePositionRefreshes.tryEmit(Unit)
    }

    private fun readPosition(): PlaybackPosition {
        return PlaybackPosition(
            positionMs = legacyController.getCurrentPosition().coerceAtLeast(0),
            bufferedMs = legacyController.getBufferedPosition().coerceAtLeast(0),
            durationMs = legacyController.getDuration().coerceAtLeast(0),
        )
    }
}

internal fun legacyPlayerState(
    music: Music?,
    playing: Boolean,
    loading: Boolean,
    playMode: PlayMode,
    playlistId: Long?,
): PlayerState {
    val currentItem = music?.toPlayableItem(playlistId)
    return PlayerState(
        currentItem = currentItem,
        status = when {
            loading -> PlaybackStatus.Loading
            currentItem == null -> PlaybackStatus.Idle
            playing -> PlaybackStatus.Playing
            else -> PlaybackStatus.Paused
        },
        repeatMode = playMode.toRepeatMode(),
        shuffleEnabled = false,
    )
}

internal fun legacyPlaybackQueue(
    playlist: Playlist?,
    currentMusic: Music?,
): PlaybackQueue {
    if (playlist == null) return PlaybackQueue.Empty
    val playlistId = playlist.abstr.meta.id.value
    val items = playlist.musics.map { music ->
        music.toPlayableItem(playlistId)
    }
    val currentIndex = currentMusic?.meta?.id?.value?.let { currentId ->
        playlist.musics.indexOfFirst { music -> music.meta.id.value == currentId }
    } ?: -1
    return PlaybackQueue(
        items = items,
        currentIndex = currentIndex,
    )
}

private fun PlayMode.toRepeatMode(): RepeatMode {
    return when (this) {
        PlayMode.SINGLE,
        PlayMode.LIST -> RepeatMode.Off
        PlayMode.SINGLE_LOOP -> RepeatMode.One
        PlayMode.LIST_LOOP -> RepeatMode.All
    }
}

private fun RepeatMode.toLegacyPlayMode(queueSize: Int): PlayMode {
    return when (this) {
        RepeatMode.Off -> if (queueSize > 1) PlayMode.LIST else PlayMode.SINGLE
        RepeatMode.One -> PlayMode.SINGLE_LOOP
        RepeatMode.All -> PlayMode.LIST_LOOP
    }
}
