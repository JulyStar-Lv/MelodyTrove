package com.github.tidetunes.di

import com.github.tidetunes.singleton.IosPermissionChecker
import com.github.tidetunes.singleton.IosPlayerController
import com.github.tidetunes.core.domain.repository.PermissionChecker
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.service.download.data.scheduler.IosUrlSessionDownloadScheduler
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlayerController> { IosPlayerController(get(), get(), get(), get(), get(), get(), get()) }
    single<PermissionChecker> { IosPermissionChecker() }
    single<DownloadTaskScheduler> {
        IosUrlSessionDownloadScheduler(
            repository = get(),
            sourceRegistry = get(),
            legacyStoragePlaybackResolver = get(),
            scope = get(),
        )
    }
}
