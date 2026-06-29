package com.github.tidetunes.feature.dashboard.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.service.playback.domain.SleepModeLeftTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardRoot(
    onOpenSleepTimer: (SleepModeLeftTime) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    sourcesContent: @Composable () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    DashboardScreen(
        state = state,
        onAction = { action ->
            when (action) {
                DashboardAction.OpenSleepTimer -> onOpenSleepTimer(
                    SleepModeLeftTime(state.sleepRemainingMs),
                )
                DashboardAction.NavigateToDownloads -> onNavigateToDownloads()
                DashboardAction.NavigateToAddDevice -> onNavigateToAddDevice()
                is DashboardAction.PauseImport,
                is DashboardAction.ResumeImport,
                is DashboardAction.RetryImport,
                is DashboardAction.CancelImport -> viewModel.onAction(action)
            }
        },
        sourcesContent = sourcesContent,
    )
}
