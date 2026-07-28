package io.github.julystar.musicapp.feature.playlist.di

import io.github.julystar.musicapp.feature.playlist.presentation.CreatePlaylistVM
import io.github.julystar.musicapp.feature.playlist.presentation.EditPlaylistVM
import io.github.julystar.musicapp.feature.playlist.presentation.PlaylistVM
import io.github.julystar.musicapp.feature.playlist.presentation.PlaylistsVM
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
