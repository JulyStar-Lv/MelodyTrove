package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.service.download.data.RoomDownloadTaskRepository
import io.github.julystar.musicapp.service.download.data.di.downloadDataModule
import io.github.julystar.musicapp.service.download.domain.DownloadTaskRepository
import io.github.julystar.musicapp.service.download.domain.EnqueueDownloadUseCase
import org.koin.dsl.module

val downloadModule = module {
    includes(downloadDataModule)
    single<DownloadTaskRepository> { RoomDownloadTaskRepository(get()) }
    single { EnqueueDownloadUseCase(get()) }
}
