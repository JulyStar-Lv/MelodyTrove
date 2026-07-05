package com.github.tidetunes.di

import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.singleton.DesktopPermissionChecker
import com.github.tidetunes.singleton.DesktopPlaybackEngine
import com.github.tidetunes.singleton.DesktopPlayerController
import com.github.tidetunes.singleton.FallbackDesktopPlaybackEngine
import com.github.tidetunes.singleton.MpvDesktopPlaybackEngine
import com.github.tidetunes.singleton.VlcjDesktopPlaybackEngine
import com.github.tidetunes.core.domain.repository.PermissionChecker
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.service.download.data.scheduler.DesktopCoroutineDownloadScheduler
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import com.github.tidetunes.service.playback.data.DesktopAdvancedPlaybackController
import com.github.tidetunes.service.playback.domain.AdvancedPlaybackController
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DesktopPlaybackEngine> {
        FallbackDesktopPlaybackEngine(
            primary = VlcjDesktopPlaybackEngine(),
            fallback = MpvDesktopPlaybackEngine(),
        )
    }
    single<DownloadTaskScheduler> {
        DesktopCoroutineDownloadScheduler(
            repository = get(),
            sourceRegistry = get(),
            legacyStoragePlaybackResolver = get(),
            scope = get(),
        )
    }
    single<PlayerController> {
        DesktopPlayerController(
            playerRepository = get(),
            toastRepository = get(),
            playlistRepository = get(),
            storageRepository = get(),
            roomLibraryStore = get(),
            playbackResourceResolver = get(),
            playbackEngine = get(),
            scope = get(),
        )
    }
    single<PermissionChecker> { DesktopPermissionChecker() }
    single<AdvancedPlaybackController> { DesktopAdvancedPlaybackController() }
}
