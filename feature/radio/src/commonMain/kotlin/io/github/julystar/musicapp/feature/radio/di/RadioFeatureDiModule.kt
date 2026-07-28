package io.github.julystar.musicapp.feature.radio.di

import io.github.julystar.musicapp.feature.radio.presentation.RadioViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val radioFeatureDiModule = module {
    viewModelOf(::RadioViewModel)
}
