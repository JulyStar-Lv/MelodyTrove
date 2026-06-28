package com.github.tidetunes.feature.radio.di

import com.github.tidetunes.feature.radio.presentation.RadioViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val radioFeatureDiModule = module {
    viewModelOf(::RadioViewModel)
}
