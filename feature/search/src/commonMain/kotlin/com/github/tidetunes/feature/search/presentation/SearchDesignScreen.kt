package com.github.tidetunes.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideLoadingIndicator
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideSearchBar
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.search.generated.resources.Res
import tidetunes.feature.search.generated.resources.icon_download
import tidetunes.feature.search.generated.resources.icon_music_note
import tidetunes.feature.search.generated.resources.icon_search
import tidetunes.feature.search.generated.resources.search_hint
import tidetunes.feature.search.generated.resources.search_title
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchDesignScreen(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        val compact = maxWidth < 1024.dp
        val pagePadding = if (compact) TideTunesTokens.spacing.pageCompact else TideTunesTokens.spacing.pageExpanded

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .widthIn(max = TideTunesTokens.adaptive.contentMaxWidth),
            contentPadding = PaddingValues(
                start = pagePadding,
                top = if (compact) 10.dp else 8.dp,
                end = pagePadding,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!compact) {
                item {
                    TidePageHeader(
                        title = stringResource(Res.string.search_title),
                        subtitle = "Songs, artists, albums, genres and connected sources.",
                    )
                }
            }
            item {
                TideSearchBar(
                    value = state.query,
                    onValueChange = { onAction(SearchAction.QueryChanged(it)) },
                    placeholder = stringResource(Res.string.search_hint),
                    onSearch = { onAction(SearchAction.SubmitSearch) },
                    onClear = { onAction(SearchAction.ClearQuery) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when (state.loadState) {
                SearchLoadState.Searching -> item {
                    SearchStatus(
                        title = "Searching your library",
                        message = state.query,
                        loading = true,
                    )
                }
                SearchLoadState.Error -> {
                    item {
                        SearchStatus(
                            title = "Some sources are unavailable",
                            message = "Check the source connection and try again.",
                            actionLabel = "Retry",
                            onAction = { onAction(SearchAction.Retry) },
                        )
                    }
                    item { SearchDiscovery(state, onAction) }
                }
                SearchLoadState.Empty -> {
                    item {
                        SearchStatus(
                            title = "No matches yet",
                            message = "Try a song, artist, album, genre or source name.",
                            actionLabel = "Clear search",
                            onAction = { onAction(SearchAction.ClearQuery) },
                        )
                    }
                    item { SearchDiscovery(state, onAction) }
                }
                SearchLoadState.Results -> {
                    item {
                        SearchResultsHeader(
                            count = state.tracks.size,
                            query = state.query,
                        )
                    }
                    itemsIndexed(
                        items = state.tracks,
                        key = { index, track -> track.id ?: track.mediaId?.value ?: "$index-${track.title}" },
                    ) { index, track ->
                        SearchResultRow(
                            rank = index + 1,
                            track = track,
                            onOpen = { onAction(SearchAction.OpenTrack(track)) },
                            onDownload = { onAction(SearchAction.DownloadTrack(track)) },
                        )
                    }
                }
                SearchLoadState.Idle,
                SearchLoadState.Typing -> item {
                    SearchDiscovery(state, onAction)
                }
            }
        }
    }
}

@Composable
private fun SearchDiscovery(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        RecentSearches(
            searches = state.history.ifEmpty { state.suggestions }.take(8),
            onSelect = { onAction(SearchAction.SelectSuggestion(it)) },
            onClear = { onAction(SearchAction.ClearHistory) },
        )
        GenreSection(onSelect = { onAction(SearchAction.SelectSuggestion(it)) })
        TrendingSection(
            suggestions = state.suggestions.ifEmpty { defaultTrendingQueries },
            onSelect = { onAction(SearchAction.SelectSuggestion(it)) },
        )
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (searches.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            title = "Recent Searches",
            action = "Clear",
            onAction = onClear,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            searches.forEach { search ->
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                        .background(MiuixTheme.colorScheme.surfaceVariant)
                        .clickable { onSelect(search) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_search),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = search,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GenreSection(onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title = "Browse Genres")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 900.dp -> 5
                maxWidth >= 600.dp -> 4
                else -> 2
            }
            val gap = 12.dp
            val width = (maxWidth - gap * (columns - 1)) / columns
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                maxItemsInEachRow = columns,
            ) {
                searchGenreCards.forEach { genre ->
                    GenreCard(
                        genre = genre,
                        width = width,
                        onClick = { onSelect(genre.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GenreCard(
    genre: SearchGenre,
    width: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(genre.start, genre.end)))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = "#",
            color = Color.White.copy(alpha = 0.42f),
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            text = genre.label,
            color = Color.White,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun TrendingSection(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(
            title = "Trending in Your Library",
            subtitle = "Your most-played searches · Last 7 days",
        )
        suggestions.take(6).forEachIndexed { index, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(label) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(18.dp),
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    searchGenreCards[index % searchGenreCards.size].start,
                                    searchGenreCards[index % searchGenreCards.size].end,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_music_note),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.84f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Across your connected music sources",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsHeader(count: Int, query: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = "Search Results",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$count matches for “${query.trim()}”",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun SearchResultRow(
    rank: Int,
    track: SearchTrackItem,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = rank.toString(),
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(18.dp),
        )
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            searchGenreCards[(rank - 1) % searchGenreCards.size].start,
                            searchGenreCards[(rank - 1) % searchGenreCards.size].end,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_music_note),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.84f),
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(track.artist, track.sourceLabel.takeIf { it.isNotBlank() }).joinToString(" · "),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (track.mediaId != null) {
            Icon(
                painter = painterResource(Res.drawable.icon_download),
                contentDescription = "Download",
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDownload)
                    .padding(8.dp)
                    .size(17.dp),
            )
        }
    }
}

@Composable
private fun SearchStatus(
    title: String,
    message: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    TideCardSurface(contentPadding = PaddingValues(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    TideLoadingIndicator(size = 22.dp)
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.icon_search),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAction)
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = MiuixTheme.colorScheme.primary,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAction)
                    .padding(6.dp),
            )
        }
    }
}

private data class SearchGenre(
    val label: String,
    val start: Color,
    val end: Color,
)

private val searchGenreCards = listOf(
    SearchGenre("Electronic", TideTunesBrand.Primary, TideTunesBrand.Secondary),
    SearchGenre("Ambient", TideTunesBrand.Secondary, TideTunesBrand.SupportBlue),
    SearchGenre("Synthwave", TideTunesBrand.SupportOrange, TideTunesBrand.Primary),
    SearchGenre("Techno", TideTunesBrand.SupportGreen, TideTunesBrand.SupportBlue),
    SearchGenre("IDM", TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange),
    SearchGenre("Post-Rock", TideTunesBrand.SupportBlue, TideTunesBrand.Secondary),
    SearchGenre("Shoegaze", TideTunesBrand.Primary, TideTunesBrand.SupportOrange),
    SearchGenre("Experimental", TideTunesBrand.Secondary, TideTunesBrand.SupportGreen),
    SearchGenre("Jazz", TideTunesBrand.SupportOrange, TideTunesBrand.SupportYellow),
    SearchGenre("Classical", TideTunesBrand.SupportBlue, TideTunesBrand.SupportGreen),
)

private val defaultTrendingQueries = listOf(
    "Midnight Cascade",
    "Luna Waves",
    "Synthwave",
    "Hi-Res",
    "Ambient",
    "Recently Added",
)
