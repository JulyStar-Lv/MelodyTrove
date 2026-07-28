package io.github.julystar.musicapp.feature.sources.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.feature.sources.presentation.SourcesRoot

@Composable
fun SourcesDashboardContent(
    onNavigateToSourceEditor: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SourcesRoot(
        onNavigateToSourceEditor = onNavigateToSourceEditor,
        modifier = modifier,
    )
}
