package com.github.tidetunes.feature.settings.presentation

sealed interface DebugAction {
    data object TriggerRustError : DebugAction
    data object TriggerRustAsyncError : DebugAction
    data object TriggerRustPanic : DebugAction
    data object TriggerKotlinError : DebugAction
    data object TriggerKotlinAsyncError : DebugAction
}
