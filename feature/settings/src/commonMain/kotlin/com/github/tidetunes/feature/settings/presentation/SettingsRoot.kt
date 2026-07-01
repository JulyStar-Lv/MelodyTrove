package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoot(
    appVersion: String,
    onNavigateToLog: () -> Unit,
    onNavigateToDebugMore: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val playbackVM: PlaybackSettingsVM = koinViewModel()
    val playbackState by playbackVM.state.collectAsState()

    SettingsScreen(
        appVersion = appVersion,
        playbackState = playbackState,
        onAction = { action ->
            when (action) {
                SettingsAction.NavigateToLog -> onNavigateToLog()
                SettingsAction.NavigateToDebugMore -> onNavigateToDebugMore()
                is SettingsAction.OpenGitRepo -> uriHandler.openUri(action.url)
                is SettingsAction.SetGaplessEnabled ->
                    playbackVM.onAction(PlaybackSettingsAction.SetGaplessEnabled(action.enabled))
                is SettingsAction.SetCrossfadeDurationMs ->
                    playbackVM.onAction(PlaybackSettingsAction.SetCrossfadeDurationMs(action.durationMs))
                is SettingsAction.SetReplayGainMode ->
                    playbackVM.onAction(PlaybackSettingsAction.SetReplayGainMode(action.mode))
                is SettingsAction.SetReplayGainPreampDb ->
                    playbackVM.onAction(PlaybackSettingsAction.SetReplayGainPreampDb(action.preampDb))
            }
        },
    )
}
