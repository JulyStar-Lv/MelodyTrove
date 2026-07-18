package com.github.tidetunes.di

import com.github.tidetunes.domain.importing.RemoteLibraryImportCoordinator
import com.github.tidetunes.source.storage.MetadataRepository
import com.github.tidetunes.source.storage.RemoteMetadataReader
import com.github.tidetunes.source.storage.RemoteScannerRepository
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.RemoteServerGateway
import com.github.tidetunes.source.api.RemoteServerKind
import com.github.tidetunes.source.api.LegacyStorageConnectionTester
import com.github.tidetunes.source.api.LegacyStorageDirectoryLister
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.LegacyStorageSearchProvider
import com.github.tidetunes.source.api.SourceListFailureReason
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.local.LocalMusicSource
import com.github.tidetunes.source.onedrive.OneDriveMusicSource
import com.github.tidetunes.source.storage.BridgeLegacyPlaybackSessionFactory
import com.github.tidetunes.source.storage.LegacyPlaybackSessionFactory
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.storage.LiveStorageSearchProvider
import com.github.tidetunes.source.storage.LiveStorageLookup
import com.github.tidetunes.source.storage.RetainedLegacyStoragePlaybackResolver
import com.github.tidetunes.source.storage.RoomLegacyStorageSearchProvider
import com.github.tidetunes.source.storage.StorageDirectoryLister
import com.github.tidetunes.source.storage.toArgUpsertStorage
import com.github.tidetunes.source.storage.toLegacyStorageIdOrNull
import com.github.tidetunes.source.storage.toSourceListResult
import com.github.tidetunes.source.storage.toSourceAuthResult
import com.github.tidetunes.source.storage.toStorageType
import com.github.tidetunes.source.webdav.WebDavMusicSource
import com.github.tidetunes.source.server.RemoteServerGatewayImpl
import com.github.tidetunes.source.server.ServerMusicSource
import org.koin.dsl.module
import org.koin.core.qualifier.named
import uniffi.tidetunes_backend.ListStorageEntryChildrenResp
import uniffi.tidetunes_backend.StorageId

val sourceDataModule = module {
    single { StorageRepositoryImpl(get(), get(), get(), get()) }
    single<StorageRepository> { get<StorageRepositoryImpl>() }
    single<LegacyStorageLookup> {
        val storageRepository = get<StorageRepositoryImpl>()
        LegacyStorageLookup { storageId ->
            storageRepository.storageForRust(storageId)
        }
    }
    single<LegacyPlaybackSessionFactory> {
        BridgeLegacyPlaybackSessionFactory(get())
    }
    single<LegacyStoragePlaybackResolver> {
        RetainedLegacyStoragePlaybackResolver(
            storageLookup = get(),
            sessionFactory = get(),
        )
    }
    single<LegacyStorageConnectionTester> {
        val storageRepository = get<StorageRepositoryImpl>()
        LegacyStorageConnectionTester { request ->
            storageRepository.test(request.toArgUpsertStorage()).toSourceAuthResult()
        }
    }
    single<LegacyStorageDirectoryLister> {
        val storageRepository = get<StorageRepositoryImpl>()
        val remoteScannerRepository = get<RemoteScannerRepository>()
        LegacyStorageDirectoryLister { accountId, directoryId, expectedStorageKind ->
            val expectedType = expectedStorageKind.toStorageType()
            val storageId = accountId.toLegacyStorageIdOrNull()
                ?: return@LegacyStorageDirectoryLister SourceListResult.Failure(
                    SourceListFailureReason.UnsupportedAccount
                )
            val storage = storageRepository.storageForRust(storageId)
                ?: return@LegacyStorageDirectoryLister SourceListResult.Failure(
                    SourceListFailureReason.UnsupportedAccount
                )
            if (storage.typ != expectedType) {
                return@LegacyStorageDirectoryLister SourceListResult.Failure(
                    SourceListFailureReason.UnsupportedAccount
                )
            }

            remoteScannerRepository
                .listDirectory(
                    storageId = storageId,
                    path = directoryId ?: "/",
                )
                .toSourceListResult(accountId)
        }
    }
    single<LegacyStorageSearchProvider> {
        val storageRepository = get<StorageRepositoryImpl>()
        RoomLegacyStorageSearchProvider(
            storageLookup = { storageId ->
                storageRepository.storageForRust(storageId)
            },
            trackDao = get(),
        )
    }
    single<LegacyStorageSearchProvider>(named("liveSearch")) {
        val storageRepository = get<StorageRepositoryImpl>()
        LiveStorageSearchProvider(
            directoryLister = get(),
            storageLookup = LiveStorageLookup { storageId ->
                storageRepository.storageForRust(storageId)
            },
        )
    }
    single<StorageDirectoryLister> {
        val remoteScannerRepository = get<RemoteScannerRepository>()
        object : StorageDirectoryLister {
            override suspend fun listDirectory(
                storageId: StorageId,
                path: String,
            ): ListStorageEntryChildrenResp {
                return remoteScannerRepository.listDirectory(storageId, path)
            }
        }
    }
    single { LocalMusicSource(get(), get(), get(named("liveSearch"))) }
    single { WebDavMusicSource(get(), get(), get(), get(named("liveSearch"))) }
    single { OneDriveMusicSource(get(), get(), get(), get(named("liveSearch"))) }
    single<RemoteServerGateway> { RemoteServerGatewayImpl(get(), get()) }
    single(named("navidromeSource")) {
        ServerMusicSource(RemoteServerKind.Navidrome, get())
    }
    single(named("openSubsonicSource")) {
        ServerMusicSource(RemoteServerKind.OpenSubsonic, get())
    }
    single(named("embySource")) {
        ServerMusicSource(RemoteServerKind.Emby, get())
    }
    single {
        MusicSourceRegistry(
            listOf(
                get<LocalMusicSource>(),
                get<WebDavMusicSource>(),
                get<OneDriveMusicSource>(),
                get<ServerMusicSource>(named("navidromeSource")),
                get<ServerMusicSource>(named("openSubsonicSource")),
                get<ServerMusicSource>(named("embySource")),
            )
        )
    }
    single { MetadataRepository(get(), get()) }
    single<RemoteMetadataReader> { get<MetadataRepository>() }
    single { RemoteScannerRepository(get(), get()) }
    single {
        RemoteLibraryImportCoordinator(
            get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
}
