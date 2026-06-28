package com.github.tidetunes.feature.queue.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QueueRoot(
    onNavigateBack: () -> Unit,
    viewModel: QueueViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    QueueScreen(
        state = state,
        onAction = { action ->
            when (action) {
                QueueAction.NavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        },
    )
}
