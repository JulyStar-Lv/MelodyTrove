package com.github.tidetunes.service.download.domain

interface DownloadTaskScheduler {
    suspend fun schedule(task: DownloadTask)
    suspend fun pause(id: DownloadTaskId)
    suspend fun cancel(id: DownloadTaskId)
}
