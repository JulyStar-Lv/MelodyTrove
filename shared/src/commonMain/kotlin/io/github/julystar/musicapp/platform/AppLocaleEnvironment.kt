package io.github.julystar.musicapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import io.github.julystar.musicapp.core.domain.model.AppLanguageMode

/**
 * Provides the locale used by Compose Multiplatform resources.
 *
 * Platform default-locale APIs alone do not invalidate the resource environment,
 * so the localized UI subtree is keyed by the selected language as well.
 */
expect object LocalAppLocale {
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppLocaleEnvironment(
    languageMode: AppLanguageMode,
    content: @Composable () -> Unit,
) {
    val localeTag = languageMode.resourceLocaleTag()
    CompositionLocalProvider(LocalAppLocale provides localeTag) {
        key(localeTag) {
            content()
        }
    }
}

internal fun AppLanguageMode.resourceLocaleTag(): String? = when (this) {
    AppLanguageMode.System -> null
    AppLanguageMode.Chinese -> "zh-Hans"
    AppLanguageMode.English -> "en"
}
