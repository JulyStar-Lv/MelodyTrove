package io.github.julystar.musicapp.car.di

import io.github.julystar.musicapp.car.platform.library.DefaultCarLocalLibraryImporter
import io.github.julystar.musicapp.car.presentation.screen.CarLocalLibraryImporter
import org.koin.dsl.module

val carPlatformModule = module {
    single<CarLocalLibraryImporter> { DefaultCarLocalLibraryImporter(get(), get()) }
}
