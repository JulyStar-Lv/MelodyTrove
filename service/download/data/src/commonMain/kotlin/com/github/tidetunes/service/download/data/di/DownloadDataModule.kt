package com.github.tidetunes.service.download.data.di

import com.github.tidetunes.service.download.data.PersistentDownloadController
import com.github.tidetunes.service.download.data.scheduler.NoOpDownloadTaskScheduler
import com.github.tidetunes.service.download.domain.DownloadController
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import org.koin.dsl.module

val downloadDataModule = module {
    single<DownloadTaskScheduler> { NoOpDownloadTaskScheduler }
    single<DownloadController> { PersistentDownloadController(get(), get()) }
}
