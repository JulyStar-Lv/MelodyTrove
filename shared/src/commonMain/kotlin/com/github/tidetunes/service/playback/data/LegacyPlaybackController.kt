package com.github.tidetunes.service.playback.data

import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import com.github.tidetunes.service.playback.domain.PlaybackQueue
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.domain.PlayerState
import com.github.tidetunes.service.playback.domain.RepeatMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.PlayNextMode
import com.github.tidetunes.core.domain.model.PlaybackAdvancedSettings
import com.github.tidetunes.core.domain.model.ShuffleStrategy
import com.github.tidetunes.core.domain.model.StartupPlaybackMode
import com.github.tidetunes.core.domain.model.LIBRARY_PLAYBACK_PLAYLIST_ID
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.service.playback.data.PlayerController as LegacyPlayerController
import com.github.tidetunes.service.playback.data.PlayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    private val settingsRepository: SettingsRepository? = null,
) : PlaybackController {
    private val immediatePositionRefreshes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
    )
    private val settings = settingsRepository?.settings?.stateIn(
        scope,
        SharingStarted.Eagerly,
        AppSettings.Default,
    )
    private val shuffleEnabled = MutableStateFlow(false)
    private val requestedNext = mutableListOf<MusicAbstract>()

    init {
        scope.launch { restoreStartupPlayback() }
        scope.launch {
            playerRepository.music.collect { current ->
                val currentId = current?.meta?.id ?: return@collect
                requestedNext.removeAll { it.meta.id == currentId }
            }
        }
        scope.launch {
            while (isActive) {
                delay(2_000)
                if (settings?.value?.playbackAdvanced?.resumePlaybackPosition != false) {
                    playerRepository.savePlaybackSession(
                        positionMs = readPosition().positionMs,
                        wasPlaying = state.value.status == PlaybackStatus.Playing,
                    )
                }
            }
        }
    }

    private val legacyState: StateFlow<PlayerState> = combine(
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

    override val state: StateFlow<PlayerState> = combine(
        legacyState,
        shuffleEnabled,
    ) { state, shuffled ->
        state.copy(shuffleEnabled = shuffled)
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
        val saved = playerRepository.persistedPlaybackSession()
        legacyController.play(MusicId(musicId), PlaylistId(playlistId))
        if (
            settings?.value?.playbackAdvanced?.resumePlaybackPosition != false &&
            saved?.trackId == musicId &&
            saved.playlistId == playlistId &&
            saved.positionMs > 0L
        ) {
            seekAfterTrackLoads(musicId, saved.positionMs)
        }
    }

    override fun play() {
        legacyController.resume()
    }

    override fun pause() {
        scope.launch { playerRepository.savePlaybackSession(readPosition().positionMs, false) }
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
        val advanced = settings?.value?.playbackAdvanced ?: PlaybackAdvancedSettings.Default
        if (shuffleEnabled.value && advanced.shuffleStrategy == ShuffleStrategy.TrueRandom) {
            val playlist = playerRepository.playlist.value ?: return
            val currentId = playerRepository.music.value?.meta?.id
            val next = playlist.musics.filterNot { it.meta.id == currentId }.randomOrNull() ?: return
            legacyController.play(next.meta.id, playlist.abstr.meta.id)
        } else {
            legacyController.playNext()
        }
    }

    override fun skipPrevious() {
        legacyController.playPrevious()
    }

    override fun enqueueNext(item: PlayableItem) {
        val trackId = item.libraryTrackId ?: return
        scope.launch {
            val currentId = playerRepository.music.value?.meta?.id?.value ?: return@launch
            if (trackId == currentId) return@launch
            val music = playerRepository.musicAbstract(trackId) ?: return@launch
            requestedNext.removeAll { it.meta.id.value == trackId }
            when (settings?.value?.playbackAdvanced?.playNextMode ?: PlayNextMode.FirstRequestedFirst) {
                PlayNextMode.FirstRequestedFirst -> requestedNext.add(music)
                PlayNextMode.LastRequestedFirst -> requestedNext.add(0, music)
            }
            rebuildRequestedNextQueue()
        }
    }

    override fun setShuffle(enabled: Boolean) {
        if (shuffleEnabled.value == enabled) return
        shuffleEnabled.value = enabled
        requestedNext.clear()
        scope.launch {
            val strategy = settings?.value?.playbackAdvanced?.shuffleStrategy
                ?: ShuffleStrategy.QueueOrder
            if (!enabled || strategy == ShuffleStrategy.TrueRandom) {
                if (!enabled) playerRepository.restorePlaybackQueueOrder()
                return@launch
            }
            val playlist = playerRepository.playlist.value ?: return@launch
            val currentId = playerRepository.music.value?.meta?.id
            val current = playlist.musics.firstOrNull { it.meta.id == currentId }
            val shuffled = playlist.musics.filterNot { it.meta.id == currentId }.shuffled()
            playerRepository.replacePlaybackQueue(listOfNotNull(current) + shuffled)
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        playerRepository.setPlayMode(
            mode.toLegacyPlayMode(queue.value.items.size)
        )
    }

    override fun moveQueueItem(from: Int, to: Int) {
        val musics = playerRepository.playlist.value?.musics?.toMutableList() ?: return
        if (from !in musics.indices || to !in musics.indices || from == to) return
        musics.add(to, musics.removeAt(from))
        playerRepository.replacePlaybackQueue(musics)
    }

    override fun removeQueueItem(index: Int) {
        val playlist = playerRepository.playlist.value ?: return
        if (index !in playlist.musics.indices) return
        val removingCurrent = playlist.musics[index].meta.id == playerRepository.music.value?.meta?.id
        val musics = playlist.musics.toMutableList().apply { removeAt(index) }
        requestedNext.removeAll { queued -> musics.none { it.meta.id == queued.meta.id } }
        playerRepository.replacePlaybackQueue(musics)
        if (removingCurrent) {
            val replacement = musics.getOrNull(index) ?: musics.lastOrNull()
            if (replacement == null) legacyController.stop()
            else legacyController.play(replacement.meta.id, playlist.abstr.meta.id)
            immediatePositionRefreshes.tryEmit(Unit)
        }
    }

    override fun clearQueue() {
        scope.launch { playerRepository.savePlaybackSession(readPosition().positionMs, false) }
        legacyController.stop()
        immediatePositionRefreshes.tryEmit(Unit)
    }

    private fun readPosition(): PlaybackPosition {
        return legacyPlaybackPosition(
            currentPositionMs = legacyController.getCurrentPosition(),
            bufferedPositionMs = legacyController.getBufferedPosition(),
            durationMs = legacyController.getDuration(),
            pendingSeekPositionMs = legacyController.getPendingSeekPosition(),
        )
    }

    private suspend fun restoreStartupPlayback() {
        val repository = settingsRepository ?: return
        val advanced = repository.settings.first().playbackAdvanced
        val saved = playerRepository.persistedPlaybackSession()
        val target = when (advanced.startupPlaybackMode) {
            StartupPlaybackMode.Off -> null
            StartupPlaybackMode.ResumeLastQueue -> saved?.trackId?.let { trackId ->
                trackId to saved.playlistId
            }
            StartupPlaybackMode.ShuffleLibrary -> {
                playerRepository.randomTrackInPlaylist(LIBRARY_PLAYBACK_PLAYLIST_ID)?.let { trackId ->
                    trackId to LIBRARY_PLAYBACK_PLAYLIST_ID
                }
            }
        } ?: return
        legacyController.play(MusicId(target.first), PlaylistId(target.second))
        if (
            advanced.startupPlaybackMode == StartupPlaybackMode.ResumeLastQueue &&
            advanced.resumePlaybackPosition &&
            saved != null &&
            saved.positionMs > 0L
        ) {
            seekAfterTrackLoads(target.first, saved.positionMs)
        }
    }

    private suspend fun seekAfterTrackLoads(trackId: Long, positionMs: Long) {
        withTimeoutOrNull(5_000) {
            playerRepository.music.filter { music -> music?.meta?.id?.value == trackId }.first()
        } ?: return
        legacyController.seek(positionMs.coerceAtLeast(0L).toULong())
        immediatePositionRefreshes.tryEmit(Unit)
    }

    private fun rebuildRequestedNextQueue() {
        val playlist = playerRepository.playlist.value ?: return
        val currentId = playerRepository.music.value?.meta?.id ?: return
        val requestedIds = requestedNext.map { it.meta.id }.toSet()
        val base = playlist.musics.filterNot { it.meta.id in requestedIds }.toMutableList()
        val currentIndex = base.indexOfFirst { it.meta.id == currentId }
        if (currentIndex < 0) return
        base.addAll(currentIndex + 1, requestedNext)
        playerRepository.replacePlaybackQueue(base)
    }
}

internal fun legacyPlaybackPosition(
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    pendingSeekPositionMs: Long?,
): PlaybackPosition {
    return PlaybackPosition(
        positionMs = (pendingSeekPositionMs ?: currentPositionMs).coerceAtLeast(0),
        bufferedMs = bufferedPositionMs.coerceAtLeast(0),
        durationMs = durationMs.coerceAtLeast(0),
        isSeeking = pendingSeekPositionMs != null,
    )
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
