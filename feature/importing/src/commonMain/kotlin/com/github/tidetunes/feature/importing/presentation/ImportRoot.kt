package com.github.tidetunes.feature.importing.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.feature.importing.presentation.ImportVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportRoot(
    onNavigateBack: () -> Unit,
    viewModel: ImportVM = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ImportEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    ImportScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
