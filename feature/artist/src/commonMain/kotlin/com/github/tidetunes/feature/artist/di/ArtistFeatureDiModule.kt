package com.github.tidetunes.feature.artist.di

import com.github.tidetunes.feature.artist.presentation.ArtistViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val artistFeatureDiModule = module {
    viewModelOf(::ArtistViewModel)
}
