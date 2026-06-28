package com.github.tidetunes.feature.downloads.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadsRoot(
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DownloadsEvent.ShowMessage -> onShowMessage(event.message)
            }
        }
    }

    DownloadsScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
