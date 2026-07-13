@file:JvmName("DesktopAppVersionKt")

package com.github.tidetunes.platform

actual fun getAppVersion(): String {
    return "0.3.0-dev"
}

actual fun getAppBuildInfo(): String {
    return "Desktop dev"
}
