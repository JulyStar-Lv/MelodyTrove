package io.github.julystar.musicapp.feature.queue.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.julystar.musicapp.core.presentation.theme.AppTheme
import io.github.julystar.musicapp.core.presentation.theme.AppThemeMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QueueRoot(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: QueueViewModel = koinViewModel(),
) {
    if (!show) return

    val state by viewModel.state.collectAsState()

    AppTheme(
        darkTheme = true,
        themeMode = AppThemeMode.Dark,
    ) {
        QueueDialog(
            state = state,
            onDismiss = onDismiss,
            onAction = viewModel::onAction,
        )
    }
}
