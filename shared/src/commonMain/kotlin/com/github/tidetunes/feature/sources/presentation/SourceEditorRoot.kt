package com.github.tidetunes.feature.sources.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import com.github.tidetunes.viewmodels.EditStorageVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SourceEditorRoot(
    onNavigateBack: () -> Unit,
    onNavigateToLibraryFolderImport: () -> Unit,
    viewModel: EditStorageVM = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SourceEditorEvent.NavigateBack -> onNavigateBack()
                SourceEditorEvent.OpenLibraryFolderImport -> onNavigateToLibraryFolderImport()
                is SourceEditorEvent.OpenOneDriveOAuth -> {
                    uriHandler.openUri(event.authorizationUrl)
                }
            }
        }
    }

    SourceEditorScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
