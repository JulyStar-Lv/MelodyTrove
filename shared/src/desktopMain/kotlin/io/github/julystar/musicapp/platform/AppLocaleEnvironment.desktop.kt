package io.github.julystar.musicapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

private val systemLocaleAtStartup: Locale = Locale.getDefault()
private val DesktopAppLocale = staticCompositionLocalOf { systemLocaleAtStartup.toLanguageTag() }

actual object LocalAppLocale {
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val locale = value?.let(Locale::forLanguageTag) ?: systemLocaleAtStartup
        Locale.setDefault(locale)
        return DesktopAppLocale.provides(locale.toLanguageTag())
    }
}
