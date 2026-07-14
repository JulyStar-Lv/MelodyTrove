package com.github.tidetunes.service.playback.presentation.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.service.playback.presentation.PlayerVM
import com.github.tidetunes.service.playback.presentation.sleep.SleepModeVM
import com.github.tidetunes.core.presentation.media.ArtworkPalette
import com.github.tidetunes.core.presentation.media.rememberArtworkPalette
import com.github.tidetunes.core.presentation.platform.KeepScreenOnEffect
import com.github.tidetunes.core.presentation.theme.TideTunesTheme
import com.github.tidetunes.core.presentation.theme.TideTunesThemeMode
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NowPlayingRoot(
    onNavigateBack: () -> Unit,
    onNavigateToLyricImport: () -> Unit,
    onSearchMetadata: (NowPlayingTrackItem) -> Unit,
    playerViewModel: PlayerVM = koinViewModel(),
    sleepModeViewModel: SleepModeVM = koinViewModel(),
    settingsRepository: SettingsRepository = koinInject(),
) {
    val state by playerViewModel.nowPlayingState.collectAsState()
    val settings by settingsRepository.settings.collectAsState(AppSettings.Default)
    val palette = rememberArtworkPalette(artwork = state.currentTrack?.artwork)
    val sleepModeState by sleepModeViewModel.state.collectAsState()
    KeepScreenOnEffect(enabled = settings.keepScreenOnInPlayer)

    val currentDuration by playerViewModel.currentDuration.collectAsState()

    LaunchedEffect(playerViewModel) {
        playerViewModel.nowPlayingEvents.collect { event ->
            when (event) {
                is NowPlayingEvent.ShowMessage -> Unit
            }
        }
    }

    fun onAction(action: NowPlayingAction) {
        when (action) {
            NowPlayingAction.NavigateBack -> onNavigateBack()
            NowPlayingAction.AddLyric -> {
                if (state.currentTrack != null) {
                    onNavigateToLyricImport()
                }
            }
            NowPlayingAction.SearchMetadata -> state.currentTrack?.let(onSearchMetadata)
            NowPlayingAction.OpenSleepTimer -> sleepModeViewModel.openModal()
            else -> playerViewModel.onNowPlayingAction(action)
        }
    }

    TideTunesTheme(
        darkTheme = true,
        themeMode = TideTunesThemeMode.Dark,
    ) {
        NowPlayingScreen(
            state = state,
            palette = palette,
            currentPositionMs = currentDuration.inWholeMilliseconds,
            isSleepTimerEnabled = sleepModeState.enabled,
            progressContent = { trackDurationMs ->
                NowPlayingProgressRoot(
                    trackDurationMs = trackDurationMs,
                    playerViewModel = playerViewModel,
                    onAction = ::onAction,
                )
            },
            onAction = ::onAction,
        )
    }
}

@Composable
private fun NowPlayingProgressRoot(
    trackDurationMs: Long?,
    playerViewModel: PlayerVM,
    onAction: (NowPlayingAction) -> Unit,
) {
    val currentDuration by playerViewModel.currentDuration.collectAsState()
    val bufferDuration by playerViewModel.bufferDuration.collectAsState()
    val playerDuration by playerViewModel.playerDuration.collectAsState()

    NowPlayingProgressPanel(
        progressState = NowPlayingProgressState(
            currentDuration = currentDuration,
            bufferDuration = bufferDuration,
            playerDuration = playerDuration,
        ),
        trackDurationMs = trackDurationMs,
        onAction = onAction,
    )
}
