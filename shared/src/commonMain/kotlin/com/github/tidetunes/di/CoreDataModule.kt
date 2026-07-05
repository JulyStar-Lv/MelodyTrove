package com.github.tidetunes.di

import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.buildDatabase
import com.github.tidetunes.core.data.datastore.AppPreferencesRepository
import com.github.tidetunes.core.data.datastore.createAppDataStore
import com.github.tidetunes.core.data.settings.DataStoreSettingsRepository
import com.github.tidetunes.core.data.settings.FileStorageUsageRepository
import com.github.tidetunes.core.data.settings.RoomSourceSettingsRepository
import com.github.tidetunes.core.domain.model.SettingsCapabilities
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.domain.repository.SourceSettingsRepository
import com.github.tidetunes.core.domain.repository.StorageUsageRepository
import com.github.tidetunes.platform.getAppCacheDir
import com.github.tidetunes.platform.isSystemDynamicColorAvailable
import com.github.tidetunes.platform.getAppDocumentDir
import com.github.tidetunes.core.data.security.createCredentialStore
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.singleton.RoomLibraryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    single { createAppDataStore() }
    single { AppPreferencesRepository(get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<SourceSettingsRepository> { RoomSourceSettingsRepository(get(), get()) }
    single<StorageUsageRepository> { FileStorageUsageRepository() }
    single { SettingsCapabilities(dynamicColorSupported = isSystemDynamicColorAvailable()) }
    single { createCredentialStore() }
    single { Bridge(getAppDocumentDir(), getAppCacheDir(), get()) }
    single { RoomLibraryStore(get(), get(), get(), get(), get(), get()) }
}
