package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.feature.settings.presentation.LogVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LogRoot(
    logVM: LogVM = koinViewModel(),
) {
    val logs by logVM.logs.collectAsState()

    LaunchedEffect(Unit) {
        logVM.reload()
    }

    LogScreen(
        logs = logs.map { LogEntry(name = it.name) },
        onAction = { action ->
            when (action) {
                LogAction.Reload -> logVM.reload()
            }
        },
    )
}
