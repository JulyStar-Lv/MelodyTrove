package com.github.tidetunes.feature.playlist.di

import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistVM
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistVM
import com.github.tidetunes.feature.playlist.presentation.PlaylistVM
import com.github.tidetunes.feature.playlist.presentation.PlaylistsVM
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playlistsFeatureDiModule = module {
    viewModel { PlaylistsVM(get()) }
    viewModelOf(::CreatePlaylistVM)
    viewModelOf(::PlaylistVM)
    viewModel { parameters ->
        EditPlaylistVM(
            importRepository = get(),
            editPlaylistGateway = get(),
            savedStateHandle = parameters.get(),
        )
    }
}
