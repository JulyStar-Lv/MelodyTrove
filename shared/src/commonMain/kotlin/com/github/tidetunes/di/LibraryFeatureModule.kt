package com.github.tidetunes.di

import com.github.tidetunes.core.data.media.LegacyArtworkRepository
import com.github.tidetunes.core.domain.repository.ArtworkRepository
import com.github.tidetunes.core.presentation.media.ArtworkImageLoader
import com.github.tidetunes.core.data.media.RepositoryArtworkImageLoader
import com.github.tidetunes.core.data.media.AssetRepository
import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.core.domain.repository.BrowseRepository
import com.github.tidetunes.core.data.BrowseRepositoryImpl
import com.github.tidetunes.core.data.LibraryRepositoryImpl
import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.feature.playlist.presentation.PlaylistMetaToEdit
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.core.data.UpdatePlaylistRequest
import com.github.tidetunes.core.data.toSourceNodeSelection
import com.github.tidetunes.source.api.ImportRepository
import com.github.tidetunes.feature.importing.data.ImportRepositoryImpl
import com.github.tidetunes.core.domain.repository.PlaylistRepository
import com.github.tidetunes.feature.album.presentation.AlbumViewModel
import com.github.tidetunes.feature.artist.presentation.ArtistViewModel
import com.github.tidetunes.feature.lyrics.presentation.LyricsViewModel
import com.github.tidetunes.feature.onboarding.di.onboardingFeatureModule
import com.github.tidetunes.feature.queue.di.queueFeatureModule
import com.github.tidetunes.core.data.TrackBrowserRepositoryImpl
import com.github.tidetunes.core.domain.repository.TrackBrowserRepository
import com.github.tidetunes.feature.browse.di.browseFeatureDiModule
import com.github.tidetunes.feature.radio.di.radioFeatureDiModule
import com.github.tidetunes.feature.recentlyadded.di.recentlyAddedFeatureDiModule
import com.github.tidetunes.feature.recentlyplayed.di.recentlyPlayedFeatureDiModule
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistVM
import com.github.tidetunes.feature.library.di.libraryFeatureDiModule
import com.github.tidetunes.viewmodels.PlaylistVM
import com.github.tidetunes.feature.playlist.di.playlistsFeatureDiModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryFeatureModule = module {
    includes(
        browseFeatureDiModule,
        onboardingFeatureModule,
        queueFeatureModule,
    )

    single { AssetRepository(get(), get(), get()) }
    single<ArtworkRepository> { LegacyArtworkRepository(get(), get(), get(), get(), get()) }
    single<ArtworkImageLoader> { RepositoryArtworkImageLoader(get()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get(), get(), get()) }
    single<BrowseRepository> { BrowseRepositoryImpl(get(), get()) }
    single<TrackBrowserRepository> { TrackBrowserRepositoryImpl(get(), get(), get()) }
    single<PlaylistRepository> { PlaylistRepositoryImpl(get(), get(), get(), get()) }
    single<ImportRepository> { ImportRepositoryImpl() }

    includes(playlistsFeatureDiModule)
    viewModel { PlaylistVM(get(), get(), get(), get(), get(), get(), get(), get()) }
    
    viewModel { parameters ->
        val playlistRepo = get<PlaylistRepositoryImpl>()
        val storageRepo = get<StorageRepositoryImpl>()
        EditPlaylistVM(
            importRepository = get<ImportRepository>(),
            onGetPlaylistMetaToEdit = { id ->
                playlistRepo.playlists.value
                    .find { it.meta.id.value == id }
                    ?.let { item ->
                        PlaylistMetaToEdit(
                            title = item.meta.title,
                            coverSelection = item.meta.cover?.toSourceNodeSelection(
                                storageRepo.storages.value
                            ),
                        )
                    }
            },
            onUpdatePlaylistRequest = { id, title, cover ->
                playlistRepo.editPlaylist(
                    UpdatePlaylistRequest(
                        id = id,
                        title = title,
                        cover = cover,
                    )
                )
            },
            savedStateHandle = parameters.get(),
        )
    }
    includes(libraryFeatureDiModule)
    includes(radioFeatureDiModule)
    includes(recentlyAddedFeatureDiModule)
    includes(recentlyPlayedFeatureDiModule)
    viewModelOf(::AlbumViewModel)
    viewModelOf(::ArtistViewModel)
    viewModelOf(::LyricsViewModel)
}
