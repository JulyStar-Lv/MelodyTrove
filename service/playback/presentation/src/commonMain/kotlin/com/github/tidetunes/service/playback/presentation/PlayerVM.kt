package com.github.tidetunes.service.playback.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.PlaybackAdvancedSettings
import com.github.tidetunes.core.domain.model.CurrentTrackInfo
import com.github.tidetunes.core.domain.model.PlayerInteractionSettings
import com.github.tidetunes.core.domain.model.PreviousButtonBehavior
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.domain.repository.ExternalEditorKind
import com.github.tidetunes.core.domain.repository.ExternalEditorLauncher
import com.github.tidetunes.core.domain.repository.ExternalEditorRequest
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import com.github.tidetunes.service.playback.domain.NowPlayingRepository
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.domain.RepeatMode
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingAction
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingEvent
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingState
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingTrackItem
import com.github.tidetunes.service.playback.presentation.nowplaying.toNowPlayingControlsState
import com.github.tidetunes.service.playback.presentation.nowplaying.toNowPlayingQueueState
import com.github.tidetunes.service.playback.presentation.nowplaying.toNowPlayingTrackItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PlayerVM constructor(
    private val nowPlayingRepository: NowPlayingRepository,
    private val playbackController: PlaybackController,
    private val enqueueDownload: EnqueueDownloadUseCase,
    private val settingsRepository: SettingsRepository,
    private val externalEditorLauncher: ExternalEditorLauncher,
) : ViewModel() {
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)
    private val _nowPlayingState = MutableStateFlow(
        NowPlayingState(externalEditorSupported = externalEditorLauncher.isSupported)
    )
    private val _nowPlayingEvents = Channel<NowPlayingEvent>(Channel.BUFFERED)
    private val _playbackAdvancedSettings = MutableStateFlow(PlaybackAdvancedSettings.Default)
    private var playerInteractionSettings = PlayerInteractionSettings.Default
    private var currentTrackInfo: CurrentTrackInfo? = null

    val playbackState = playbackController.state
    val playbackPosition = playbackController.position
    val playbackQueue = playbackController.queue
    val nowPlayingState = _nowPlayingState.asStateFlow()
    val nowPlayingEvents = _nowPlayingEvents.receiveAsFlow()

    val playing = playbackState.map { state ->
        state.status == PlaybackStatus.Playing
    }.stateIn(viewModelScope, whileSubscribed, false)

    val currentDuration = playbackPosition.map { position ->
        position.positionMs.milliseconds
    }.stateIn(viewModelScope, whileSubscribed, 0.milliseconds)

    val bufferDuration = playbackPosition.map { position ->
        position.bufferedMs.milliseconds
    }.stateIn(viewModelScope, whileSubscribed, 0.milliseconds)

    val playerDuration = playbackPosition.map { position ->
        position.durationMs.milliseconds
    }.stateIn(viewModelScope, whileSubscribed, 0.milliseconds)

    val loading = playbackState.map { state ->
        state.status == PlaybackStatus.Loading
    }.stateIn(viewModelScope, whileSubscribed, false)

    val lyricIndex = combine(currentDuration, nowPlayingState) {
        currentDuration, nowPlayingState ->
            nowPlayingState.currentTrack?.lyrics?.lines?.indexOfLast { it.duration <= currentDuration } ?: -1
    }.stateIn(viewModelScope, SharingStarted.Lazily, -1)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _playbackAdvancedSettings.value = settings.playbackAdvanced
                playerInteractionSettings = settings.playerInteraction
            }
        }
        viewModelScope.launch {
            nowPlayingRepository.currentTrackInfo.collect { info ->
                currentTrackInfo = info
                _nowPlayingState.value = _nowPlayingState.value.copy(
                    currentTrack = info?.toNowPlayingTrackItem(),
                )
            }
        }
        viewModelScope.launch {
            combine(
                nowPlayingRepository.previousArtwork,
                nowPlayingRepository.nextArtwork,
                nowPlayingRepository.canPlayPrevious,
                nowPlayingRepository.canPlayNext,
                playbackQueue,
            ) { prevArt, nextArt, canPlayPrevious, canPlayNext, queue ->
                queue.toNowPlayingQueueState(
                    previousArtwork = prevArt,
                    nextArtwork = nextArt,
                    canPlayPrevious = canPlayPrevious,
                    canPlayNext = canPlayNext,
                )
            }.collect { queue ->
                _nowPlayingState.value = _nowPlayingState.value.copy(queue = queue)
            }
        }
        viewModelScope.launch {
            playbackState.collect { state ->
                _nowPlayingState.value = _nowPlayingState.value.copy(
                    currentTrack = _nowPlayingState.value.currentTrack?.copy(
                        artist = state.currentItem?.artist?.takeIf { it.isNotBlank() },
                    ),
                    controls = state.toNowPlayingControlsState(),
                )
            }
        }
    }

    fun onNowPlayingAction(action: NowPlayingAction) {
        when (action) {
            NowPlayingAction.NavigateBack -> Unit
            NowPlayingAction.AddLyric -> Unit
            NowPlayingAction.SearchMetadata -> Unit
            NowPlayingAction.OpenMetadataEditor -> openExternalEditor(ExternalEditorKind.Metadata)
            NowPlayingAction.OpenLyricTimingEditor -> openExternalEditor(ExternalEditorKind.LyricTiming)
            NowPlayingAction.RemoveLyric -> removeLyric()
            NowPlayingAction.RemoveCurrentTrack -> remove()
            NowPlayingAction.DownloadCurrentTrack -> downloadCurrentTrack()
            NowPlayingAction.OpenSleepTimer -> Unit
            NowPlayingAction.OpenLyrics -> Unit
            NowPlayingAction.OpenQueue -> Unit
            NowPlayingAction.PlayPrevious -> playPrevious()
            NowPlayingAction.PlayNext -> playNext()
            NowPlayingAction.Resume -> resume()
            NowPlayingAction.Pause -> pause()
            NowPlayingAction.CycleRepeatMode -> changePlayModeToNext()
            is NowPlayingAction.SeekTo -> seek(action.positionMs)
        }
    }

    fun resume() {
        playbackController.play()
    }

    fun pause() {
        playbackController.pause()
    }

    fun stop() {
        playbackController.clearQueue()
    }

    fun playNext() {
        playbackController.skipNext()
    }

    fun playPrevious() {
        when (_playbackAdvancedSettings.value.previousButtonBehavior) {
            PreviousButtonBehavior.PreviousTrack -> playbackController.skipPrevious()
            PreviousButtonBehavior.RestartCurrentTrack -> playbackController.seekTo(0L)
        }
    }

    fun remove() {
        nowPlayingRepository.removeCurrentTrack()
    }

    fun seek(ms: ULong) {
        playbackController.seekTo(ms.toPlaybackPositionMs())
    }

    fun play(trackId: Long, playlistId: Long) {
        viewModelScope.launch {
            playbackController.play(
                items = listOf(
                    PlayableItem(
                        title = "Track $trackId",
                        libraryTrackId = trackId,
                        libraryPlaylistId = playlistId,
                    )
                ),
            )
        }
    }

    fun changePlayModeToNext() {
        playbackController.setRepeatMode(playbackState.value.repeatMode.next())
    }

    fun removeLyric() {
        nowPlayingRepository.removeCurrentLyrics()
    }

    private fun downloadCurrentTrack() {
        val track = _nowPlayingState.value.currentTrack
        val mediaId = track?.mediaId
        if (track == null || mediaId == null) {
            viewModelScope.launch {
                _nowPlayingEvents.send(NowPlayingEvent.ShowMessage("This track cannot be downloaded yet."))
            }
            return
        }
        enqueueTrackDownload(track)
    }

    private fun openExternalEditor(kind: ExternalEditorKind) {
        val track = currentTrackInfo ?: return
        val launched = externalEditorLauncher.launch(
            ExternalEditorRequest(
                kind = kind,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                sourcePath = track.sourcePath,
                metadataEditor = playerInteractionSettings.metadataEditor,
                lyricTimingEditor = playerInteractionSettings.lyricTimingEditor,
            )
        )
        if (!launched) {
            viewModelScope.launch {
                _nowPlayingEvents.send(
                    NowPlayingEvent.ShowMessage("Unable to open an editor for this track")
                )
            }
        }
    }

    private fun enqueueTrackDownload(track: NowPlayingTrackItem) {
        val mediaId = track.mediaId ?: return
        viewModelScope.launch {
            try {
                enqueueDownload(
                    DownloadRequest(
                        mediaId = mediaId,
                        title = track.title,
                        durationMs = track.durationMs,
                    )
                )
                _nowPlayingEvents.send(NowPlayingEvent.ShowMessage("Added to Downloads."))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _nowPlayingEvents.send(
                    NowPlayingEvent.ShowMessage(
                        exception.message?.takeIf { it.isNotBlank() } ?: "Failed to add download.",
                    )
                )
            }
        }
    }
}

private fun ULong.toPlaybackPositionMs(): Long {
    val max = Long.MAX_VALUE.toULong()
    return if (this > max) Long.MAX_VALUE else toLong()
}

private fun RepeatMode.next(): RepeatMode {
    return when (this) {
        RepeatMode.Off -> RepeatMode.One
        RepeatMode.One -> RepeatMode.All
        RepeatMode.All -> RepeatMode.Off
    }
}
