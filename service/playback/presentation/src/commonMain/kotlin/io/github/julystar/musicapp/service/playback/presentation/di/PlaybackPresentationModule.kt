package io.github.julystar.musicapp.service.playback.presentation.di

import io.github.julystar.musicapp.service.playback.presentation.PlayerVM
import io.github.julystar.musicapp.service.playback.presentation.sleep.SleepModeVM
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playbackPresentationModule = module {
    viewModelOf(::PlayerVM)
    viewModel { SleepModeVM(get()) }
}
