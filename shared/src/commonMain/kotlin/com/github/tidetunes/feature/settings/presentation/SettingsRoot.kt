package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.RouteDebugMore
import com.github.tidetunes.core.RouteLog

@Composable
fun SettingsRoot() {
    val navController = LocalNavController.current
    val uriHandler = LocalUriHandler.current

    SettingsScreen(
        appVersion = getAppVersion(),
        onAction = { action ->
            when (action) {
                SettingsAction.NavigateToLog -> navController.navigate(RouteLog())
                SettingsAction.NavigateToDebugMore -> navController.navigate(RouteDebugMore())
                is SettingsAction.OpenGitRepo -> uriHandler.openUri(action.url)
            }
        }
    )
}
