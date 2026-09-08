package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.core.data.LegacyEditPlaylistGateway
import io.github.julystar.musicapp.core.data.media.RepositoryArtworkImageLoader
import io.github.julystar.musicapp.core.presentation.media.ArtworkImageLoader
import io.github.julystar.musicapp.feature.album.di.albumFeatureDiModule
import io.github.julystar.musicapp.feature.artist.di.artistFeatureDiModule
import io.github.julystar.musicapp.feature.browse.di.browseFeatureDiModule
import io.github.julystar.musicapp.feature.library.di.libraryFeatureDiModule
import io.github.julystar.musicapp.feature.lyrics.di.lyricsFeatureDiModule
import io.github.julystar.musicapp.feature.playlist.di.playlistsFeatureDiModule
import io.github.julystar.musicapp.feature.playlist.domain.EditPlaylistGateway
import io.github.julystar.musicapp.feature.queue.di.queueFeatureModule
import io.github.julystar.musicapp.feature.radio.di.radioFeatureDiModule
import io.github.julystar.musicapp.feature.recentlyadded.di.recentlyAddedFeatureDiModule
import io.github.julystar.musicapp.feature.recentlyplayed.di.recentlyPlayedFeatureDiModule
import org.koin.dsl.module

val libraryPresentationModule = module {
    single<ArtworkImageLoader> { RepositoryArtworkImageLoader(get()) }
    single<EditPlaylistGateway> { LegacyEditPlaylistGateway(get(), get()) }

    includes(
        browseFeatureDiModule,
        queueFeatureModule,
        playlistsFeatureDiModule,
        libraryFeatureDiModule,
        radioFeatureDiModule,
        recentlyAddedFeatureDiModule,
        recentlyPlayedFeatureDiModule,
        lyricsFeatureDiModule,
        albumFeatureDiModule,
        artistFeatureDiModule,
    )
}

val libraryFeatureModule = module {
    includes(libraryPresentationModule)
}
