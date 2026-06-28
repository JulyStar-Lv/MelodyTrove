package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LyricsRoot(
    onNavigateBack: () -> Unit,
    viewModel: LyricsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LyricsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                LyricsAction.NavigateBack -> onNavigateBack()
                LyricsAction.Retry -> viewModel.onAction(action)
            }
        },
    )
}
