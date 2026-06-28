package com.github.tidetunes.di

import com.github.tidetunes.core.domain.repository.PermissionChecker
import com.github.tidetunes.singleton.PermissionRepository
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.singleton.PlayerControllerRepository
import com.github.tidetunes.service.download.data.scheduler.AndroidWorkManagerDownloadScheduler
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { PlayerControllerRepository(get(), get(), get(), get(), get(), get(), get(), get()) } bind PlayerController::class
    single { PermissionRepository(get()) } bind PermissionChecker::class
    single<DownloadTaskScheduler> { AndroidWorkManagerDownloadScheduler() }
}
