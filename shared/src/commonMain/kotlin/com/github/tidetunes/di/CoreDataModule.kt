package com.github.tidetunes.di

import com.github.tidetunes.core.data.datastore.AppPreferencesRepository
import com.github.tidetunes.core.data.datastore.createAppDataStore
import com.github.tidetunes.core.data.security.createCredentialStore
import com.github.tidetunes.core.data.settings.AutoScanCoordinator
import com.github.tidetunes.core.data.settings.DataStoreSettingsRepository
import com.github.tidetunes.core.data.settings.FileDiagnosticsService
import com.github.tidetunes.core.data.settings.FileStorageUsageRepository
import com.github.tidetunes.core.data.settings.RoomLibraryMaintenanceService
import com.github.tidetunes.core.data.settings.RoomAppDataClearService
import com.github.tidetunes.core.data.settings.RoomSettingsMigration
import com.github.tidetunes.core.data.settings.RoomSourceSettingsRepository
import com.github.tidetunes.core.data.settings.JsonSettingsBackupService
import com.github.tidetunes.core.domain.model.SettingsCapabilities
import com.github.tidetunes.core.domain.repository.DiagnosticsService
import com.github.tidetunes.core.domain.repository.AppDataClearService
import com.github.tidetunes.core.domain.repository.LibraryMaintenanceService
import com.github.tidetunes.core.domain.repository.SettingsMigration
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.domain.repository.SettingsBackupService
import com.github.tidetunes.core.domain.repository.SourceSettingsRepository
import com.github.tidetunes.core.domain.repository.StorageUsageRepository
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.buildDatabase
import com.github.tidetunes.platform.getAppCacheDir
import com.github.tidetunes.platform.getAppDocumentDir
import com.github.tidetunes.platform.getAppVersion
import com.github.tidetunes.platform.platformSettingsCapabilities
import com.github.tidetunes.plugin.install.PluginInstaller
import com.github.tidetunes.plugin.management.MetadataLookupUseCase
import com.github.tidetunes.plugin.management.PluginManager
import com.github.tidetunes.plugin.management.PluginMetaSourceRegistry
import com.github.tidetunes.plugin.management.PluginRepository
import com.github.tidetunes.plugin.runtime.PluginCandidateContextStore
import com.github.tidetunes.plugin.runtime.PluginResultParser
import com.github.tidetunes.plugin.runtime.PluginRuntimeFactory
import com.github.tidetunes.plugin.runtime.PluginRuntimeManager
import com.github.tidetunes.plugin.runtime.PluginRuntimeSettings
import com.github.tidetunes.plugin.runtime.PluginScriptBundleBuilder
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.feature.home.data.RoomHomeHistoryRepository
import com.github.tidetunes.feature.home.data.RoomHomeStatisticsRepository
import com.github.tidetunes.feature.home.domain.HomeHistoryRepository
import com.github.tidetunes.feature.home.domain.HomeStatisticsRepository

import com.github.tidetunes.source.api.MetaSourceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.koin.dsl.onClose
import org.koin.dsl.module

val coreDataModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { buildDatabase() }
    single { get<TideTunesDatabase>().sourceAccountDao() }
    single { get<TideTunesDatabase>().libraryRootDao() }
    single { get<TideTunesDatabase>().sourceItemDao() }
    single { get<TideTunesDatabase>().trackSourceRefDao() }
    single { get<TideTunesDatabase>().trackDao() }
    single { get<TideTunesDatabase>().trackFtsDao() }
    single { get<TideTunesDatabase>().playlistDao() }
    single { get<TideTunesDatabase>().metadataDao() }
    single { get<TideTunesDatabase>().syncDao() }
    single { get<TideTunesDatabase>().sourceErrorDao() }
    single { get<TideTunesDatabase>().downloadTaskDao() }
    single { get<TideTunesDatabase>().pluginDao() }
    single { createAppDataStore() }
    single { AppPreferencesRepository(get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<SettingsBackupService>(createdAtStart = true) {
        JsonSettingsBackupService(get(), getAppDocumentDir(), get(), get())
    }
    single<SettingsMigration> { RoomSettingsMigration(get(), get()) }
    single<SourceSettingsRepository> { RoomSourceSettingsRepository(get(), get()) }
    single<StorageUsageRepository> { FileStorageUsageRepository() }
    single<DiagnosticsService> { FileDiagnosticsService(get(), get(), get(), get(), get(), get()) }
    single<LibraryMaintenanceService> {
        RoomLibraryMaintenanceService(get(), get(), get(), get(), get())
    }
    single<AppDataClearService> {
        RoomAppDataClearService(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    single { AutoScanCoordinator(get(), get(), get(), get(), get()) }
    single<SettingsCapabilities> { platformSettingsCapabilities() }
    single { createCredentialStore() }
    single { Bridge(getAppDocumentDir(), getAppCacheDir(), get()) }
    single { RoomLibraryStore(get(), get(), get(), get(), get(), get(), get()) }
    single<HomeHistoryRepository> { RoomHomeHistoryRepository(get(), get(), get()) }
    single<HomeStatisticsRepository> { RoomHomeStatisticsRepository(get(), get(), get()) }


    single {
        PluginRuntimeSettings(
            appVersionName = getAppVersion(),
            cacheDirectory = getAppCacheDir(),
        )
    }
    single { PluginScriptBundleBuilder() }
    single { PluginRuntimeFactory(get()) }
    single { PluginRuntimeManager(get(), get()) }
    single { PluginCandidateContextStore() }
    single { PluginResultParser(get()) }
    single {
        PluginRepository(
            pluginDao = get(),
            pluginsDir = getAppDocumentDir().toPath() / "plugins",
        )
    }
    single {
        PluginInstaller(
            pluginDao = get(),
            pluginsDir = getAppDocumentDir().toPath() / "plugins",
        )
    }
    single { MetaSourceRegistry() }
    single {
        PluginMetaSourceRegistry(
            scope = get(),
            repository = get(),
            runtimeManager = get(),
            resultParser = get(),
            registry = get(),
        )
    } onClose { registry ->
        registry?.let { runBlocking { it.shutdown() } }
    }
    single {
        MetadataLookupUseCase(
            registry = get<PluginMetaSourceRegistry>().registry,
            pluginRepository = get(),
            manualOperationTimeoutMs = get<PluginRuntimeSettings>().manualOperationTimeoutMs,
        )
    }
    single {
        PluginManager(
            repository = get(),
            installer = get(),
            runtimeManager = get(),
            resultParser = get(),
            runtimeSettings = get(),
        )
    }
}
