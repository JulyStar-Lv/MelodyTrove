package io.github.julystar.musicapp.feature.artist.di

import io.github.julystar.musicapp.feature.artist.presentation.ArtistViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val artistFeatureDiModule = module {
    viewModelOf(::ArtistViewModel)
}
