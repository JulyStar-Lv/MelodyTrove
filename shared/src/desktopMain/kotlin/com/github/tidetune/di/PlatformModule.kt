package com.github.tidetune.di

import com.github.tidetune.singleton.DesktopPermissionChecker
import com.github.tidetune.singleton.DesktopPlayerController
import com.github.tidetune.singleton.PermissionChecker
import com.github.tidetune.singleton.PlayerController
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlayerController> { DesktopPlayerController() }
    single<PermissionChecker> { DesktopPermissionChecker() }
}
