package com.github.tidetunes.platform

import platform.Foundation.NSBundle

actual fun getAppVersion(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        ?: "<unknown>"
}

actual fun getAppBuildInfo(): String {
    return "iOS bundle"
}
