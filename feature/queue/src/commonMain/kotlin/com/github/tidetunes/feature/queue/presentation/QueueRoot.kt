package com.github.tidetunes.feature.queue.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.service.playback.presentation.PlayerVM
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingAction
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QueueRoot(
    onNavigateBack: () -> Unit,
    viewModel: QueueViewModel = koinViewModel(),
    playerViewModel: PlayerVM = koinViewModel(),
    settingsRepository: SettingsRepository = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val nowPlayingState by playerViewModel.nowPlayingState.collectAsState()
    val currentDuration by playerViewModel.currentDuration.collectAsState()
    val bufferDuration by playerViewModel.bufferDuration.collectAsState()
    val playerDuration by playerViewModel.playerDuration.collectAsState()
    val settings by settingsRepository.settings.collectAsState(AppSettings.Default)

    QueueScreen(
        state = state,
        nowPlayingState = nowPlayingState,
        currentDuration = currentDuration,
        bufferDuration = bufferDuration,
        playerDuration = playerDuration,
        playerInteractionSettings = settings.playerInteraction,
        onAction = { action ->
            when (action) {
                QueueAction.NavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        },
        onPlayerAction = { action ->
            when (action) {
                NowPlayingAction.NavigateBack -> onNavigateBack()
                else -> playerViewModel.onNowPlayingAction(action)
            }
        },
    )
}
