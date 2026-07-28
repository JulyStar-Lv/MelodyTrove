package io.github.julystar.musicapp.feature.queue.di

import io.github.julystar.musicapp.feature.queue.presentation.QueueViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val queueFeatureModule = module {
    viewModelOf(::QueueViewModel)
}
