package com.github.tidetunes.platform

import android.graphics.BitmapFactory
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.github.tidetunes.core.domain.model.AppLanguageMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun getAppDocumentDir(): String {
    return appContext.filesDir.absolutePath
}

actual fun getAppCacheDir(): String {
    return appContext.cacheDir.absolutePath
}

actual fun getAppDatabasePath(): String? {
    return appContext.getDatabasePath("tidetunes.db").absolutePath
}

actual fun isSystemDynamicColorAvailable(): Boolean {
    return false
}

actual fun platformSettingsCapabilities() =
    com.github.tidetunes.core.domain.model.SettingsCapabilities(
        dynamicColorSupported = isSystemDynamicColorAvailable(),
        customMusicDirectorySupported = true,
        secureCredentialStoreSupported = true,
        audioFocusSupported = true,
        deviceDisconnectSupported = true,
        networkStatusSupported = true,
        diagnosticsExportSupported = true,
    )

private val systemLocalesAtStartup: LocaleList by lazy {
    appContext.resources.configuration.locales
}

actual fun applyAppLanguageMode(mode: AppLanguageMode) {
    val locales = when (mode) {
        AppLanguageMode.System -> LocaleList.getEmptyLocaleList()
        AppLanguageMode.Chinese -> LocaleList.forLanguageTags("zh-Hans")
        AppLanguageMode.English -> LocaleList.forLanguageTags("en")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        appContext.getSystemService(LocaleManager::class.java).applicationLocales = locales
        return
    }

    val effectiveLocales = if (mode == AppLanguageMode.System) systemLocalesAtStartup else locales
    val configuration = android.content.res.Configuration(appContext.resources.configuration)
    configuration.setLocales(effectiveLocales)
    if (!effectiveLocales.isEmpty) java.util.Locale.setDefault(effectiveLocales[0])
    @Suppress("DEPRECATION")
    appContext.resources.updateConfiguration(configuration, appContext.resources.displayMetrics)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bm.asImageBitmap()
}

lateinit var appContext: android.content.Context
