package com.github.tidetunes.di

import com.github.tidetunes.plugin.management.ManualMetadataService
import com.github.tidetunes.service.playback.data.LegacyPlaybackController
import com.github.tidetunes.service.playback.data.LegacyNowPlayingRepository
import com.github.tidetunes.service.playback.data.LegacyPlaylistPlaybackSync
import com.github.tidetunes.service.playback.data.PlaybackResourceResolver
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.service.playback.domain.NowPlayingRepository
import com.github.tidetunes.service.playback.domain.PlaybackController
import com.github.tidetunes.service.playback.domain.PlaylistPlaybackSync
import com.github.tidetunes.service.playback.domain.SleepController
import com.github.tidetunes.service.playback.presentation.di.playbackPresentationModule
import org.koin.dsl.module

val playbackModule = module {
    includes(playbackPresentationModule)

    single { PlaybackResourceResolver(get(), get(), get(), get()) }
    single { PlayerRepository(get(), get(), get(), get(), get(), get()) }
    single { ManualMetadataService(get(), get(), get(), get(), get(), get()) }
    single<PlaybackController> {
        LegacyPlaybackController(
            playerRepository = get(),
            legacyController = get(),
            scope = get(),
            settingsRepository = get(),
        )
    }
    single<SleepController> { get<PlayerController>() }
    single<NowPlayingRepository> { LegacyNowPlayingRepository(get(), get()) }
    single<PlaylistPlaybackSync> { LegacyPlaylistPlaybackSync(get(), get()) }
}
