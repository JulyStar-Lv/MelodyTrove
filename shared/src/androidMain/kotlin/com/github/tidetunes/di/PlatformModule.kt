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
import com.github.tidetunes.core.data.settings.AndroidNetworkStatusProvider
import com.github.tidetunes.core.domain.repository.NetworkStatusProvider
import com.github.tidetunes.core.AndroidExternalEditorLauncher
import com.github.tidetunes.core.domain.repository.ExternalEditorLauncher
import com.github.tidetunes.platform.appContext

actual val platformModule: Module = module {
    single {
        PlayerControllerRepository(
            playerRepository = get(),
            toastRepository = get(),
            playlistRepository = get(),
            storageRepository = get(),
            bridge = get(),
            roomLibraryStore = get(),
            playbackResourceResolver = get(),
            _scope = get(),
            settingsRepository = get(),
            networkStatusProvider = get(),
        )
    } bind PlayerController::class
    single { PermissionRepository(get()) } bind PermissionChecker::class
    single<DownloadTaskScheduler> { AndroidWorkManagerDownloadScheduler() }
    single<NetworkStatusProvider> { AndroidNetworkStatusProvider() }
    single<ExternalEditorLauncher> { AndroidExternalEditorLauncher(appContext) }
}
