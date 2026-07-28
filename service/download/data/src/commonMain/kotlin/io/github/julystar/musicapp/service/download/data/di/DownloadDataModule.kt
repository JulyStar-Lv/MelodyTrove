package io.github.julystar.musicapp.service.download.data.di

import io.github.julystar.musicapp.service.download.data.PersistentDownloadController
import io.github.julystar.musicapp.service.download.data.scheduler.NoOpDownloadTaskScheduler
import io.github.julystar.musicapp.service.download.domain.DownloadController
import io.github.julystar.musicapp.service.download.domain.DownloadTaskScheduler
import org.koin.dsl.module

val downloadDataModule = module {
    single<DownloadTaskScheduler> { NoOpDownloadTaskScheduler }
    single<DownloadController> { PersistentDownloadController(get(), get()) }
}
