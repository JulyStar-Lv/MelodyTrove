package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun SettingsRoot(
    appVersion: String,
    onNavigateToLog: () -> Unit,
    onNavigateToDebugMore: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    SettingsScreen(
        appVersion = appVersion,
        onAction = { action ->
            when (action) {
                SettingsAction.NavigateToLog -> onNavigateToLog()
                SettingsAction.NavigateToDebugMore -> onNavigateToDebugMore()
                is SettingsAction.OpenGitRepo -> uriHandler.openUri(action.url)
            }
        },
    )
}
