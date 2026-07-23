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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.QualityBadge
import com.github.tidetunes.core.presentation.components.QualityBadgeType
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideLoadingIndicator
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideGlassScene
import com.github.tidetunes.core.presentation.components.TideSearchBar
import com.github.tidetunes.core.presentation.components.TideStickyGlassActionBar
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesFontFamilies
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.search.generated.resources.Res
import tidetunes.feature.search.generated.resources.icon_music_note
import tidetunes.feature.search.generated.resources.icon_search
import tidetunes.feature.search.generated.resources.search_hint
import tidetunes.feature.search.generated.resources.search_change_filter
import tidetunes.feature.search.generated.resources.search_connection_retry
import tidetunes.feature.search.generated.resources.search_no_matches_yet
import tidetunes.feature.search.generated.resources.search_sources_unavailable
import tidetunes.feature.search.generated.resources.search_title
import tidetunes.feature.search.generated.resources.search_try_query
import tidetunes.feature.search.generated.resources.searching_library
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_timelapse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class SearchResultFilter(val label: String) {
    All("All"),
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists");

    val showsTracks get() = this == All || this == Songs
    val showsAlbums get() = this == All || this == Albums
    val showsArtists get() = this == All || this == Artists
}

@Composable
fun SearchDesignScreen(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var resultFilter by remember { mutableStateOf(SearchResultFilter.All) }

    TideGlassScene(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
        val compact = maxWidth < 1024.dp
        val pagePadding = if (compact) TideTunesTokens.spacing.pageCompact else TideTunesTokens.spacing.pageExpanded
        val listState = rememberLazyListState()
        val collapseDistance = with(LocalDensity.current) { 88.dp.roundToPx() }
        val actionBarProgress by remember(listState, collapseDistance) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    (listState.firstVisibleItemScrollOffset / collapseDistance.toFloat())
                        .coerceIn(0f, 1f)
                }
            }
        }
        val pageTitleAlpha = (1f - actionBarProgress / 0.70f).coerceIn(0f, 1f)

        LazyColumn(
            state = listState,
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
            item {
                TidePageHeader(
                    title = stringResource(Res.string.search_title),
                    subtitle = if (compact) null else "Songs, artists, albums, genres and connected sources.",
                    modifier = Modifier.alpha(pageTitleAlpha),
                )
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
                SearchLoadState.Searching -> {
                    item {
                        SearchStatus(
                            title = stringResource(Res.string.searching_library),
                            message = state.query,
                            loading = true,
                        )
                    }
                }
                SearchLoadState.Error -> {
                    item {
                        SearchStatus(
                            title = stringResource(Res.string.search_sources_unavailable),
                            message = stringResource(Res.string.search_connection_retry),
                            actionLabel = "Retry",
                            onAction = { onAction(SearchAction.Retry) },
                        )
                    }
                    item { SearchDiscovery(state, onAction) }
                }
                SearchLoadState.Empty -> {
                    item {
                        SearchStatus(
                            title = stringResource(Res.string.search_no_matches_yet),
                            message = stringResource(Res.string.search_try_query),
                            actionLabel = "Clear search",
                            onAction = { onAction(SearchAction.ClearQuery) },
                        )
                    }
                    item { SearchDiscovery(state, onAction) }
                }
                SearchLoadState.Results -> {
                    item {
                        SearchResultFilters(
                            current = resultFilter,
                            trackCount = state.tracks.size,
                            albumCount = demoSearchAlbums.size,
                            artistCount = demoSearchArtists.size,
                            onSelect = { resultFilter = it },
                        )
                    }
                    if (resultFilter.showsTracks) {
                        item {
                            SearchResultSectionHeader("Songs", state.tracks.size)
                        }
                        itemsIndexed(
                            items = state.tracks,
                            key = { index, track -> track.lazyListKey(index) },
                        ) { index, track ->
                            SearchResultRow(
                                rank = index + 1,
                                track = track,
                                onOpen = { onAction(SearchAction.OpenTrack(track)) },
                            )
                        }
                    }
                    if (resultFilter.showsAlbums) {
                        item {
                            SearchResultSectionHeader("Albums", demoSearchAlbums.size)
                        }
                        item {
                            SearchAlbumRow(
                                albums = demoSearchAlbums,
                                onSelect = { album ->
                                    onAction(SearchAction.SelectSuggestion(album.title))
                                },
                            )
                        }
                    }
                    if (resultFilter.showsArtists) {
                        item {
                            SearchResultSectionHeader("Artists", demoSearchArtists.size)
                        }
                        item {
                            SearchArtistRow(
                                artists = demoSearchArtists,
                                onSelect = { artist ->
                                    onAction(SearchAction.SelectSuggestion(artist.name))
                                },
                            )
                        }
                    }
                    if ((resultFilter == SearchResultFilter.Songs && state.tracks.isEmpty()) ||
                        resultFilter == SearchResultFilter.Albums ||
                        resultFilter == SearchResultFilter.Artists
                    ) {
                        item {
                            SearchStatus(
                                title = "No ${resultFilter.label.lowercase()} found",
                                message = stringResource(Res.string.search_change_filter),
                            )
                        }
                    }
                }
                SearchLoadState.Idle,
                SearchLoadState.Typing -> {
                    item { SearchDiscovery(state, onAction) }
                }
            }
        }
        TideStickyGlassActionBar(
            title = stringResource(Res.string.search_title),
            collapseFraction = actionBarProgress,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        }
    }
}

@Composable
private fun SearchDiscovery(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        RecentSearches(
            searches = state.history.ifEmpty { defaultRecentSearches }.take(8),
            onSelect = { onAction(SearchAction.SelectSuggestion(it)) },
            onClear = { onAction(SearchAction.ClearHistory) },
        )
        GenreGrid(onSelect = { onAction(SearchAction.SelectSuggestion(it)) })
        TrendingNow(
            tracks = demoTrendingTracks,
            onPlay = { onAction(SearchAction.OpenTrack(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RecentSearches(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (searches.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Recent Searches",
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Clear",
                color = MiuixTheme.colorScheme.primary,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            searches.forEach { label ->
                SearchChip(label = label, onClick = { onSelect(label) })
            }
        }
    }
}

@Composable
private fun SearchChip(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_timelapse),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GenreGrid(onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Browse Genres",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 12.dp) / 2
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                searchGenreCards.forEach { genre ->
                    GenreCard(
                        genre = genre,
                        width = cardWidth,
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
            .height(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(genre.start, genre.end)))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = "#",
            color = Color.White.copy(alpha = 0.45f),
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            text = genre.label,
            color = Color.White,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun TrendingNow(
    tracks: List<SearchTrackItem>,
    onPlay: (SearchTrackItem) -> Unit,
) {
    if (tracks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Trending in Your Library",
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Your most-played tracks · Last 7 days",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        tracks.take(6).forEachIndexed { index, track ->
            TrendingRow(
                rank = index + 1,
                track = track,
                onClick = { onPlay(track) },
            )
        }
    }
}

@Composable
private fun TrendingRow(
    rank: Int,
    track: SearchTrackItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = rank.toString(),
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = track.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                QualityBadge(type = track.qualityBadgeType())
            }
            Text(
                text = track.artist ?: track.sourceLabel,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = track.durationMs.durationText(),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1.copy(fontFamily = TideTunesFontFamilies.Mono),
        )
    }
}

@Composable
private fun SearchResultFilters(
    current: SearchResultFilter,
    trackCount: Int,
    albumCount: Int,
    artistCount: Int,
    onSelect: (SearchResultFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val total = trackCount + albumCount + artistCount
        Text(
            text = "Search Results",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$total ${if (total == 1) "match" else "matches"}",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchResultFilter.entries.forEach { filter ->
                val count = when (filter) {
                    SearchResultFilter.All -> total
                    SearchResultFilter.Songs -> trackCount
                    SearchResultFilter.Albums -> albumCount
                    SearchResultFilter.Artists -> artistCount
                }
                FilterPill(
                    label = filter.label,
                    count = count,
                    selected = current == filter,
                    onClick = { onSelect(filter) },
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) MiuixTheme.colorScheme.primary
    else MiuixTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (selected) MiuixTheme.colorScheme.onPrimary
    else MiuixTheme.colorScheme.onSurfaceVariantSummary
    val countColor = if (selected) Color.White.copy(alpha = 0.7f)
    else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Text(
            text = count.toString(),
            color = countColor,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SearchResultSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.subtitle,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$count",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun SearchResultRow(
    rank: Int,
    track: SearchTrackItem,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = rank.toString(),
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(18.dp),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
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
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(16.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = track.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                QualityBadge(type = track.qualityBadgeType())
            }
            Text(
                text = listOfNotNull(track.artist, track.sourceLabel.takeIf { it.isNotBlank() }).joinToString(" · "),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = track.durationMs.durationText(),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1.copy(fontFamily = TideTunesFontFamilies.Mono),
        )
    }
}

@Composable
private fun SearchAlbumRow(
    albums: List<DemoAlbum>,
    onSelect: (DemoAlbum) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = { onSelect(album) }),
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(album.gradientStart, album.gradientEnd),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = album.initials,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = album.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchArtistRow(
    artists: List<DemoArtist>,
    onSelect: (DemoArtist) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        items(artists, key = { it.id }) { artist ->
            Column(
                modifier = Modifier
                    .width(128.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = { onSelect(artist) }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                        .background(
                            Brush.linearGradient(
                                listOf(artist.gradientStart, artist.gradientEnd),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = artist.initials,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = artist.name,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist.followers,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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

private data class SearchGenre(
    val label: String,
    val start: Color,
    val end: Color,
)

private data class DemoAlbum(
    val id: Int,
    val title: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val initials: String,
)

private data class DemoArtist(
    val id: Int,
    val name: String,
    val followers: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val initials: String,
)

private fun Long?.durationText(): String {
    val duration = this?.milliseconds ?: return "--:--"
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun SearchTrackItem.qualityBadgeType(): QualityBadgeType = when {
    sourceLabel.contains("Hi-Res", ignoreCase = true) -> QualityBadgeType.HiRes
    sourceLabel.contains("Lossless", ignoreCase = true) -> QualityBadgeType.Flac
    sourceLabel.contains("Dolby", ignoreCase = true) -> QualityBadgeType.DolbyAtmos
    else -> QualityBadgeType.Flac
}

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

private val defaultRecentSearches = listOf(
    "Luna Waves",
    "Synthwave",
    "Midnight Cascade",
    "Hi-Res",
    "Ambient",
)

private val demoTrendingTracks = listOf(
    SearchTrackItem(id = 1, title = "Midnight Cascade", artist = "Luna Waves · Tidal Drift", durationMs = 222_000, sourceLabel = "Hi-Res"),
    SearchTrackItem(id = 2, title = "Neon Undertow", artist = "Prism Circuit · Voltage Dreams", durationMs = 258_000, sourceLabel = "Lossless"),
    SearchTrackItem(id = 3, title = "Silver Tide", artist = "Coastal Drift · Open Water", durationMs = 235_000, sourceLabel = "Library"),
    SearchTrackItem(id = 4, title = "Aurora Sequence", artist = "Polar Echo · Northern Lights", durationMs = 302_000, sourceLabel = "Dolby Atmos"),
    SearchTrackItem(id = 5, title = "Depth Protocol", artist = "Ocean Syntax · Subsonic", durationMs = 210_000, sourceLabel = "Library"),
    SearchTrackItem(id = 6, title = "Glass Architecture", artist = "Fractal Mind · Prism", durationMs = 284_000, sourceLabel = "Lossless"),
)

private val demoSearchAlbums = listOf(
    DemoAlbum(1, "Tidal Drift", TideTunesBrand.Primary, TideTunesBrand.Secondary, "TD"),
    DemoAlbum(2, "Voltage Dreams", TideTunesBrand.SupportOrange, TideTunesBrand.Primary, "VD"),
    DemoAlbum(3, "Open Water", TideTunesBrand.SupportBlue, TideTunesBrand.Secondary, "OW"),
    DemoAlbum(4, "Northern Lights", TideTunesBrand.Secondary, TideTunesBrand.SupportGreen, "NL"),
    DemoAlbum(5, "Subsonic", TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange, "SS"),
    DemoAlbum(6, "Prism", TideTunesBrand.SupportGreen, TideTunesBrand.SupportBlue, "PR"),
)

private val demoSearchArtists = listOf(
    DemoArtist(1, "Luna Waves", "2.4K", TideTunesBrand.Primary, TideTunesBrand.Secondary, "LW"),
    DemoArtist(2, "Prism Circuit", "1.8K", TideTunesBrand.SupportOrange, TideTunesBrand.Primary, "PC"),
    DemoArtist(3, "Coastal Drift", "3.2K", TideTunesBrand.SupportBlue, TideTunesBrand.Secondary, "CD"),
    DemoArtist(4, "Polar Echo", "5.6K", TideTunesBrand.Secondary, TideTunesBrand.SupportGreen, "PE"),
    DemoArtist(5, "Ocean Syntax", "980", TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange, "OS"),
    DemoArtist(6, "Fractal Mind", "7.1K", TideTunesBrand.SupportGreen, TideTunesBrand.SupportBlue, "FM"),
)
