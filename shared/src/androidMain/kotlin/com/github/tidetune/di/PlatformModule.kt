package com.github.tidetune.di

import com.github.tidetune.singleton.PermissionChecker
import com.github.tidetune.singleton.PermissionRepository
import com.github.tidetune.singleton.PlayerController
import com.github.tidetune.singleton.PlayerControllerRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { PlayerControllerRepository(get(), get(), get(), get(), get(), get(), get()) } bind PlayerController::class
    single { PermissionRepository(get()) } bind PermissionChecker::class
}
