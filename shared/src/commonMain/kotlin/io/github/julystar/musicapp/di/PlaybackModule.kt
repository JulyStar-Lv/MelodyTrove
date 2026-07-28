package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.plugin.management.ManualMetadataService
import io.github.julystar.musicapp.service.playback.data.LegacyPlaybackController
import io.github.julystar.musicapp.service.playback.data.LegacyNowPlayingRepository
import io.github.julystar.musicapp.service.playback.data.LegacyPlaylistPlaybackSync
import io.github.julystar.musicapp.service.playback.data.PlaybackResourceResolver
import io.github.julystar.musicapp.service.playback.data.PlayerController
import io.github.julystar.musicapp.service.playback.data.PlayerRepository
import io.github.julystar.musicapp.service.playback.domain.NowPlayingRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import io.github.julystar.musicapp.service.playback.domain.PlaylistPlaybackSync
import io.github.julystar.musicapp.service.playback.domain.SleepController
import io.github.julystar.musicapp.service.playback.presentation.di.playbackPresentationModule
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
