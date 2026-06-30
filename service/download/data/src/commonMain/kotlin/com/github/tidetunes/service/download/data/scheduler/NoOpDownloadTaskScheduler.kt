package com.github.tidetunes.service.download.data.scheduler

import com.github.tidetunes.service.download.domain.DownloadTask
import com.github.tidetunes.service.download.domain.DownloadTaskId
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler

object NoOpDownloadTaskScheduler : DownloadTaskScheduler {
    override suspend fun schedule(task: DownloadTask) = Unit

    override suspend fun pause(id: DownloadTaskId) = Unit

    override suspend fun cancel(id: DownloadTaskId) = Unit
}
