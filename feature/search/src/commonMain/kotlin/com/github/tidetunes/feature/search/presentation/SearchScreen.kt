package com.github.tidetunes.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideChipSection
import com.github.tidetunes.core.presentation.components.TideIconBadge
import com.github.tidetunes.core.presentation.components.TideIconBadgeVariant
import com.github.tidetunes.core.presentation.components.TideLoadingIndicator
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideSearchBar
import com.github.tidetunes.core.presentation.components.TideStatusBadge
import com.github.tidetunes.core.presentation.components.TideStatusTone
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.search.generated.resources.Res
import tidetunes.feature.search.generated.resources.downloads_title
import tidetunes.feature.search.generated.resources.search_empty
import tidetunes.feature.search.generated.resources.search_hint
import tidetunes.feature.search.generated.resources.search_remote_failures
import tidetunes.feature.search.generated.resources.search_suggestions
import tidetunes.feature.search.generated.resources.search_title
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchScreen(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TideTunesTokens.spacing

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .padding(horizontal = horizontalPadding, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        TidePageHeader(
            title = stringResource(Res.string.search_title),
            subtitle = "One search across every source",
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                    .background(MiuixTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Global",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        SearchInput(
            state = state,
            onAction = onAction,
        )
        if (state.failedSourceCount > 0) {
            TideStatusBadge(
                label = "${state.failedSourceCount} ${stringResource(Res.string.search_remote_failures)}",
                tone = TideStatusTone.Error,
            )
        }
        SearchContent(
            state = state,
            onAction = onAction,
            modifier = Modifier.weight(1f),
        )
    }
    }
}

@Composable
private fun SearchInput(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
) {
    TideSearchBar(
        value = state.query,
        onValueChange = { query -> onAction(SearchAction.QueryChanged(query)) },
        placeholder = stringResource(Res.string.search_hint),
        onSearch = { onAction(SearchAction.SubmitSearch) },
        onClear = { onAction(SearchAction.ClearQuery) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SearchContent(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = TideTunesTokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(TideTunesTokens.spacing.md),
    ) {
        when (state.loadState) {
            SearchLoadState.Searching -> {
                item {
                    SearchStatusCard(
                        title = "Searching",
                        message = state.query.ifBlank { stringResource(Res.string.search_hint) },
                        loading = true,
                    )
                }
            }
            SearchLoadState.Error -> {
                item {
                    SearchStatusCard(
                        title = stringResource(Res.string.search_remote_failures),
                        message = "Check source connection and try again.",
                        actionText = "Retry",
                        onAction = { onAction(SearchAction.Retry) },
                    )
                }
                item {
                    SearchDiscovery(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
            SearchLoadState.Empty -> {
                item {
                    SearchStatusCard(
                        title = stringResource(Res.string.search_empty),
                        message = "Try a recent search, artist, album, folder, or source.",
                    )
                }
                item {
                    SearchDiscovery(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
            SearchLoadState.Results -> {
                item {
                    SearchResultHeader(resultCount = state.tracks.size)
                }
                itemsIndexed(
                    items = state.tracks,
                    key = { index, track -> track.lazyListKey(index) },
                ) { _, track ->
                    SearchTrackCard(
                        track = track,
                        onClick = { onAction(SearchAction.OpenTrack(track)) },
                        onDownload = { onAction(SearchAction.DownloadTrack(track)) },
                    )
                }
            }
            SearchLoadState.Idle,
            SearchLoadState.Typing -> {
                item {
                    SearchDiscovery(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultHeader(resultCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Songs",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.subtitle,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$resultCount results",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun SearchStatusCard(
    title: String,
    message: String,
    loading: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    TideCardSurface(
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TideIconBadge(
                variant = TideIconBadgeVariant.Surface,
            ) {
                if (loading) {
                    TideLoadingIndicator(size = 24.dp)
                } else {
                    Text(
                        text = "S",
                        color = MiuixTheme.colorScheme.primary,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (actionText != null && onAction != null) {
                TideTextButton(
                    text = actionText,
                    variant = TideTextButtonVariant.Primary,
                    size = TideTextButtonSize.Small,
                    onClick = onAction,
                )
            }
        }
    }
}

@Composable
private fun SearchDiscovery(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TideTunesTokens.spacing.md)) {
        if (state.history.isNotEmpty()) {
            TideChipSection(
                title = "Recent Search",
                labels = state.history.take(8),
                trailing = {
                    TideTextButton(
                        text = "Clear",
                        variant = TideTextButtonVariant.Default,
                        size = TideTextButtonSize.Small,
                        onClick = { onAction(SearchAction.ClearHistory) },
                    )
                },
                onLabelClick = { query -> onAction(SearchAction.SelectSuggestion(query)) },
            )
        }
        TideChipSection(
            title = if (state.suggestions.isNotEmpty()) {
                stringResource(Res.string.search_suggestions)
            } else {
                "Trending"
            },
            labels = state.trendingQueries(),
            onLabelClick = { query -> onAction(SearchAction.SelectSuggestion(query)) },
        )
        SearchCategoryGrid()
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SearchCategoryGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Browse",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.subtitle,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            searchCategories.forEach { category ->
                SearchCategoryCard(category = category)
            }
        }
    }
}

@Composable
private fun SearchCategoryCard(category: SearchCategoryUi) {
    TideCardSurface(
        modifier = Modifier
            .widthIn(min = 154.dp, max = 220.dp)
            .heightIn(min = 76.dp),
        contentPadding = PaddingValues(14.dp),
        fillMaxWidth = false,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TideIconBadge(
                marker = category.marker,
                accentColor = category.tint,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = category.label,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = category.summary,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchTrackCard(
    track: SearchTrackItem,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    TideCardSurface(
        cornerRadius = shapes.md,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TideIconBadge(
                variant = TideIconBadgeVariant.Brand,
                marker = "M",
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = track.title,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist ?: track.sourceLabel,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (track.mediaId != null) {
                    TideTextButton(
                        text = stringResource(Res.string.downloads_title),
                        variant = TideTextButtonVariant.Primary,
                        size = TideTextButtonSize.Small,
                        onClick = onDownload,
                    )
                }
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .widthIn(max = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = track.sourceLabel,
                        color = MiuixTheme.colorScheme.primary,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.durationMs.durationText(),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
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

private fun SearchState.trendingQueries(): List<String> {
    val historySet = history.toSet()
    val suggestionQueries = suggestions
        .filterNot { query -> query in historySet }
        .map { query -> query.trim() }
        .filter { query -> query.isNotBlank() }
        .distinct()

    return (suggestionQueries.ifEmpty { fallbackTrendingQueries }).take(8)
}

private fun Long?.durationText(): String {
    val duration = this?.milliseconds ?: return "--:--"
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private data class SearchCategoryUi(
    val label: String,
    val summary: String,
    val marker: String,
    val tint: Color,
)

private val fallbackTrendingQueries = listOf(
    "Lossless",
    "Hi-Res",
    "WebDAV",
    "Recently Added",
    "Live",
    "Karaoke",
)

private val searchCategories = listOf(
    SearchCategoryUi(
        label = "Albums",
        summary = "Album matches",
        marker = "A",
        tint = TideTunesBrand.Primary,
    ),
    SearchCategoryUi(
        label = "Artists",
        summary = "Artist matches",
        marker = "R",
        tint = TideTunesBrand.Secondary,
    ),
    SearchCategoryUi(
        label = "Songs",
        summary = "Track results",
        marker = "S",
        tint = TideTunesBrand.SupportBlue,
    ),
    SearchCategoryUi(
        label = "Folders",
        summary = "Storage paths",
        marker = "F",
        tint = TideTunesBrand.SupportOrange,
    ),
    SearchCategoryUi(
        label = "Sources",
        summary = "Connected sources",
        marker = "D",
        tint = TideTunesBrand.SupportGreen,
    ),
)
