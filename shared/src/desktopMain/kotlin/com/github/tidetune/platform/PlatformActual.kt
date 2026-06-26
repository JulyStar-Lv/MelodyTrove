package com.github.tidetune.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun getAppDocumentDir(): String {
    return "${System.getProperty("user.home")}/.tidetune"
}

actual fun getAppCacheDir(): String {
    return "${System.getProperty("user.home")}/.tidetune/cache"
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
