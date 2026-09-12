package io.github.julystar.musicapp.car.presentation.di

import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import io.github.julystar.musicapp.car.presentation.nowplaying.CarNowPlayingViewModel
import io.github.julystar.musicapp.car.presentation.screen.CarHomeViewModel
import io.github.julystar.musicapp.car.presentation.screen.CarLibraryViewModel
import io.github.julystar.musicapp.car.presentation.screen.CarSearchViewModel
import io.github.julystar.musicapp.car.presentation.screen.CarSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val carPresentationModule = module {
    single { CarLayoutProfileResolver() }
    viewModelOf(::CarHomeViewModel)
    viewModelOf(::CarLibraryViewModel)
    viewModelOf(::CarSearchViewModel)
    viewModelOf(::CarSettingsViewModel)
    viewModelOf(::CarNowPlayingViewModel)
}
