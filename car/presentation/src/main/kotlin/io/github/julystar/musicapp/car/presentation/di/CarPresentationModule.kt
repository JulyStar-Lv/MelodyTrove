package io.github.julystar.musicapp.car.presentation.di

import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import io.github.julystar.musicapp.car.presentation.screen.CarHomeViewModel
import io.github.julystar.musicapp.car.presentation.screen.CarLocalLibraryImporter
import io.github.julystar.musicapp.car.presentation.screen.CarLocalLibraryViewModel
import io.github.julystar.musicapp.car.presentation.screen.DefaultCarLocalLibraryImporter
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val carPresentationModule = module {
    single { CarLayoutProfileResolver() }
    single<CarLocalLibraryImporter> { DefaultCarLocalLibraryImporter(get(), get()) }
    viewModelOf(::CarHomeViewModel)
    viewModelOf(::CarLocalLibraryViewModel)
}
