package com.github.tidetunes.feature.search.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.AppChip
import com.github.tidetunes.core.presentation.components.AppTextField
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.search.generated.resources.Res
import tidetunes.feature.search.generated.resources.downloads_title
import tidetunes.feature.search.generated.resources.search_empty
import tidetunes.feature.search.generated.resources.search_hint
import tidetunes.feature.search.generated.resources.search_suggestions
import tidetunes.feature.search.generated.resources.search_remote_failures
import tidetunes.feature.search.generated.resources.search_title

@Composable
fun SearchScreen(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.search_title),
            color = MiuixTheme.colorScheme.primary,
            fontSize = 20.sp,
        )
        AppTextField(
            value = state.query,
            onValueChange = { query -> onAction(SearchAction.QueryChanged(query)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = stringResource(Res.string.search_hint),
        )
        if (state.suggestions.isNotEmpty()) {
            SearchSuggestions(
                title = stringResource(Res.string.search_suggestions),
                suggestions = state.suggestions,
                onSelect = { query -> onAction(SearchAction.SelectSuggestion(query)) },
            )
        }
        if (state.failedSourceCount > 0) {
            Text(
                text = "${state.failedSourceCount} ${stringResource(Res.string.search_remote_failures)}",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
        SearchContent(
            state = state,
            onOpenTrack = { track -> onAction(SearchAction.OpenTrack(track)) },
            onDownloadTrack = { track -> onAction(SearchAction.DownloadTrack(track)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SearchSuggestions(
    title: String,
    suggestions: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { suggestion ->
                AppChip(
                    label = suggestion,
                    onClick = { onSelect(suggestion) },
                )
            }
        }
    }
}

@Composable
private fun SearchContent(
    state: SearchState,
    onOpenTrack: (SearchTrackItem) -> Unit,
    onDownloadTrack: (SearchTrackItem) -> Unit,
    modifier: Modifier,
) {
    when (state.loadState) {
        SearchLoadState.Searching -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        SearchLoadState.Empty -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.search_empty),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                )
            }
        }
        SearchLoadState.Error -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.search_remote_failures),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                )
            }
        }
        SearchLoadState.Idle,
        SearchLoadState.Typing,
        SearchLoadState.Results -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = state.tracks,
                    key = { index, track -> track.lazyListKey(index) },
                ) { _, track ->
                    SearchTrackRow(
                        track = track,
                        onClick = { onOpenTrack(track) },
                        onDownload = { onDownloadTrack(track) },
                    )
                }
            }
        }
    }
}

internal fun SearchTrackItem.lazyListKey(index: Int): String {
    val itemKey = mediaId?.let { mediaId ->
        "${mediaId.sourceId.value}:${mediaId.remoteId}"
    } ?: "local:${id ?: "unknown"}"
    return "search-track-$index-$itemKey"
}

@Composable
private fun SearchTrackRow(
    track: SearchTrackItem,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist ?: track.sourceLabel,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (track.mediaId != null) {
                TideTunesTextButton(
                    text = stringResource(Res.string.downloads_title),
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Small,
                    onClick = onDownload,
                )
            }
            Text(
                text = track.sourceLabel,
                color = MiuixTheme.colorScheme.primary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.durationMs.durationText(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
    }
}

private fun Long?.durationText(): String {
    val duration = this?.milliseconds ?: return "--:--"
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
