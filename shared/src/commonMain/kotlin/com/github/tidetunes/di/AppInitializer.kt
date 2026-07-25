package com.github.tidetunes.di

import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.service.playback.data.PlayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.Koin
import com.github.tidetunes.core.data.settings.AutoScanCoordinator
import com.github.tidetunes.core.domain.repository.SettingsMigration
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.platform.applyAppLanguageMode
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import kotlinx.coroutines.flow.first

/**
 * Shared app initialization called by every platform entry point
 * after [initKoin]. Initialize bridge synchronously, then reload
 * repositories asynchronously in the given scope.
 */
object AppInitializer {

    /**
     * Synchronous startup: initialize the Rust bridge.
     * Must be called before any Compose content renders.
     */
    fun initializeBridge(koin: Koin) {
        koin.get<Bridge>().initialize()
    }

    /**
     * Asynchronously reload player, storage, and playlist repositories.
     */
    fun reloadRepositories(koin: Koin, scope: CoroutineScope) {
        scope.launch {
            koin.get<SettingsMigration>().migrate()
            applyAppLanguageMode(koin.get<SettingsRepository>().settings.first().languageMode)
            koin.get<LibrarySyncController>().recoverInterruptedTasks()
            koin.get<PlayerRepository>().reload()
            koin.get<StorageRepositoryImpl>().reload()
            koin.get<PlaylistRepositoryImpl>().reload()
            koin.get<AutoScanCoordinator>().runStartupScan()
        }
    }
}
