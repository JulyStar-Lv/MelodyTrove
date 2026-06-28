package com.github.tidetunes.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.core.domain.model.MediaId
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchRoot(
    onOpenTrack: (MediaId) -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.OpenTrack -> onOpenTrack(event.mediaId)
                is SearchEvent.ShowMessage -> Unit
            }
        }
    }

    SearchScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
