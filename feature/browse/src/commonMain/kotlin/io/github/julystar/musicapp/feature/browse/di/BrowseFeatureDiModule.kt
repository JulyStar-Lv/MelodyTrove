package io.github.julystar.musicapp.feature.browse.di

import io.github.julystar.musicapp.feature.browse.presentation.BrowseViewModel
import io.github.julystar.musicapp.feature.browse.presentation.GenreTracksViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val browseFeatureDiModule = module {
    viewModelOf(::BrowseViewModel)
    viewModelOf(::GenreTracksViewModel)
}
