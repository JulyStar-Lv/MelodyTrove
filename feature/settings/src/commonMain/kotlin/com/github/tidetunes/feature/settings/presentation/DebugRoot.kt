package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DebugRoot(
    debugMoreVM: DebugMoreVM = koinViewModel(),
) {
    DebugScreen { action ->
        when (action) {
            DebugAction.TriggerRustError -> debugMoreVM.triggerRustError()
            DebugAction.TriggerRustAsyncError -> debugMoreVM.triggerRustAsyncError()
            DebugAction.TriggerRustPanic -> debugMoreVM.triggerRustPanic()
            DebugAction.TriggerKotlinError -> debugMoreVM.triggerKotlinError()
            DebugAction.TriggerKotlinAsyncError -> debugMoreVM.triggerKotlinAsyncError()
        }
    }
}
