package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideStatusCard
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LyricsScreen(
    state: LyricsState,
    onAction: (LyricsAction) -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            TidePageHeader(
                title = state.trackTitle,
                subtitle = state.trackArtist ?: "",
            )
            when {
                state.isLoading -> TideStatusCard(title = "Loading lyrics", message = state.trackTitle, loading = true, modifier = Modifier.weight(1f))
                state.error != null -> TideStatusCard(title = "Lyrics unavailable", message = state.error, actionText = "Retry", onAction = { onAction(LyricsAction.Retry) }, modifier = Modifier.weight(1f))
                state.lines.isEmpty() && state.wordTimedLines.isEmpty() -> TideStatusCard(title = "No lyrics", message = state.trackTitle, modifier = Modifier.weight(1f))
                state.wordTimedLines.isNotEmpty() -> WordTimedLyricsContent(state)
                else -> FlatLyricsContent(state)
            }
        }
    }
}

@Composable
private fun FlatLyricsContent(state: LyricsState) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text(text = state.trackTitle, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        state.trackArtist?.let { artist ->
            item {
                Text(text = artist, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), textAlign = TextAlign.Center)
            }
        }
        itemsIndexed(state.lines, key = { index, _ -> index }) { _, line ->
            Text(text = line, style = MiuixTheme.textStyles.body1, modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WordTimedLyricsContent(state: LyricsState) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text(text = state.trackTitle, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        state.trackArtist?.let { artist ->
            item {
                Text(text = artist, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), textAlign = TextAlign.Center)
            }
        }
        itemsIndexed(state.wordTimedLines, key = { index, _ -> index }) { _, line ->
            Text(
                text = buildAnnotatedString {
                    if (line.words.isEmpty()) { append(line.text) }
                    else {
                        line.words.forEachIndexed { wi, word ->
                            val isActive = true
                            if (isActive && wi == line.words.lastIndex) {
                                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                            }
                            append(word.text)
                            if (isActive && wi == line.words.lastIndex) pop()
                        }
                    }
                },
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
