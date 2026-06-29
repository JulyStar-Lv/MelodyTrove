package com.github.tidetunes.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.service.playback.domain.SleepController
import com.github.tidetunes.service.playback.domain.SleepModeLeftTime
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class DashboardViewModel(
    private val sleepController: SleepController,
    private val librarySyncController: LibrarySyncController,
    private val currentTimeMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {
    private val timeTicks = flow {
        while (true) {
            emit(currentTimeMs())
            delay(1_000)
        }
    }

    val state = combine(
        sleepController.sleepState,
        librarySyncController.recentTasks,
        timeTicks,
    ) { sleepState, tasks, now ->
        val remainingMs = if (sleepState.enabled) {
            (sleepState.expiredMs - now).coerceAtLeast(0L)
        } else {
            0L
        }
        val leftTime = SleepModeLeftTime(remainingMs)
        DashboardState(
            sleepEnabled = sleepState.enabled,
            sleepRemainingMs = remainingMs,
            sleepHour = leftTime.hour,
            sleepMinute = leftTime.minute,
            importJobs = tasks.map { it.toUi() }.toImmutableList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState(),
    )

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.OpenSleepTimer,
            DashboardAction.NavigateToDownloads,
            DashboardAction.NavigateToAddDevice -> Unit
            is DashboardAction.PauseImport -> pause(action.id)
            is DashboardAction.ResumeImport -> resume(action.id)
            is DashboardAction.RetryImport -> retry(action.id)
            is DashboardAction.CancelImport -> cancel(action.id)
        }
    }

    private fun pause(scanId: String) {
        viewModelScope.launch {
            librarySyncController.pause(scanId)
        }
    }

    private fun resume(scanId: String) {
        viewModelScope.launch {
            librarySyncController.resume(scanId)
        }
    }

    private fun retry(scanId: String) {
        viewModelScope.launch {
            librarySyncController.retry(scanId)
        }
    }

    private fun cancel(scanId: String) {
        viewModelScope.launch {
            librarySyncController.cancel(scanId)
        }
    }
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
