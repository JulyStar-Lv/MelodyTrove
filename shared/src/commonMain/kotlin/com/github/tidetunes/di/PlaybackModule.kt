package com.github.tidetunes.di

import com.github.tidetunes.service.playback.data.LegacyPlaybackController
import com.github.tidetunes.service.playback.data.PlaybackResourceResolver
import com.github.tidetunes.service.playback.domain.PlaybackController
import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.viewmodels.PlayerVM
import com.github.tidetunes.feature.dashboard.presentation.SleepModeVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val playbackModule = module {
    single { PlaybackResourceResolver(get(), get(), get()) }
    single { PlayerRepository(get(), get(), get(), get()) }
    single<PlaybackController> { LegacyPlaybackController(get(), get(), get()) }
    viewModel { PlayerVM(get(), get(), get()) }
    viewModel { SleepModeVM(get()) { com.github.tidetunes.platform.currentTimeMillis() } }
}
