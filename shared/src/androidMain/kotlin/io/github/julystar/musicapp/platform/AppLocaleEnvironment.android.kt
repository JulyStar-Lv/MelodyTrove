package io.github.julystar.musicapp.platform

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private val systemLocaleAtStartup: Locale by lazy { Locale.getDefault() }

actual object LocalAppLocale {
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val locale = value?.let(Locale::forLanguageTag) ?: systemLocaleAtStartup
        val configuration = Configuration(LocalConfiguration.current).apply {
            setLocale(locale)
        }
        Locale.setDefault(locale)

        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}
