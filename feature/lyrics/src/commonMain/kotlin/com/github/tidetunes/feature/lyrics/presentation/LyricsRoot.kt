package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.repository.FavoritesRepository
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.service.playback.presentation.PlayerVM
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LyricsRoot(
    onNavigateBack: () -> Unit,
    viewModel: LyricsViewModel = koinViewModel(),
    playerViewModel: PlayerVM = koinViewModel(),
    settingsRepository: SettingsRepository = koinInject(),
    favoritesRepository: FavoritesRepository = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val nowPlayingState by playerViewModel.nowPlayingState.collectAsState()
    val currentDuration by playerViewModel.currentDuration.collectAsState()
    val settings by settingsRepository.settings.collectAsState(AppSettings.Default)
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    val coroutineScope = rememberCoroutineScope()
    val nowPlayingTrack = nowPlayingState.currentTrack
        ?.takeIf { track -> track.id == state.trackId }

    LyricsScreen(
        state = state,
        nowPlayingTrack = nowPlayingTrack,
        currentPositionMs = currentDuration.inWholeMilliseconds,
        isPlaying = nowPlayingState.controls.isPlaying,
        lyricDisplaySettings = settings.lyrics,
        isFavorite = state.trackId?.let(favoriteTrackIds::contains) == true,
        onToggleFavorite = {
            state.trackId?.let { trackId ->
                coroutineScope.launch { favoritesRepository.toggleFavorite(trackId) }
            }
        },
        onAction = { action ->
            when (action) {
                LyricsAction.NavigateBack -> onNavigateBack()
                LyricsAction.Retry -> viewModel.onAction(action)
            }
        },
        onPlayerAction = playerViewModel::onNowPlayingAction,
    )
}
