package com.github.tidetunes.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import platform.Foundation.*

private fun platformDirectory(directory: ULong): String {
    return NSSearchPathForDirectoriesInDomains(
        directory = directory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).firstOrNull() as? String ?: error("iOS directory $directory is unavailable")
}

actual fun getAppDocumentDir(): String = platformDirectory(NSDocumentDirectory)

actual fun getAppCacheDir(): String = platformDirectory(NSCachesDirectory)

actual fun getAppDatabasePath(): String? = "${getAppDocumentDir()}/tidetunes.db"

actual fun isSystemDynamicColorAvailable(): Boolean = false

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1_000.0).toLong()

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}
