package com.github.tidetune.di

import com.github.tidetune.singleton.IosPermissionChecker
import com.github.tidetune.singleton.IosPlayerController
import com.github.tidetune.singleton.PermissionChecker
import com.github.tidetune.singleton.PlayerController
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlayerController> { IosPlayerController(get(), get(), get(), get(), get(), get(), get()) }
    single<PermissionChecker> { IosPermissionChecker() }
}
