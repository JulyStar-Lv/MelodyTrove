package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            state.lines.isEmpty() && state.wordTimedLines.isEmpty() -> AppEmptyState(message = "No lyrics available.")
            state.wordTimedLines.isNotEmpty() -> WordTimedLyricsContent(state)
            else -> FlatLyricsContent(state)
        }
    }
}

@Composable
private fun FlatLyricsContent(state: LyricsState) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text(
                text = state.trackTitle,
                style = MiuixTheme.textStyles.title3,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
        state.trackArtist?.let { artist ->
            item {
                Text(
                    text = artist,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
        itemsIndexed(state.lines, key = { index, _ -> index }) { _, line ->
            Text(
                text = line,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun WordTimedLyricsContent(state: LyricsState) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text(
                text = state.trackTitle,
                style = MiuixTheme.textStyles.title3,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
        state.trackArtist?.let { artist ->
            item {
                Text(
                    text = artist,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
        itemsIndexed(state.wordTimedLines, key = { index, _ -> index }) { _, line ->
            Text(
                text = buildAnnotatedString {
                    if (line.words.isEmpty()) {
                        append(line.text)
                    } else {
                        line.words.forEachIndexed { wi, word ->
                            val isActive = true
                            if (isActive && wi == line.words.lastIndex) {
                                pushStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                    )
                                )
                            }
                            append(word.text)
                            if (isActive && wi == line.words.lastIndex) {
                                pop()
                            }
                        }
                    }
                },
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}
