package com.github.tidetunes.di

import com.github.tidetunes.core.data.ToastRepositoryImpl
import com.github.tidetunes.core.domain.repository.ToastRepository
import com.github.tidetunes.core.presentation.di.corePresentationModule
import com.github.tidetunes.feature.settings.di.settingsFeatureDiModule
import org.koin.dsl.module

val settingsFeatureModule = module {
    includes(corePresentationModule)
    includes(settingsFeatureDiModule)

    single { ToastRepositoryImpl(get()) }
    single<ToastRepository> { get<ToastRepositoryImpl>() }
}
