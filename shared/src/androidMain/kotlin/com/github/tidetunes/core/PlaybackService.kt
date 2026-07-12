package com.github.tidetunes.core

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.service.playback.data.PlaybackResourceResolver
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourcePlaybackResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uniffi.tidetunes_backend.Playlist
import com.github.tidetunes.singleton.Bridge
import org.koin.android.ext.android.inject
import uniffi.tidetunes_backend.MusicAbstract
import uniffi.tidetunes_backend.tidetunesError
import uniffi.tidetunes_backend.tidetunesLog


const val PLAYER_TO_PREV_COMMAND = "PLAYER_TO_PREV_COMMAND";
const val PLAYER_TO_NEXT_COMMAND = "PLAYER_TO_NEXT_COMMAND";



class PlaybackService : MediaSessionService() {
    private val playerRepository: PlayerRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val bridge: Bridge by inject()
    private val roomLibraryStore: RoomLibraryStore by inject()
    private val playbackResourceResolver: PlaybackResourceResolver by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var _mediaSession: MediaSession? = null
    private var playbackResource: PlaybackResource? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        tidetunesLog("Playback service creating...")
        val context = this

        val intent = Intent(this, Class.forName("com.github.tidetunes.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                mediaAudioAttributes(),
                !AppSettings.Default.allowMixedPlayback
            )
            .setHandleAudioBecomingNoisy(AppSettings.Default.pauseOnDisconnect)
            .setWakeMode(WAKE_MODE_NETWORK)
            .setMediaSourceFactory(ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context)))
            .build()
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

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playerRepository.setIsPlaying(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
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
        tidetunesLog("Playback service created")

        serviceScope.launch(Dispatchers.Main) {
            settingsRepository.settings.collect { settings ->
                player.setAudioAttributes(
                    mediaAudioAttributes(),
                    !settings.allowMixedPlayback,
                )
                player.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
            }
        }

        serviceScope.launch(Dispatchers.Main) {
            playerRepository.pauseRequest.collect {
                val player = _mediaSession?.player ?: return@collect

                if (player.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                    player.pause()
                } else {
                    tidetunesError("media player pause failed, command COMMAND_PLAY_PAUSE is unavailable")
                }
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
                    tidetunesError("Android playback resource resolve failed: ${result.reason}")
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

private fun mediaAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
}
