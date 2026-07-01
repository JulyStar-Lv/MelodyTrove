package com.github.tidetunes.di

import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.buildDatabase
import com.github.tidetunes.core.data.datastore.AppPreferencesRepository
import com.github.tidetunes.core.data.datastore.createAppDataStore
import com.github.tidetunes.platform.getAppCacheDir
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
    single { get<TideTunesDatabase>().storageDao() }
    single { get<TideTunesDatabase>().selectedFolderDao() }
    single { get<TideTunesDatabase>().remoteFileDao() }
    single { get<TideTunesDatabase>().trackDao() }
    single { get<TideTunesDatabase>().trackFtsDao() }
    single { get<TideTunesDatabase>().playlistDao() }
    single { get<TideTunesDatabase>().metadataDao() }
    single { get<TideTunesDatabase>().syncDao() }
    single { get<TideTunesDatabase>().downloadTaskDao() }
    single { createAppDataStore() }
    single { AppPreferencesRepository(get()) }
    single { createCredentialStore() }
    single { Bridge(getAppDocumentDir(), getAppCacheDir(), get()) }
    single { RoomLibraryStore(get(), get(), get(), get(), get()) }
}
