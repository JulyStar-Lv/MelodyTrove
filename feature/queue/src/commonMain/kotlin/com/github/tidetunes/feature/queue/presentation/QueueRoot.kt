package com.github.tidetunes.feature.queue.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.core.presentation.theme.TideTunesTheme
import com.github.tidetunes.core.presentation.theme.TideTunesThemeMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QueueRoot(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: QueueViewModel = koinViewModel(),
) {
    if (!show) return

    val state by viewModel.state.collectAsState()

    TideTunesTheme(
        darkTheme = true,
        themeMode = TideTunesThemeMode.Dark,
    ) {
        QueueDialog(
            state = state,
            onDismiss = onDismiss,
            onAction = viewModel::onAction,
        )
    }
}
