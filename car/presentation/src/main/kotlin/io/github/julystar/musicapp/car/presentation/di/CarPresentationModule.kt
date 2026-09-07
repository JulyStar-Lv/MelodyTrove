package io.github.julystar.musicapp.car.presentation.di

import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import org.koin.dsl.module

val carPresentationModule = module {
    single { CarLayoutProfileResolver() }
}
