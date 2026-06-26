package com.github.tidetune.platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun getAppDocumentDir(): String {
    return appContext.filesDir.absolutePath
}

actual fun getAppCacheDir(): String {
    return appContext.cacheDir.absolutePath
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bm.asImageBitmap()
}

lateinit var appContext: android.content.Context
