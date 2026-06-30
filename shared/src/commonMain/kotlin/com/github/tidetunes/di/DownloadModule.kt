package com.github.tidetunes.di

import com.github.tidetunes.service.download.data.RoomDownloadTaskRepository
import com.github.tidetunes.service.download.data.di.downloadDataModule
import com.github.tidetunes.service.download.domain.DownloadTaskRepository
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import org.koin.dsl.module

val downloadModule = module {
    includes(downloadDataModule)
    single<DownloadTaskRepository> { RoomDownloadTaskRepository(get()) }
    single { EnqueueDownloadUseCase(get()) }
}
