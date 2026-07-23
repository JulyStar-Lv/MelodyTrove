package com.github.tidetunes

import androidx.compose.ui.window.ComposeUIViewController
import com.github.tidetunes.di.AppInitializer
import com.github.tidetunes.di.initKoin
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.service.download.data.scheduler.IosUrlSessionDownloadScheduler
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import platform.UIKit.UIViewController

private var applicationInitialized = false
private var applicationKoin: Koin? = null

private fun initializeApplication() {
    if (applicationInitialized) return

    val koin = initKoin().koin
    applicationKoin = koin
    AppInitializer.initializeBridge(koin)
    AppInitializer.reloadRepositories(koin, koin.get())
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
        ?.get<StorageRepositoryImpl>()
        ?.receiveOneDriveOAuthRedirect(code, state)
}

fun handleEventsForBackgroundURLSession(
    identifier: String,
    completionHandler: () -> Unit,
) {
    initializeApplication()
    val scheduler = applicationKoin
        ?.get<DownloadTaskScheduler>() as? IosUrlSessionDownloadScheduler
    scheduler?.setBackgroundCompletionHandler(identifier, completionHandler)
        ?: completionHandler()
}

fun shutdownApplication() {
    if (!applicationInitialized) return
    stopKoin()
    applicationKoin = null
    applicationInitialized = false
}
