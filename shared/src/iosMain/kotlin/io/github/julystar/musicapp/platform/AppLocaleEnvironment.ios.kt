package io.github.julystar.musicapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"
private val systemLanguageAtStartup: String =
    NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
private val IosAppLocale = staticCompositionLocalOf { systemLanguageAtStartup }

actual object LocalAppLocale {
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val localeTag = value ?: systemLanguageAtStartup
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) {
            defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
        } else {
            defaults.setObject(listOf(localeTag), APPLE_LANGUAGES_KEY)
        }
        return IosAppLocale.provides(localeTag)
    }
}
