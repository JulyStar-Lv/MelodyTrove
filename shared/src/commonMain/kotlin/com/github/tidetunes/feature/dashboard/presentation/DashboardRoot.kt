package com.github.tidetunes.feature.dashboard.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.RouteAddDevices
import com.github.tidetunes.navigation.MusicGraph
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.feature.importing.presentation.ImportStatusVM
import com.github.tidetunes.service.playback.domain.SleepModeLeftTime
import com.github.tidetunes.feature.dashboard.presentation.SleepModeVM
import com.github.tidetunes.feature.sources.presentation.SourcesRoot
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardRoot(
    sleepModeVM: SleepModeVM = koinViewModel(),
    importStatusVM: ImportStatusVM = koinViewModel(),
) {
    val navController = LocalNavController.current
    val sleepState by sleepModeVM.state.collectAsState()
    val jobs by importStatusVM.recentJobs.collectAsState()

    var leftTime by remember {
        mutableStateOf(SleepModeLeftTime(sleepState.expiredMs - currentTimeMillis()))
    }

    LaunchedEffect(sleepState.expiredMs, sleepState.enabled) {
        while (true) {
            leftTime = SleepModeLeftTime(sleepState.expiredMs - currentTimeMillis())
            if (!sleepState.enabled) break
            kotlinx.coroutines.delay(1_000)
        }
    }

    val state = DashboardState(
        sleepEnabled = sleepState.enabled,
        sleepHour = leftTime.hour,
        sleepMinute = leftTime.minute,
        importJobs = jobs.map { it.toUi() }.toImmutableList(),
    )

    DashboardScreen(
        state = state,
        onAction = { action ->
            when (action) {
                DashboardAction.OpenSleepTimer -> sleepModeVM.openModal(leftTime)
                DashboardAction.NavigateToDownloads -> navController.navigate(MusicGraph.Downloads)
                DashboardAction.NavigateToAddDevice -> navController.navigate(RouteAddDevices((-1).toString()))
                is DashboardAction.PauseImport -> importStatusVM.pause(action.id)
                is DashboardAction.ResumeImport -> importStatusVM.resume(action.id)
                is DashboardAction.RetryImport -> importStatusVM.retry(action.id)
                is DashboardAction.CancelImport -> importStatusVM.cancel(action.id)
            }
        },
        sourcesContent = {
            SourcesRoot(
                onNavigateToSourceEditor = { id ->
                    navController.navigate(RouteAddDevices(id.toString()))
                },
            )
        },
    )
}

private fun LibrarySyncTask.toUi(): ImportJobUi {
    return ImportJobUi(
        id = id,
        status = status,
        scannedCount = scannedCount,
        importedCount = importedCount,
        skippedCount = skippedCount,
        failedCount = failedCount,
        checkpoint = checkpoint,
        errorMessage = errorMessage,
        hasError = hasError,
        isActive = isActive,
        canResume = canResume,
        canRetry = canRetry,
        statusLabel = status.toLabel(),
    )
}

private fun LibrarySyncStatus.toLabel(): String {
    return when (this) {
        LibrarySyncStatus.Queued -> "QUEUED"
        LibrarySyncStatus.Running -> "RUNNING"
        LibrarySyncStatus.Paused -> "PAUSED"
        LibrarySyncStatus.Completed -> "COMPLETED"
        LibrarySyncStatus.CompletedWithErrors -> "COMPLETED_WITH_ERRORS"
        LibrarySyncStatus.Failed -> "FAILED"
        LibrarySyncStatus.Cancelled -> "CANCELLED"
        LibrarySyncStatus.Unknown -> "UNKNOWN"
    }
}
