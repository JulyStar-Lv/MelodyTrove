package com.github.tidetunes.di

import com.github.tidetunes.service.librarysync.data.LegacyLibrarySyncController
import com.github.tidetunes.service.librarysync.data.LegacyLibrarySyncImporter
import com.github.tidetunes.service.librarysync.data.LegacyLibrarySyncStorageProvider
import com.github.tidetunes.service.librarysync.data.RemoteLibraryImportGateway
import com.github.tidetunes.service.librarysync.data.RoomLibrarySyncTaskRepository
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTaskRepository
import com.github.tidetunes.core.data.StorageRepositoryImpl
import org.koin.dsl.module

val librarySyncModule = module {
    single<LibrarySyncTaskRepository> { RoomLibrarySyncTaskRepository(get()) }
    single<LegacyLibrarySyncImporter> { RemoteLibraryImportGateway(get()) }
    single<LegacyLibrarySyncStorageProvider> {
        val storageRepository = get<StorageRepositoryImpl>()
        LegacyLibrarySyncStorageProvider { storageId ->
            storageRepository.storages.value.firstOrNull { storage -> storage.id == storageId }
        }
    }
    single<LibrarySyncController> { LegacyLibrarySyncController(get(), get(), get()) }
}
