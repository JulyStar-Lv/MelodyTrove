package com.github.tidetunes.core.presentation.di

import com.github.tidetunes.core.presentation.overlay.ToastVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val corePresentationModule = module {
    viewModel { ToastVM(get()) }
}
