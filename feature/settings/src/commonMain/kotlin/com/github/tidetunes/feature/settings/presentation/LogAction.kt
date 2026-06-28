package com.github.tidetunes.feature.settings.presentation

sealed interface LogAction {
    data object Reload : LogAction
}
