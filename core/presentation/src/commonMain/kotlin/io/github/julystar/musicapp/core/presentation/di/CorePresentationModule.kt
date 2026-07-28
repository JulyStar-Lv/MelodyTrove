package io.github.julystar.musicapp.core.presentation.di

import io.github.julystar.musicapp.core.presentation.overlay.ToastVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val corePresentationModule = module {
    viewModel { ToastVM(get()) }
}
