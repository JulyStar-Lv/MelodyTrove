package com.github.tidetunes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.presentation.theme.TideTunesTheme
import com.github.tidetunes.core.presentation.theme.TideTunesThemeMode
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.RoutesProvider
import com.github.tidetunes.navigation.AppNavigation
import com.github.tidetunes.platform.isSystemDynamicColorAvailable
import org.koin.compose.koinInject

@Composable
fun Root() {
    RoutesProvider {
        val controller = LocalNavController.current
        val settingsRepository = koinInject<SettingsRepository>()
        val settings by settingsRepository.settings.collectAsState(AppSettings.Default)

        TideTunesTheme(
            themeMode = settings.themeMode.toPresentationThemeMode(),
            dynamicColor = settings.dynamicColorEnabled && isSystemDynamicColorAvailable(),
        ) {
            AppNavigation(navController = controller)
        }
    }
}

private fun AppThemeMode.toPresentationThemeMode(): TideTunesThemeMode {
    return when (this) {
        AppThemeMode.System -> TideTunesThemeMode.FollowSystem
        AppThemeMode.Light -> TideTunesThemeMode.Light
        AppThemeMode.Dark -> TideTunesThemeMode.Dark
    }
}
