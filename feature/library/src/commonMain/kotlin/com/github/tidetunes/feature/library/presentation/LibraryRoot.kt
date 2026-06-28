package com.github.tidetunes.feature.library.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryRoot(
    viewModel: LibraryVM = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ShowMessage -> Unit
            }
        }
    }

    LibraryScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
