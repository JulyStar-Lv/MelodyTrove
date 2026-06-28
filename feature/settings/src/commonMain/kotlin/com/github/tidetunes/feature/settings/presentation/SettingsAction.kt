package com.github.tidetunes.feature.settings.presentation

sealed interface SettingsAction {
    data object NavigateToLog : SettingsAction
    data object NavigateToDebugMore : SettingsAction
    data class OpenGitRepo(val url: String) : SettingsAction
}
