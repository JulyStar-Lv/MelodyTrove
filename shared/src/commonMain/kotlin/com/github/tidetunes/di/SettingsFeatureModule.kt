package com.github.tidetunes.di

import com.github.tidetunes.core.data.ToastRepositoryImpl
import com.github.tidetunes.core.domain.repository.ToastRepository
import com.github.tidetunes.feature.settings.di.settingsFeatureDiModule
import org.koin.dsl.module

val settingsFeatureModule = module {
    includes(settingsFeatureDiModule)

    single<ToastRepository> { ToastRepositoryImpl(get()) }
}
