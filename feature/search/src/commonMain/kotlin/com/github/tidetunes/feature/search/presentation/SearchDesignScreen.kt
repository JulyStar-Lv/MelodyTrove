package com.github.tidetunes.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.github.tidetunes.feature.search.domain.SearchAlbumItem
import com.github.tidetunes.feature.search.domain.SearchArtistItem
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.search.generated.resources.Res
import tidetunes.feature.search.generated.resources.icon_music_note
import tidetunes.feature.search.generated.resources.icon_search
import tidetunes.feature.search.generated.resources.search_hint
import tidetunes.feature.search.generated.resources.search_connection_retry
import tidetunes.feature.search.generated.resources.search_no_matches_yet
import tidetunes.feature.search.generated.resources.search_recent_searches
import tidetunes.feature.search.generated.resources.search_sources_unavailable
import tidetunes.feature.search.generated.resources.search_suggestions
import tidetunes.feature.search.generated.resources.search_title
import tidetunes.feature.search.generated.resources.search_try_suggestion
import tidetunes.feature.search.generated.resources.search_try_query
import tidetunes.feature.search.generated.resources.searching_library
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_timelapse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchDesignScreen(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        SearchResultsSummary(trackCount = state.tracks.size, albumCount = state.albums.size, artistCount = state.artists.size)
                    }
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
                    if (state.albums.isNotEmpty()) {
                        item {
                            SearchResultSectionHeader("Albums", state.albums.size)
                        }
                        items(
                            items = state.albums,
                            key = { album -> "search-album-${album.id}" },
                        ) { album ->
                            SearchAlbumResultRow(
                                album = album,
                                onClick = { onAction(SearchAction.OpenAlbum(album)) },
                            )
                        }
                    }
                    if (state.artists.isNotEmpty()) {
                        item {
                            SearchResultSectionHeader("Artists", state.artists.size)
                        }
                        items(
                            items = state.artists,
                            key = { artist -> "search-artist-${artist.id}" },
                        ) { artist ->
                            SearchArtistResultRow(
                                artist = artist,
                                onClick = { onAction(SearchAction.OpenArtist(artist)) },
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
    val history = state.history.take(8)
    val suggestions = state.suggestions
        .filterNot { suggestion -> history.any { it.equals(suggestion, ignoreCase = true) } }
        .take(8)

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (history.isNotEmpty()) {
            SearchQueryChips(
                title = stringResource(Res.string.search_recent_searches),
                searches = history,
                onSelect = { onAction(SearchAction.SelectSuggestion(it)) },
                onClear = { onAction(SearchAction.ClearHistory) },
            )
        }
        if (suggestions.isNotEmpty()) {
            SearchQueryChips(
                title = stringResource(Res.string.search_suggestions),
                searches = suggestions,
                onSelect = { onAction(SearchAction.SelectSuggestion(it)) },
            )
        }
        if (history.isEmpty() && suggestions.isEmpty()) {
            SearchStatus(
                title = stringResource(Res.string.search_title),
                message = stringResource(Res.string.search_try_suggestion),
            )
        }
    }
}

@Composable
private fun SearchQueryChips(
    title: String,
    searches: List<String>,
    onSelect: (String) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    if (searches.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
            )
            if (onClear != null) {
                Text(
                    text = "Clear",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(searches, key = { it }) { label ->
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
            .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget)
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
private fun SearchResultsSummary(
    trackCount: Int,
    albumCount: Int = 0,
    artistCount: Int = 0,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Search Results",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
        )
        val parts = buildList {
            if (trackCount > 0) add("$trackCount ${if (trackCount == 1) "song" else "songs"}")
            if (albumCount > 0) add("$albumCount ${if (albumCount == 1) "album" else "albums"}")
            if (artistCount > 0) add("$artistCount ${if (artistCount == 1) "artist" else "artists"}")
        }
        Text(
            text = parts.joinToString(" · ").ifEmpty { "No matches" },
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
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
                            searchTrackGradients[(rank - 1) % searchTrackGradients.size].first,
                            searchTrackGradients[(rank - 1) % searchTrackGradients.size].second,
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

private fun SearchAlbumResultRow(
    album: SearchAlbumItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(TideTunesBrand.SupportBlue, TideTunesBrand.SupportGreen),
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
            Text(
                text = album.name,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (album.artist != null) {
                Text(
                    text = album.artist,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "Album",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun SearchArtistResultRow(
    artist: SearchArtistItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(TideTunesBrand.SupportOrange, TideTunesBrand.Primary),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                color = Color.White,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = artist.name,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "Artist",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
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
                        .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAction)
                        .padding(8.dp),
                )
            }
        }
    }
}

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

private val searchTrackGradients = listOf(
    TideTunesBrand.Primary to TideTunesBrand.Secondary,
    TideTunesBrand.Secondary to TideTunesBrand.SupportBlue,
    TideTunesBrand.SupportOrange to TideTunesBrand.Primary,
    TideTunesBrand.SupportGreen to TideTunesBrand.SupportBlue,
)
