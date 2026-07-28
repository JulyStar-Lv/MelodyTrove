package io.github.julystar.musicapp.service.download.data.scheduler

import io.github.julystar.musicapp.service.download.domain.DownloadTask
import io.github.julystar.musicapp.service.download.domain.DownloadTaskId
import io.github.julystar.musicapp.service.download.domain.DownloadTaskScheduler

object NoOpDownloadTaskScheduler : DownloadTaskScheduler {
    override suspend fun schedule(task: DownloadTask) = Unit

    override suspend fun pause(id: DownloadTaskId) = Unit

    override suspend fun cancel(id: DownloadTaskId) = Unit
}
