package com.github.tidetune

import androidx.compose.ui.window.ComposeUIViewController
import com.github.tidetune.di.appModule
import com.github.tidetune.singleton.Bridge
import com.github.tidetune.singleton.PlayerRepository
import com.github.tidetune.singleton.PlaylistRepository
import com.github.tidetune.singleton.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private var applicationInitialized = false
private var applicationKoin: Koin? = null

private fun initializeApplication() {
    if (applicationInitialized) return

    val koin = startKoin {
        modules(appModule)
    }.koin
    applicationKoin = koin
    koin.get<Bridge>().initialize()
    koin.get<CoroutineScope>().launch {
        koin.get<PlayerRepository>().reload()
        koin.get<StorageRepository>().reload()
        koin.get<PlaylistRepository>().reload()
    }
    applicationInitialized = true
}

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    initializeApplication()
    return ComposeUIViewController {
        Root()
    }
}

fun handleOneDriveOAuthRedirect(code: String, state: String) {
    initializeApplication()
    applicationKoin
        ?.get<StorageRepository>()
        ?.receiveOneDriveOAuthRedirect(code, state)
}
