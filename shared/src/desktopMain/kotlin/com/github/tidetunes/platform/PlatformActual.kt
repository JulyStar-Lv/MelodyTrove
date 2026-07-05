package com.github.tidetunes.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun getAppDocumentDir(): String {
    return "${System.getProperty("user.home")}/.tidetunes"
}

actual fun getAppCacheDir(): String {
    return "${System.getProperty("user.home")}/.tidetunes/cache"
}

actual fun getAppDatabasePath(): String? {
    return "${getAppDocumentDir()}/tidetunes.db"
}

actual fun isSystemDynamicColorAvailable(): Boolean {
    return false
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
