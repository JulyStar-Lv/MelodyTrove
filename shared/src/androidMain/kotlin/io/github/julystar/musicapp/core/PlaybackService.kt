package io.github.julystar.musicapp.core

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
import io.github.julystar.musicapp.shared.R
import io.github.julystar.musicapp.core.domain.model.AppSettings
import io.github.julystar.musicapp.core.domain.model.AudioFocusMode
import io.github.julystar.musicapp.core.domain.model.ReplayGainMode
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import io.github.julystar.musicapp.core.audio.RustDspAudioProcessor
import io.github.julystar.musicapp.core.audio.TideTunesRenderersFactory
import com.google.common.util.concurrent.Futures
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import io.github.julystar.musicapp.service.playback.data.PlayerRepository
import io.github.julystar.musicapp.service.playback.data.toPlaybackArtwork
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import io.github.julystar.musicapp.service.playback.domain.RepeatMode
import io.github.julystar.musicapp.singleton.RoomLibraryStore
import io.github.julystar.musicapp.service.playback.data.PlaybackResourceResolver
import io.github.julystar.musicapp.source.api.PlaybackResource
import io.github.julystar.musicapp.source.api.SourcePlaybackResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uniffi.app_backend.Playlist
import io.github.julystar.musicapp.singleton.Bridge
import org.koin.android.ext.android.inject
import uniffi.app_backend.MusicAbstract
import io.github.julystar.musicapp.core.domain.model.DiagnosticLogCategory
import io.github.julystar.musicapp.diagnostics.AppLogger
import kotlin.math.log10
import kotlin.math.min


const val PLAYER_TO_PREV_COMMAND = "PLAYER_TO_PREV_COMMAND";
const val PLAYER_TO_NEXT_COMMAND = "PLAYER_TO_NEXT_COMMAND";
const val PLAYER_TOGGLE_FAVORITE_COMMAND = "PLAYER_TOGGLE_FAVORITE_COMMAND";
const val PLAYER_CYCLE_PLAYBACK_MODE_COMMAND = "PLAYER_CYCLE_PLAYBACK_MODE_COMMAND";



class PlaybackService : MediaSessionService() {
    private val playerRepository: PlayerRepository by inject()
    private val playbackController: PlaybackController by inject()
    private val artworkRepository: ArtworkRepository by inject()
    private val favoritesRepository: FavoritesRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val bridge: Bridge by inject()
    private val roomLibraryStore: RoomLibraryStore by inject()
    private val playbackResourceResolver: PlaybackResourceResolver by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var _mediaSession: MediaSession? = null
    private var playbackResource: PlaybackResource? = null
    private var audioFocusController: PlaybackAudioFocusController? = null
    private var lyricOutputController: AndroidLyricOutputController? = null
    private var dspAudioProcessor: RustDspAudioProcessor? = null
    private var currentSettings = AppSettings.Default
    private var favoriteTrackIds: Set<Long> = emptySet()
    private val previousCommand = SessionCommand(PLAYER_TO_PREV_COMMAND, Bundle.EMPTY)
    private val nextCommand = SessionCommand(PLAYER_TO_NEXT_COMMAND, Bundle.EMPTY)
    private val toggleFavoriteCommand = SessionCommand(PLAYER_TOGGLE_FAVORITE_COMMAND, Bundle.EMPTY)
    private val cyclePlaybackModeCommand =
        SessionCommand(PLAYER_CYCLE_PLAYBACK_MODE_COMMAND, Bundle.EMPTY)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        AppLogger.info(
            DiagnosticLogCategory.Playback,
            "PlaybackService",
            "Playback service creating",
        )
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.notification_small_icon)
            }
        )
        val context = this

        val intent = Intent(this, Class.forName("io.github.julystar.musicapp.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val dspProcessor = RustDspAudioProcessor().also {
            it.updateSettings(AppSettings.Default.audioEffects)
            dspAudioProcessor = it
        }
        val player = ExoPlayer.Builder(
            context,
            TideTunesRenderersFactory(context, dspProcessor),
        )
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
                        val sessionCommands =
                            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                                .add(previousCommand)
                                .add(nextCommand)
                                .add(toggleFavoriteCommand)
                                .add(cyclePlaybackModeCommand)
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
                        // Media button preferences and commands configure the notification session.
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setMediaButtonPreferences(buildMediaButtonPreferences(session.player))
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
                    return when (customCommand.customAction) {
                        PLAYER_TO_PREV_COMMAND -> {
                            playPrevious()
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        PLAYER_TO_NEXT_COMMAND -> {
                            playNext()
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        PLAYER_TOGGLE_FAVORITE_COMMAND -> {
                            toggleCurrentFavorite()
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        PLAYER_CYCLE_PLAYBACK_MODE_COMMAND -> {
                            cyclePlaybackMode()
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        else -> super.onCustomCommand(session, controller, customCommand, args)
                    }
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
                dspAudioProcessor?.resetDspState()
            }
        })
        AppLogger.info(
            DiagnosticLogCategory.Playback,
            "PlaybackService",
            "Playback service created",
        )

        serviceScope.launch(Dispatchers.Main) {
            settingsRepository.settings.collect { settings ->
                currentSettings = settings
                audioFocusController?.updateMode(settings.audioFocusMode)
                player.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
                updateAudioDsp(settings)
            }
        }

        serviceScope.launch(Dispatchers.Main) {
            favoritesRepository.favoriteTrackIds.collect { trackIds ->
                favoriteTrackIds = trackIds
                updateMediaButtonPreferences()
            }
        }

        serviceScope.launch(Dispatchers.Main) {
            playbackController.state.collect {
                updateMediaButtonPreferences()
            }
        }

        serviceScope.launch(Dispatchers.Main) {
            playerRepository.pauseRequest.collect {
                val player = _mediaSession?.player ?: return@collect

                if (player.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                    player.pause()
                } else {
                    AppLogger.warn(
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
        dspAudioProcessor?.close()
        dspAudioProcessor = null
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
                    AppLogger.error(
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
            updateAudioDsp(currentSettings, music.meta.id.value)
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

    private suspend fun updateAudioDsp(
        settings: AppSettings,
        trackId: Long? = playerRepository.music.value?.meta?.id?.value,
    ) {
        val replayGainDb = if (
            trackId == null ||
            settings.playbackAdvanced.replayGainMode == ReplayGainMode.Off
        ) {
            0f
        } else {
            val replayGain = withContext(Dispatchers.IO) {
                roomLibraryStore.getTrackReplayGain(trackId)
            }
            val (metadataGain, peak) = when (settings.playbackAdvanced.replayGainMode) {
                ReplayGainMode.Off -> null to null
                ReplayGainMode.Track -> replayGain?.trackGainDb to replayGain?.trackPeak
                ReplayGainMode.Album -> replayGain?.albumGainDb to replayGain?.albumPeak
                ReplayGainMode.Auto -> {
                    (replayGain?.trackGainDb ?: replayGain?.albumGainDb) to
                        (replayGain?.trackPeak ?: replayGain?.albumPeak)
                }
            }
            var gain = (metadataGain ?: 0.0) +
                settings.playbackAdvanced.replayGainPreampTenthsDb / 10.0
            if (peak != null && peak > 0.0) {
                gain = min(gain, -20.0 * log10(peak))
            }
            gain.toFloat()
        }
        dspAudioProcessor?.updateSettings(settings.audioEffects, replayGainDb)
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

    @OptIn(UnstableApi::class)
    private fun buildMediaButtonPreferences(player: Player): ImmutableList<CommandButton> {
        val playbackState = playbackController.state.value
        val isFavorite = playbackState.currentItem?.libraryTrackId
            ?.let(favoriteTrackIds::contains) == true
        val playbackModeButton = when {
            playbackState.shuffleEnabled -> CommandButton.ICON_SHUFFLE_ON to
                R.string.notification_playback_mode_shuffle
            playbackState.repeatMode == RepeatMode.One -> CommandButton.ICON_REPEAT_ONE to
                R.string.notification_playback_mode_repeat_one
            playbackState.repeatMode == RepeatMode.All -> CommandButton.ICON_REPEAT_ALL to
                R.string.notification_playback_mode_repeat_all
            else -> CommandButton.ICON_REPEAT_OFF to
                R.string.notification_playback_mode_repeat_off
        }

        return ImmutableList.of(
            CommandButton.Builder(
                if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED
            )
                .setSessionCommand(toggleFavoriteCommand)
                .setDisplayName(
                    getString(
                        if (isFavorite) {
                            R.string.notification_remove_favorite
                        } else {
                            R.string.notification_add_favorite
                        }
                    )
                )
                .build(),
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setSessionCommand(previousCommand)
                .setDisplayName(getString(R.string.notification_previous))
                .build(),
            CommandButton.Builder(
                if (player.isPlaying) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY
            )
                .setPlayerCommand(COMMAND_PLAY_PAUSE)
                .setDisplayName(
                    getString(
                        if (player.isPlaying) {
                            R.string.notification_pause
                        } else {
                            R.string.notification_play
                        }
                    )
                )
                .build(),
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setSessionCommand(nextCommand)
                .setDisplayName(getString(R.string.notification_next))
                .build(),
            CommandButton.Builder(playbackModeButton.first)
                .setSessionCommand(cyclePlaybackModeCommand)
                .setDisplayName(getString(playbackModeButton.second))
                .build(),
        )
    }

    @OptIn(UnstableApi::class)
    private fun updateMediaButtonPreferences() {
        val session = _mediaSession ?: return
        session.setMediaButtonPreferences(buildMediaButtonPreferences(session.player))
    }

    private fun toggleCurrentFavorite() {
        val trackId = playbackController.state.value.currentItem?.libraryTrackId ?: return
        serviceScope.launch {
            favoritesRepository.toggleFavorite(trackId)
        }
    }

    private fun cyclePlaybackMode() {
        val playbackState = playbackController.state.value
        val (repeatMode, shuffleEnabled) = when {
            playbackState.shuffleEnabled -> RepeatMode.One to false
            playbackState.repeatMode == RepeatMode.One -> RepeatMode.All to false
            playbackState.repeatMode == RepeatMode.All -> RepeatMode.All to true
            else -> RepeatMode.All to false
        }
        playbackController.setShuffle(shuffleEnabled)
        playbackController.setRepeatMode(repeatMode)
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
