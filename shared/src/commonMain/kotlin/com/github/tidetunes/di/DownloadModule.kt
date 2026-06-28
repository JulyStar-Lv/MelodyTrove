package com.github.tidetunes.di

import com.github.tidetunes.service.download.data.PersistentDownloadController
import com.github.tidetunes.service.download.data.RoomDownloadTaskRepository
import com.github.tidetunes.service.download.domain.DownloadController
import com.github.tidetunes.service.download.domain.DownloadTaskRepository
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import org.koin.dsl.module

val downloadModule = module {
    single<DownloadTaskRepository> { RoomDownloadTaskRepository(get()) }
    single<DownloadController> { PersistentDownloadController(get(), get()) }
    single { EnqueueDownloadUseCase(get()) }
}
