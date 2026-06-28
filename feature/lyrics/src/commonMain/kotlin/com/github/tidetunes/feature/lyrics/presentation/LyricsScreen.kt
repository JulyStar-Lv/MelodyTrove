package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppEmptyState
import com.github.tidetunes.core.presentation.components.AppErrorState
import com.github.tidetunes.core.presentation.components.AppLoadingIndicator
import com.github.tidetunes.core.presentation.components.AppTopBar

@Composable
fun LyricsScreen(
    state: LyricsState,
    onAction: (LyricsAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.trackTitle,
            navigationIcon = null,
        )
        when {
            state.isLoading -> AppLoadingIndicator()
            state.error != null -> AppErrorState(message = state.error, onRetry = { onAction(LyricsAction.Retry) })
            state.lines.isEmpty() -> AppEmptyState(message = "No lyrics available.")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Text(
                        text = state.trackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                state.trackArtist?.let { artist ->
                    item {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
                itemsIndexed(state.lines, key = { index, _ -> index }) { _, line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }
}
