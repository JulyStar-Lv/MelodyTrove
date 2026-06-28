package com.github.tidetunes.feature.sources.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.tidetunes.core.domain.model.toStorageRouteIdOrNull
import com.github.tidetunes.navigation.NEW_STORAGE_ID
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SourcesRoot(
    onNavigateToSourceEditor: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourcesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SourcesEvent.OpenNewSourceEditor -> onNavigateToSourceEditor(NEW_STORAGE_ID)
                is SourcesEvent.OpenSourceEditor -> {
                    event.id.toStorageRouteIdOrNull()?.let(onNavigateToSourceEditor)
                }
            }
        }
    }

    SourcesScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
