package com.github.tidetunes.feature.queue.di

import com.github.tidetunes.feature.queue.presentation.QueueViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val queueFeatureModule = module {
    viewModelOf(::QueueViewModel)
}
