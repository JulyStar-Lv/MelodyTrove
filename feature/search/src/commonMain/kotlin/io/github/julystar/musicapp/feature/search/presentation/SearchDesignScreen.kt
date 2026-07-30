package io.github.julystar.musicapp.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility entry point retained for existing navigation wiring.
 * The production route delegates to the localized, real-data search screen.
 */
@Composable
fun SearchDesignScreen(
    state: SearchState,
    showSearchContent: Boolean = true,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchScreen(
        state = state,
        onAction = onAction,
        modifier = modifier,
        showSearchContent = showSearchContent,
    )
}
