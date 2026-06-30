package com.github.tidetunes.feature.sources.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.tidetunes.feature.sources.presentation.SourcesRoot

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
