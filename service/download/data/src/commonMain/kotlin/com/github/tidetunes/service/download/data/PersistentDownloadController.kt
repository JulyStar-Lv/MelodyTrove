package com.github.tidetunes.service.download.data

import com.github.tidetunes.service.download.data.scheduler.NoOpDownloadTaskScheduler
import com.github.tidetunes.service.download.domain.DownloadController
import com.github.tidetunes.service.download.domain.DownloadStatus
import com.github.tidetunes.service.download.domain.DownloadTask
import com.github.tidetunes.service.download.domain.DownloadTaskId
import com.github.tidetunes.service.download.domain.DownloadTaskRepository
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import com.github.tidetunes.service.download.domain.canTransitionTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

class PersistentDownloadController(
    private val repository: DownloadTaskRepository,
    private val scheduler: DownloadTaskScheduler = NoOpDownloadTaskScheduler,
    private val nowEpochMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : DownloadController {
    override val tasks: Flow<List<DownloadTask>> = repository.observeTasks()

    override suspend fun enqueue(task: DownloadTask) {
        val queuedTask = task.copy(
            status = DownloadStatus.Queued,
            errorMessage = null,
            updatedAtEpochMs = nowEpochMs(),
        )
        repository.upsertTask(queuedTask)
        scheduler.schedule(queuedTask)
    }

    override suspend fun pause(id: DownloadTaskId) {
        updateStatus(id, DownloadStatus.Paused)?.let {
            scheduler.pause(id)
        }
    }

    override suspend fun resume(id: DownloadTaskId) {
        updateStatus(id, DownloadStatus.Queued, clearError = true)?.let { task ->
            scheduler.schedule(task)
        }
    }

    override suspend fun cancel(id: DownloadTaskId) {
        updateStatus(id, DownloadStatus.Cancelled)?.let {
            scheduler.cancel(id)
        }
    }

    override suspend fun cancelAll() {
        repository.observeActiveTasks().first().forEach { task ->
            cancel(task.id)
        }
    }

    override suspend fun retry(id: DownloadTaskId) {
        updateStatus(id, DownloadStatus.Queued, clearError = true)?.let { task ->
            scheduler.schedule(task)
        }
    }

    private suspend fun updateStatus(
        id: DownloadTaskId,
        status: DownloadStatus,
        clearError: Boolean = false,
    ): DownloadTask? {
        val current = repository.getTask(id) ?: return null
        if (!current.status.canTransitionTo(status)) return null
        val updated = current.copy(
            status = status,
            errorMessage = if (clearError) null else current.errorMessage,
            updatedAtEpochMs = nowEpochMs(),
        )
        repository.updateTask(updated)
        return updated
    }
}
