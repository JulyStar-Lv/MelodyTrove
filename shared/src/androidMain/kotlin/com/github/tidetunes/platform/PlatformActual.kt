package com.github.tidetunes.platform

import android.graphics.BitmapFactory
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

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bm.asImageBitmap()
}

lateinit var appContext: android.content.Context
