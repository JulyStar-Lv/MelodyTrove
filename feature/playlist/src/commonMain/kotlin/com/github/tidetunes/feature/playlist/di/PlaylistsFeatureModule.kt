package com.github.tidetunes.feature.playlist.di

import com.github.tidetunes.feature.playlist.presentation.PlaylistsVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val playlistsFeatureDiModule = module {
    viewModel { PlaylistsVM(get()) }
}
