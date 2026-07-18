package com.github.tidetunes.di

import com.github.tidetunes.singleton.IosPermissionChecker
import com.github.tidetunes.singleton.IosPlayerController
import com.github.tidetunes.core.domain.repository.PermissionChecker
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.service.download.data.scheduler.IosUrlSessionDownloadScheduler
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import org.koin.core.module.Module
import org.koin.dsl.module
import com.github.tidetunes.core.data.settings.IosNetworkStatusProvider
import com.github.tidetunes.core.domain.repository.NetworkStatusProvider
import com.github.tidetunes.core.data.settings.UnsupportedExternalEditorLauncher
import com.github.tidetunes.core.domain.repository.ExternalEditorLauncher

actual val platformModule: Module = module {
    single<PlayerController> {
        IosPlayerController(
            playerRepository = get(),
            toastRepository = get(),
            playlistRepository = get(),
            storageRepository = get(),
            roomLibraryStore = get(),
            playbackResourceResolver = get(),
            scope = get(),
            settingsRepository = get(),
            networkStatusProvider = get(),
        )
    }
    single<PermissionChecker> { IosPermissionChecker() }
    single<DownloadTaskScheduler> {
        IosUrlSessionDownloadScheduler(
            repository = get(),
            sourceRegistry = get(),
            legacyStoragePlaybackResolver = get(),
            scope = get(),
        )
    }
    single<NetworkStatusProvider> { IosNetworkStatusProvider() }
    single<ExternalEditorLauncher> { UnsupportedExternalEditorLauncher() }
}
