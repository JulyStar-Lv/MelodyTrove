package com.github.tidetunes.feature.downloads.di

import com.github.tidetunes.feature.downloads.presentation.DownloadsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val downloadsFeatureModule = module {
    viewModel { DownloadsViewModel(get()) }
}
