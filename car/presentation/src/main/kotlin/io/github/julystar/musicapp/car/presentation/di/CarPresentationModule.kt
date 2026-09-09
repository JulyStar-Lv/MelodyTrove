package io.github.julystar.musicapp.car.presentation.di

import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import io.github.julystar.musicapp.car.presentation.screen.CarHomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val carPresentationModule = module {
    single { CarLayoutProfileResolver() }
    viewModelOf(::CarHomeViewModel)
}
