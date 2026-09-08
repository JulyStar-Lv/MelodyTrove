package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.runtime.runtimeModules
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    additionalModules: List<Module> = emptyList(),
    config: KoinAppDeclaration? = null,
): KoinApplication {
    return startKoin {
        config?.invoke(this)
        modules(runtimeModules + additionalModules)
    }
}

