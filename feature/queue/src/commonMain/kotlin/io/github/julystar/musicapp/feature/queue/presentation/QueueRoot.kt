package io.github.julystar.musicapp.feature.queue.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QueueRoot(
    show: Boolean,
    coverNowPlayingLyrics: Boolean,
    onDismiss: () -> Unit,
    viewModel: QueueViewModel = koinViewModel(),
) {
    if (!show) return

    val state by viewModel.state.collectAsState()

    QueueDialog(
        state = state,
        coverNowPlayingLyrics = coverNowPlayingLyrics,
        onDismiss = onDismiss,
        onAction = viewModel::onAction,
    )
}
