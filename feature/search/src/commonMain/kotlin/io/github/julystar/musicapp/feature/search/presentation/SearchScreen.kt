package io.github.julystar.musicapp.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.presentation.components.DesignCardSurface
import io.github.julystar.musicapp.core.presentation.components.DesignChipSection
import io.github.julystar.musicapp.core.presentation.components.DesignIconBadge
import io.github.julystar.musicapp.core.presentation.components.DesignIconBadgeVariant
import io.github.julystar.musicapp.core.presentation.components.DesignLoadingIndicator
import io.github.julystar.musicapp.core.presentation.components.DesignPageHeader
import io.github.julystar.musicapp.core.presentation.components.DesignSearchBar
import io.github.julystar.musicapp.core.presentation.components.DesignStatusBadge
import io.github.julystar.musicapp.core.presentation.components.DesignStatusTone
import io.github.julystar.musicapp.core.presentation.components.DesignSourceBadge
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import io.github.julystar.musicapp.feature.search.domain.SearchTrackItem
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import musicapp.feature.search.generated.resources.Res
import musicapp.feature.search.generated.resources.downloads_title
import musicapp.feature.search.generated.resources.icon_download
import musicapp.feature.search.generated.resources.icon_music_note
import musicapp.feature.search.generated.resources.icon_search
import musicapp.feature.search.generated.resources.search_empty
import musicapp.feature.search.generated.resources.search_hint
import musicapp.feature.search.generated.resources.search_connection_retry
import musicapp.feature.search.generated.resources.search_recent_searches
import musicapp.feature.search.generated.resources.search_remote_failures
import musicapp.feature.search.generated.resources.search_suggestions
import musicapp.feature.search.generated.resources.search_title
import musicapp.feature.search.generated.resources.search_try_suggestion
import musicapp.feature.search.generated.resources.searching
import musicapp.feature.search.generated.resources.downloads_retry
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchScreen(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = DesignTokens.spacing

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = spacing.pageCompact
        val showPageHeader = maxWidth < 1024.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .padding(
                start = horizontalPadding,
                top = 8.dp,
                end = horizontalPadding,
                bottom = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (showPageHeader) {
            DesignPageHeader(
                title = stringResource(Res.string.search_title),
                subtitle = null,
            )
        }
        SearchInput(
            state = state,
            onAction = onAction,
        )
        if (state.failedSourceCount > 0) {
            DesignStatusBadge(
                label = "${state.failedSourceCount} ${stringResource(Res.string.search_remote_failures)}",
                tone = DesignStatusTone.Error,
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
    DesignSearchBar(
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
        contentPadding = PaddingValues(bottom = DesignTokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (state.loadState) {
            SearchLoadState.Searching -> {
                item {
                    SearchStatusCard(
                        title = stringResource(Res.string.searching),
                        message = state.query.ifBlank { stringResource(Res.string.search_hint) },
                        loading = true,
                    )
                }
            }
            SearchLoadState.Error -> {
                item {
                    SearchStatusCard(
                        title = stringResource(Res.string.search_remote_failures),
                        message = stringResource(Res.string.search_connection_retry),
                        actionText = stringResource(Res.string.downloads_retry),
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
                        message = stringResource(Res.string.search_try_suggestion),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
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
    DesignCardSurface(
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesignIconBadge(
                variant = DesignIconBadgeVariant.Surface,
            ) {
                if (loading) {
                    DesignLoadingIndicator(size = 24.dp)
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
                DesignTextButton(
                    text = actionText,
                    variant = DesignTextButtonVariant.Primary,
                    size = DesignTextButtonSize.Small,
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
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.spacing.lg)) {
        DesignChipSection(
            title = stringResource(Res.string.search_recent_searches),
            labels = state.history.take(8).ifEmpty { state.trendingQueries().take(5) },
            trailing = if (state.history.isNotEmpty()) {
                {
                    DesignTextButton(
                        text = "Clear",
                        variant = DesignTextButtonVariant.Default,
                        size = DesignTextButtonSize.Small,
                        onClick = { onAction(SearchAction.ClearHistory) },
                    )
                }
            } else {
                null
            },
            chipLeading = {
                Icon(
                    painter = painterResource(Res.drawable.icon_search),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            },
            onLabelClick = { query -> onAction(SearchAction.SelectSuggestion(query)) },
        )
        SearchCategoryGrid()
        SearchTrendingNow(onAction = onAction)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SearchCategoryGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Browse Genres",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.subtitle,
            fontWeight = FontWeight.SemiBold,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 12.dp) / 2
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                searchCategories.forEach { category ->
                    SearchCategoryCard(
                        category = category,
                        modifier = Modifier.width(cardWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchCategoryCard(
    category: SearchCategoryUi,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(category.tint, category.endTint),
                ),
            )
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = category.label,
            color = Color.White,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchTrackCard(
    track: SearchTrackItem,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(DesignPalette.Primary, DesignPalette.Secondary),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_music_note),
                    tint = Color.White.copy(alpha = 0.82f),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
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
                Row(
                    modifier = Modifier.widthIn(max = 148.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (track.sourceLabel != "Library") {
                        DesignSourceBadge(
                            label = track.sourceLabel,
                            modifier = Modifier.widthIn(max = 92.dp),
                        )
                    }
                    Text(
                        text = track.durationMs.durationText(),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                    )
                    if (track.mediaId != null) {
                        Icon(
                            painter = painterResource(Res.drawable.icon_download),
                            tint = MiuixTheme.colorScheme.primary,
                            contentDescription = stringResource(Res.string.downloads_title),
                            modifier = Modifier
                                .clip(RoundedCornerShape(DesignTokens.shapes.full))
                                .clickable(onClick = onDownload)
                                .padding(4.dp)
                                .size(16.dp),
                        )
                    }
                }
            }
        }
}

@Composable
private fun SearchTrendingNow(
    onAction: (SearchAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Trending Now",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.subtitle,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        demoTrendingTracks.forEach { track ->
            SearchTrackCard(
                track = track,
                onClick = { onAction(SearchAction.OpenTrack(track)) },
                onDownload = { onAction(SearchAction.DownloadTrack(track)) },
            )
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
    val tint: Color,
    val endTint: Color,
)

private val fallbackTrendingQueries = listOf(
    "Luna Waves",
    "Synthwave",
    "Midnight Cascade",
    "Hi-Res",
    "Ambient",
)

private val searchCategories = listOf(
    SearchCategoryUi(
        label = "Electronic",
        tint = DesignPalette.Primary,
        endTint = DesignPalette.Secondary,
    ),
    SearchCategoryUi(
        label = "Ambient",
        tint = DesignPalette.Secondary,
        endTint = DesignPalette.SupportBlue,
    ),
    SearchCategoryUi(
        label = "Synthwave",
        tint = DesignPalette.SupportOrange,
        endTint = DesignPalette.Primary,
    ),
    SearchCategoryUi(
        label = "Techno",
        tint = DesignPalette.SupportGreen,
        endTint = DesignPalette.SupportBlue,
    ),
    SearchCategoryUi(
        label = "IDM",
        tint = DesignPalette.SupportYellow,
        endTint = DesignPalette.SupportOrange,
    ),
    SearchCategoryUi(
        label = "Post-Rock",
        tint = DesignPalette.SupportBlue,
        endTint = DesignPalette.Secondary,
    ),
    SearchCategoryUi(
        label = "Shoegaze",
        tint = DesignPalette.Primary,
        endTint = DesignPalette.SupportOrange,
    ),
    SearchCategoryUi(
        label = "Experimental",
        tint = DesignPalette.Secondary,
        endTint = DesignPalette.SupportGreen,
    ),
    SearchCategoryUi(
        label = "Jazz",
        tint = DesignPalette.Primary,
        endTint = DesignPalette.Secondary,
    ),
    SearchCategoryUi(
        label = "Classical",
        tint = DesignPalette.SupportBlue,
        endTint = DesignPalette.SupportBlue,
    ),
)

private val demoTrendingTracks = listOf(
    SearchTrackItem(id = 1, title = "Midnight Cascade", artist = "Luna Waves · Tidal Drift", durationMs = 222_000, sourceLabel = "Hi-Res"),
    SearchTrackItem(id = 2, title = "Neon Undertow", artist = "Prism Circuit · Voltage Dreams", durationMs = 258_000, sourceLabel = "Lossless"),
    SearchTrackItem(id = 3, title = "Silver Tide", artist = "Coastal Drift · Open Water", durationMs = 235_000, sourceLabel = "Library"),
    SearchTrackItem(id = 4, title = "Aurora Sequence", artist = "Polar Echo · Northern Lights", durationMs = 302_000, sourceLabel = "Dolby Atmos"),
    SearchTrackItem(id = 5, title = "Depth Protocol", artist = "Ocean Syntax · Subsonic", durationMs = 210_000, sourceLabel = "Library"),
    SearchTrackItem(id = 6, title = "Glass Architecture", artist = "Fractal Mind · Prism", durationMs = 284_000, sourceLabel = "Lossless"),
    SearchTrackItem(id = 7, title = "Resonance Fields", artist = "Wave Function · Quantum", durationMs = 195_000, sourceLabel = "Library"),
    SearchTrackItem(id = 8, title = "Liminal Space", artist = "Threshold · Between", durationMs = 330_000, sourceLabel = "Hi-Res"),
)
