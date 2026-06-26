package com.github.tidetune.di

import com.github.tidetune.singleton.AssetRepository
import com.github.tidetune.datastore.AppPreferencesRepository
import com.github.tidetune.datastore.createAppDataStore
import com.github.tidetune.platform.getAppCacheDir
import com.github.tidetune.platform.getAppDocumentDir
import com.github.tidetune.singleton.Bridge
import com.github.tidetune.singleton.ImportRepository
import com.github.tidetune.singleton.ImportStatusRepository
import com.github.tidetune.singleton.LibraryRepository
import com.github.tidetune.singleton.MetadataRepository
import com.github.tidetune.singleton.PlayerRepository
import com.github.tidetune.singleton.PlaylistRepository
import com.github.tidetune.singleton.RemoteScannerRepository
import com.github.tidetune.singleton.StorageRepository
import com.github.tidetune.singleton.ToastRepository
import com.github.tidetune.database.buildDatabase
import com.github.tidetune.domain.importing.RemoteLibraryImportCoordinator
import com.github.tidetune.security.createCredentialStore
import com.github.tidetune.singleton.RoomLibraryStore
import com.github.tidetune.viewmodels.AssetVM
import com.github.tidetune.viewmodels.CreatePlaylistVM
import com.github.tidetune.viewmodels.DebugMoreVM
import com.github.tidetune.viewmodels.EditPlaylistVM
import com.github.tidetune.viewmodels.EditStorageVM
import com.github.tidetune.viewmodels.ImportVM
import com.github.tidetune.viewmodels.ImportStatusVM
import com.github.tidetune.viewmodels.LibraryVM
import com.github.tidetune.viewmodels.LogVM
import com.github.tidetune.viewmodels.PlayerVM
import com.github.tidetune.viewmodels.PlaylistVM
import com.github.tidetune.viewmodels.PlaylistsVM
import com.github.tidetune.viewmodels.SleepModeVM
import com.github.tidetune.viewmodels.StoragesVM
import com.github.tidetune.viewmodels.ToastVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(platformModule)

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { buildDatabase() }
    single { get<com.github.tidetune.database.TideTuneDatabase>().storageDao() }
    single { get<com.github.tidetune.database.TideTuneDatabase>().selectedFolderDao() }
    single { get<com.github.tidetune.database.TideTuneDatabase>().remoteFileDao() }
    single { get<com.github.tidetune.database.TideTuneDatabase>().trackDao() }
    single { get<com.github.tidetune.database.TideTuneDatabase>().playlistDao() }
    single { get<com.github.tidetune.database.TideTuneDatabase>().metadataDao() }
    single { get<com.github.tidetune.database.TideTuneDatabase>().syncDao() }
    single { createAppDataStore() }
    single { AppPreferencesRepository(get()) }
    single { createCredentialStore() }
    single { Bridge(getAppDocumentDir(), getAppCacheDir(), get()) }
    single { RoomLibraryStore(get(), get(), get(), get(), get()) }
    single { PlayerRepository(get(), get(), get()) }
    single { StorageRepository(get(), get(), get(), get()) }
    single { ToastRepository(get()) }
    single { ImportRepository() }
    single { ImportStatusRepository(get(), get()) }
    single { MetadataRepository(get(), get()) }
    single { RemoteScannerRepository(get(), get()) }
    single {
        RemoteLibraryImportCoordinator(
            get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    single { AssetRepository(get(), get(), get()) }
    single { LibraryRepository(get(), get()) }
    single { PlaylistRepository(get(), get(), get(), get()) }

    viewModel { PlayerVM(get(), get()) }
    viewModel { PlaylistVM(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { PlaylistsVM(get()) }
    viewModel { AssetVM(get()) }
    viewModel { ImportVM(get(), get(), get(), get()) }
    viewModel { ImportStatusVM(get(), get()) }
    viewModel { LibraryVM(get()) }
    viewModel { CreatePlaylistVM(get(), get()) }
    viewModel { EditPlaylistVM(get(), get(), get()) }
    viewModel { EditStorageVM(get(), get(), get(), get(), get()) }
    viewModel { SleepModeVM(get()) }
    viewModel { StoragesVM(get()) }
    viewModel { ToastVM(get()) }
    viewModel { LogVM(get()) }
    viewModel { DebugMoreVM(get()) }
}
