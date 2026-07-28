package com.github.tidetunes.core

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.github.tidetunes.shared.R
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.repository.ArtworkRepository
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.service.playback.data.toPlaybackArtwork
import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.service.playback.data.PlaybackResourceResolver
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourcePlaybackResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uniffi.tidetunes_backend.Playlist
import com.github.tidetunes.singleton.Bridge
import org.koin.android.ext.android.inject
import uniffi.tidetunes_backend.MusicAbstract
import com.github.tidetunes.core.domain.model.DiagnosticLogCategory
import com.github.tidetunes.diagnostics.TideLogger


const val PLAYER_TO_PREV_COMMAND = "PLAYER_TO_PREV_COMMAND";
const val PLAYER_TO_NEXT_COMMAND = "PLAYER_TO_NEXT_COMMAND";



class PlaybackService : MediaSessionService() {
    private val playerRepository: PlayerRepository by inject()
    private val artworkRepository: ArtworkRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val bridge: Bridge by inject()
    private val roomLibraryStore: RoomLibraryStore by inject()
    private val playbackResourceResolver: PlaybackResourceResolver by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var _mediaSession: MediaSession? = null
    private var playbackResource: PlaybackResource? = null
    private var audioFocusController: PlaybackAudioFocusController? = null
    private var lyricOutputController: AndroidLyricOutputController? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        TideLogger.info(
            DiagnosticLogCategory.Playback,
            "PlaybackService",
            "Playback service creating",
        )
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.tidetunes_notification_small_icon)
            }
        )
        val context = this

        val intent = Intent(this, Class.forName("com.github.tidetunes.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                mediaAudioAttributes(),
                false,
            )
            .setHandleAudioBecomingNoisy(AppSettings.Default.pauseOnDisconnect)
            .setWakeMode(WAKE_MODE_NETWORK)
            .setMediaSourceFactory(ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context)))
            .build()
        audioFocusController = PlaybackAudioFocusController(this, player).apply {
            updateMode(AppSettings.Default.audioFocusMode)
        }
        _mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                @OptIn(UnstableApi::class)
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    if (session.isMediaNotificationController(controller)) {
                        val customPrevCommand = SessionCommand(PLAYER_TO_PREV_COMMAND, Bundle.EMPTY)
                        val customNextCommand = SessionCommand(PLAYER_TO_NEXT_COMMAND, Bundle.EMPTY)

                        val sessionCommands =
                            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                                .add(customPrevCommand)
                                .add(customNextCommand)
                                .build()
                        val playerCommands =
                            MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                                .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                                .remove(Player.COMMAND_SEEK_TO_NEXT)
                                .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                                .remove(Player.COMMAND_SEEK_BACK)
                                .remove(Player.COMMAND_SEEK_FORWARD)
                                .remove(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                                .build()
                        // Custom layout and available commands to configure the legacy/framework session.
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setCustomLayout(
                                ImmutableList.of(
                                    CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                                        .setSessionCommand(customPrevCommand)
                                        .setDisplayName("Previous")
                                        .build(),
                                    CommandButton.Builder(CommandButton.ICON_NEXT)
                                        .setSessionCommand(customNextCommand)
                                        .setDisplayName("Next")
                                        .build(),
                                )
                            )
                            .setAvailablePlayerCommands(playerCommands)
                            .setAvailableSessionCommands(sessionCommands)
                            .build()
                    }
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == PLAYER_TO_PREV_COMMAND) {
                        playPrevious()
                    } else if (customCommand.customAction == PLAYER_TO_NEXT_COMMAND) {
                        playNext()
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
        lyricOutputController = AndroidLyricOutputController(
            context = this,
            settingsRepository = settingsRepository,
            playerRepository = playerRepository,
            roomLibraryStore = roomLibraryStore,
            scope = serviceScope,
            playerProvider = { _mediaSession?.player },
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playerRepository.setIsPlaying(isPlaying)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) audioFocusController?.requestFocus()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    playerRepository.setIsPlaying(false)
                    playOnComplete()
                } else if (playbackState == Player.STATE_READY) {
                    playerRepository.setIsLoading(false)
                    syncMetadataUtil(serviceScope, playerRepository, player)
                } else if (playbackState == Player.STATE_BUFFERING) {
                    playerRepository.setIsLoading(true)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                playerRepository.notifyDurationChanged()
            }
        })
        TideLogger.info(
            DiagnosticLogCategory.Playback,
            "PlaybackService",
            "Playback service created",
        )

        serviceScope.launch(Dispatchers.Main) {
            settingsRepository.settings.collect { settings ->
                audioFocusController?.updateMode(settings.audioFocusMode)
                player.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
            }
        }

        serviceScope.launch(Dispatchers.Main) {
            playerRepository.pauseRequest.collect {
                val player = _mediaSession?.player ?: return@collect

                if (player.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                    player.pause()
                } else {
                    TideLogger.warn(
                        DiagnosticLogCategory.Playback,
                        "PlaybackService",
                        "Pause command is unavailable",
                    )
                }
            }
        }

        serviceScope.launch(Dispatchers.Main) {
            playerRepository.music.collectLatest { music ->
                music ?: return@collectLatest
                val artworkData = withContext(Dispatchers.IO) {
                    artworkRepository.load(music.toPlaybackArtwork())
                } ?: return@collectLatest
                val currentItem = player.currentMediaItem ?: return@collectLatest
                if (currentItem.mediaId != music.meta.id.value.toString()) return@collectLatest
                if (!player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
                    return@collectLatest
                }
                player.replaceMediaItem(
                    player.currentMediaItemIndex,
                    currentItem.buildUpon()
                        .setMediaMetadata(currentItem.mediaMetadata.withArtworkData(artworkData))
                        .build(),
                )
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return _mediaSession
    }

    override fun onDestroy() {
        super.onDestroy()
        _mediaSession?.player?.stop()
        runBlocking {
            releasePlaybackResource()
        }
        _mediaSession?.player?.release()
        lyricOutputController?.destroy()
        lyricOutputController = null
        audioFocusController?.release()
        audioFocusController = null
        _mediaSession?.release()
        _mediaSession = null
        serviceScope.cancel()
    }


    fun play(musicAbstract: MusicAbstract, playlist: Playlist) {
        val player = _mediaSession?.player ?: return

        serviceScope.launch {
            releasePlaybackResource()
            val music = roomLibraryStore.getMusic(musicAbstract.meta.id) ?: return@launch
            val resource = when (val result = playbackResourceResolver.resolve(music)) {
                is SourcePlaybackResult.Success -> result.resource
                is SourcePlaybackResult.Failure -> {
                    TideLogger.error(
                        DiagnosticLogCategory.Playback,
                        "PlaybackService",
                        "Playback resource resolution failed",
                        result.reason.toString(),
                    )
                    playerRepository.resetCurrent()
                    return@launch
                }
            }
            playbackResource = resource
            playerRepository.setCurrent(music, playlist)
            playUtil(
                cx = BuildMediaContext(bridge = bridge, scope = serviceScope),
                music = music,
                player = player as ExoPlayer,
                playbackUri = resource.uri,
            )
        }
    }

    private suspend fun releasePlaybackResource() {
        val resource = playbackResource ?: return
        playbackResource = null
        playbackResourceResolver.release(resource)
    }

    private fun playOnComplete() {
        val m = playerRepository.onCompleteMusic.value
        val p = playerRepository.playlist.value
        if (m != null && p != null) {
            play(m, p)
        }
    }

    private fun playNext() {
        val m = playerRepository.nextMusic.value
        val p = playerRepository.playlist.value
        if (m != null && p != null) {
            play(m, p)
        }
    }

    private fun playPrevious() {
        val m = playerRepository.previousMusic.value
        val p = playerRepository.playlist.value
        if (m != null && p != null) {
            play(m, p)
        }
    }
}

private class PlaybackAudioFocusController(
    context: Context,
    private val player: Player,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mode = AudioFocusMode.Pause
    private var resumeOnGain = false
    private var ducked = false
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener(::onAudioFocusChange)
        .build()

    fun updateMode(value: AudioFocusMode) {
        mode = value
        if (mode == AudioFocusMode.Mix) {
            restoreVolume()
            resumeOnGain = false
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }

    fun requestFocus() {
        if (mode == AudioFocusMode.Mix) return
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) player.pause()
    }

    fun release() {
        restoreVolume()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun onAudioFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                restoreVolume()
                if (resumeOnGain) {
                    resumeOnGain = false
                    player.play()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                restoreVolume()
                resumeOnGain = false
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseForTransientLoss()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                when (mode) {
                    AudioFocusMode.Pause -> pauseForTransientLoss()
                    AudioFocusMode.Duck -> {
                        player.volume = DUCK_VOLUME
                        ducked = true
                    }
                    AudioFocusMode.Mix -> Unit
                }
            }
        }
    }

    private fun pauseForTransientLoss() {
        resumeOnGain = player.isPlaying
        player.pause()
    }

    private fun restoreVolume() {
        if (ducked) player.volume = 1f
        ducked = false
    }
}

private const val DUCK_VOLUME = 0.2f

private fun mediaAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
}
