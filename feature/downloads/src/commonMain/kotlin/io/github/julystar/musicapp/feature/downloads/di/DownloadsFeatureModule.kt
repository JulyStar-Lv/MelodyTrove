package io.github.julystar.musicapp.feature.downloads.di

import io.github.julystar.musicapp.feature.downloads.presentation.DownloadsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val downloadsFeatureModule = module {
    viewModel { DownloadsViewModel(get()) }
}
