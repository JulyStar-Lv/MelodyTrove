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
import com.github.tidetunes.core.data.LegacyEditPlaylistGateway
import com.github.tidetunes.core.data.PlaylistImportTargetImpl
import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.source.api.PlaylistImportTarget
import com.github.tidetunes.core.domain.repository.PlaylistRepository
import com.github.tidetunes.feature.playlist.domain.EditPlaylistGateway
import com.github.tidetunes.feature.onboarding.di.onboardingFeatureModule
import com.github.tidetunes.feature.queue.di.queueFeatureModule
import com.github.tidetunes.core.data.TrackBrowserRepositoryImpl
import com.github.tidetunes.core.domain.repository.TrackBrowserRepository
import com.github.tidetunes.feature.browse.di.browseFeatureDiModule
import com.github.tidetunes.feature.radio.di.radioFeatureDiModule
import com.github.tidetunes.feature.recentlyadded.di.recentlyAddedFeatureDiModule
import com.github.tidetunes.feature.recentlyplayed.di.recentlyPlayedFeatureDiModule
import com.github.tidetunes.feature.library.di.libraryFeatureDiModule
import com.github.tidetunes.core.data.AlbumDetailRepositoryImpl
import com.github.tidetunes.core.data.ArtistDetailRepositoryImpl
import com.github.tidetunes.core.data.LyricsRepositoryImpl
import com.github.tidetunes.core.domain.repository.AlbumDetailRepository
import com.github.tidetunes.core.domain.repository.ArtistDetailRepository
import com.github.tidetunes.core.domain.repository.LyricsRepository
import com.github.tidetunes.feature.album.di.albumFeatureDiModule
import com.github.tidetunes.feature.artist.di.artistFeatureDiModule
import com.github.tidetunes.feature.lyrics.di.lyricsFeatureDiModule
import com.github.tidetunes.feature.playlist.di.playlistsFeatureDiModule
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
    single<TrackBrowserRepository> { TrackBrowserRepositoryImpl(get(), get()) }
    single<LyricsRepository> { LyricsRepositoryImpl(get(), get()) }
    single<AlbumDetailRepository> { AlbumDetailRepositoryImpl(get(), get()) }
    single<ArtistDetailRepository> { ArtistDetailRepositoryImpl(get(), get()) }
    single { PlaylistRepositoryImpl(get(), get(), get(), get(), get()) }
    single<PlaylistRepository> { get<PlaylistRepositoryImpl>() }
    single<EditPlaylistGateway> { LegacyEditPlaylistGateway(get(), get()) }
    single<PlaylistImportTarget> { PlaylistImportTargetImpl(get()) }

    includes(playlistsFeatureDiModule)
    includes(libraryFeatureDiModule)
    includes(radioFeatureDiModule)
    includes(recentlyAddedFeatureDiModule)
    includes(recentlyPlayedFeatureDiModule)
    includes(lyricsFeatureDiModule)
    includes(albumFeatureDiModule)
    includes(artistFeatureDiModule)
}
