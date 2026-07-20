package com.github.tidetunes.feature.home.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoot(
    scaffoldPadding: PaddingValues,
    onNavigateToDownloads: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToDownloads -> onNavigateToDownloads()
                HomeEvent.NavigateToLibrary -> onNavigateToLibrary()
                HomeEvent.NavigateToSearch -> onNavigateToSearch()
                HomeEvent.OpenSleepTimer -> onOpenSleepTimer()
                HomeEvent.OpenNowPlaying -> onOpenNowPlaying()
            }
        }
    }

    HomeDesignScreen(
        scaffoldPadding = scaffoldPadding,
        state = state,
        onAction = viewModel::onAction,
    )
}
