package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import platform.Foundation.NSBundle

@Composable
actual fun getAppVersion(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        ?: "<unknown>"
}
