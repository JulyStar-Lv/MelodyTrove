package com.github.tidetunes.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import com.github.tidetunes.core.domain.model.AppLanguageMode
import java.util.Locale

actual fun getAppDocumentDir(): String {
    return "${System.getProperty("user.home")}/.tidetunes"
}

actual fun getAppCacheDir(): String {
    return "${System.getProperty("user.home")}/.tidetunes/cache"
}

actual fun getAppDatabasePath(): String? {
    return "${getAppDocumentDir()}/tidetunes.db"
}

actual fun getPlatformName(): String = "desktop"

actual fun getProcessName(): String = "TideTunes"

actual fun isSystemDynamicColorAvailable(): Boolean {
    return false
}

actual fun platformSettingsCapabilities() =
    com.github.tidetunes.core.domain.model.SettingsCapabilities(
        dynamicColorSupported = false,
        customMusicDirectorySupported = true,
        secureCredentialStoreSupported = desktopSecureCredentialStoreAvailable(),
        desktopMediaKeysSupported = true,
        floatingLyricsSupported = true,
        gaplessPlaybackSupported = true,
        crossfadeSupported = true,
        replayGainSupported = true,
        audioEffectsSupported = true,
        diagnosticsExportSupported = true,
        diagnosticsCenterSupported = true,
        safeModeSupported = true,
        platformExitInfoSupported = false,
        historicalAnrTraceSupported = false,
        incidentRecoverySupported = true,
        fileShareSupported = true,
        settingsBackupSupported = true,
        scheduledBackupSupported = true,
        desktopShortcutsSupported = true,
    )

private val systemLocaleAtStartup: Locale = Locale.getDefault()

actual fun applyAppLanguageMode(mode: AppLanguageMode) {
    Locale.setDefault(
        when (mode) {
            AppLanguageMode.System -> systemLocaleAtStartup
            AppLanguageMode.Chinese -> Locale.forLanguageTag("zh-Hans")
            AppLanguageMode.English -> Locale.ENGLISH
        }
    )
}

private fun desktopSecureCredentialStoreAvailable(): Boolean {
    val osName = System.getProperty("os.name").orEmpty()
    return when {
        osName.startsWith("Mac", ignoreCase = true) -> true
        osName.startsWith("Windows", ignoreCase = true) -> true
        osName.startsWith("Linux", ignoreCase = true) -> runCatching {
            ProcessBuilder("which", "secret-tool").start().waitFor() == 0
        }.getOrDefault(false)
        else -> false
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
