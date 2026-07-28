package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.core.data.ToastRepositoryImpl
import io.github.julystar.musicapp.core.domain.repository.ToastRepository
import io.github.julystar.musicapp.core.presentation.di.corePresentationModule
import io.github.julystar.musicapp.feature.settings.di.settingsFeatureDiModule
import org.koin.dsl.module

val settingsFeatureModule = module {
    includes(corePresentationModule)
    includes(settingsFeatureDiModule)

    single { ToastRepositoryImpl(get()) }
    single<ToastRepository> { get<ToastRepositoryImpl>() }
}
