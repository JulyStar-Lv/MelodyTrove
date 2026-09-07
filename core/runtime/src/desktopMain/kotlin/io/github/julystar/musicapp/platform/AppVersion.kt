@file:JvmName("DesktopAppVersionKt")

package io.github.julystar.musicapp.platform

actual fun getAppVersion(): String {
    return GeneratedBuildInfo.appVersionName
}

actual fun getAppBuildInfo(): String {
    val osName = System.getProperty("os.name").orEmpty().ifBlank { "Desktop" }
    val osVersion = System.getProperty("os.version").orEmpty()
    return listOf(osName, osVersion).filter(String::isNotBlank).joinToString(" ")
}
