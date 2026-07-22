package com.github.tidetunes.feature.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.presentation.components.QualityBadge
import com.github.tidetunes.core.presentation.components.QualityBadgeType
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideGlassScene
import com.github.tidetunes.core.presentation.components.TideSearchBar
import com.github.tidetunes.core.presentation.components.TideStickyGlassActionBar
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesFontFamilies
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_album
import tidetunes.core.presentation.generated.resources.icon_cloud
import tidetunes.core.presentation.generated.resources.icon_download
import tidetunes.core.presentation.generated.resources.icon_folder
import tidetunes.core.presentation.generated.resources.icon_dashboard
import tidetunes.core.presentation.generated.resources.icon_log
import tidetunes.core.presentation.generated.resources.icon_music_note
import tidetunes.core.presentation.generated.resources.icon_pause
import tidetunes.core.presentation.generated.resources.icon_play
import tidetunes.core.presentation.generated.resources.icon_plus
import tidetunes.core.presentation.generated.resources.icon_collapse
import tidetunes.core.presentation.generated.resources.icon_chevron_right
import tidetunes.feature.home.generated.resources.Res as HomeRes
import tidetunes.feature.home.generated.resources.home_cover_1
import tidetunes.feature.home.generated.resources.home_cover_2
import tidetunes.feature.home.generated.resources.home_cover_3
import tidetunes.feature.home.generated.resources.home_cover_4
import tidetunes.feature.home.generated.resources.home_cover_5
import tidetunes.feature.home.generated.resources.home_cover_6
import tidetunes.feature.home.generated.resources.home_cover_8
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LibraryDesignScreen(
    state: LibraryState,
    currentPlayingTrackId: Long? = null,
    onNavigateToLibraryFolderImport: () -> Unit = {},
    onAction: (LibraryAction) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(LibraryDesignCategory.Playlists) }
    var moreExpanded by remember { mutableStateOf(false) }
    var songQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(LibrarySortBy.Title) }
    var creatingPlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }
    var activeArtistLetter by remember { mutableStateOf<String?>(null) }

    TideGlassScene(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
        val compact = maxWidth < 1024.dp
        val pagePadding = if (compact) TideTunesTokens.spacing.pageCompact else TideTunesTokens.spacing.pageExpanded
        val isDesktop = maxWidth >= 1024.dp
        val mobileListState = rememberLazyListState()
        val collapseDistance = with(LocalDensity.current) { 88.dp.roundToPx() }
        val actionBarProgress by remember(mobileListState, collapseDistance) {
            derivedStateOf {
                if (mobileListState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    (mobileListState.firstVisibleItemScrollOffset / collapseDistance.toFloat())
                        .coerceIn(0f, 1f)
                }
            }
        }

        if (isDesktop) {
            Row(modifier = Modifier.fillMaxSize()) {
                LibrarySidebar(
                    selected = selectedCategory,
                    onSelect = {
                        selectedCategory = it
                        moreExpanded = false
                    },
                )
                Box(modifier = Modifier.weight(1f)) {
                    LibraryContent(
                        state = state,
                        selectedCategory = selectedCategory,
                        currentPlayingTrackId = currentPlayingTrackId,
                        onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                        onAction = onAction,
                        onSelectCategory = { selectedCategory = it },
                        songQuery = songQuery,
                        onSongQueryChange = { songQuery = it },
                        sortBy = sortBy,
                        onSortByChange = { sortBy = it },
                        activeArtistLetter = activeArtistLetter,
                        onArtistLetterChange = { activeArtistLetter = it },
                        compact = false,
                        pagePadding = pagePadding,
                    )
                }
            }
        } else {
            LazyColumn(
                state = mobileListState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = TideTunesTokens.adaptive.contentMaxWidth),
                contentPadding = PaddingValues(
                    start = pagePadding,
                    top = 8.dp,
                    end = pagePadding,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    TidePageHeader(
                        title = "Library",
                        subtitle = null,
                    )
                }
                item {
                    LibraryMobileTabs(
                        selected = selectedCategory,
                        moreExpanded = moreExpanded,
                        onSelect = {
                            selectedCategory = it
                            moreExpanded = false
                        },
                        onToggleMore = { moreExpanded = !moreExpanded },
                    )
                }
                if (moreExpanded || selectedCategory !in primaryLibraryCategories) {
                    item {
                        LibraryMorePanel(
                            selected = selectedCategory,
                            onSelect = {
                                selectedCategory = it
                                moreExpanded = false
                            },
                        )
                    }
                }
                // Category content
                LibraryCategoryItems(
                    state = state,
                    selectedCategory = selectedCategory,
                    currentPlayingTrackId = currentPlayingTrackId,
                    onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                    onAction = onAction,
                    onSelectCategory = { selectedCategory = it },
                    songQuery = songQuery,
                    onSongQueryChange = { songQuery = it },
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it },
                    activeArtistLetter = activeArtistLetter,
                    onArtistLetterChange = { activeArtistLetter = it },
                )
            }
            TideStickyGlassActionBar(
                title = "Library",
                collapseFraction = actionBarProgress,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        }
    }
}

@Composable
private fun LibrarySidebar(
    selected: LibraryDesignCategory,
    onSelect: (LibraryDesignCategory) -> Unit,
) {
    val dividerColor = MiuixTheme.colorScheme.outline
    Column(
        modifier = Modifier
            .width(196.dp)
            .fillMaxHeight()
            .background(MiuixTheme.colorScheme.surface)
            .drawBehind {
                val dividerWidth = 1.dp.toPx()
                val x = size.width - dividerWidth / 2f
                drawLine(
                    color = dividerColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = dividerWidth,
                )
            }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        librarySidebarGroups.forEach { group ->
            Text(
                text = group.label,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            group.categories.forEach { category ->
                val isSelected = selected == category
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MiuixTheme.colorScheme.tertiaryContainer
                            else Color.Transparent,
                        )
                        .clickable { onSelect(category) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(category.icon),
                        contentDescription = null,
                        tint = if (isSelected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = category.label,
                        color = if (isSelected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class SidebarGroup(
    val label: String,
    val categories: List<LibraryDesignCategory>,
)

private val librarySidebarGroups = listOf(
    SidebarGroup("Collection", listOf(
        LibraryDesignCategory.Playlists,
        LibraryDesignCategory.Songs,
        LibraryDesignCategory.Albums,
        LibraryDesignCategory.Artists,
        LibraryDesignCategory.Genres,
    )),
    SidebarGroup("Storage", listOf(
        LibraryDesignCategory.Folders,
    )),
    SidebarGroup("More", listOf(
        LibraryDesignCategory.Favorites,
        LibraryDesignCategory.Downloads,
        LibraryDesignCategory.History,
        LibraryDesignCategory.RecentlyAdded,
        LibraryDesignCategory.RecentlyPlayed,
        LibraryDesignCategory.Lossless,
        LibraryDesignCategory.HiRes,
        LibraryDesignCategory.Sources,
    )),
)

@Composable
private fun LibraryMobileTabs(
    selected: LibraryDesignCategory,
    moreExpanded: Boolean,
    onSelect: (LibraryDesignCategory) -> Unit,
    onToggleMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.lg))
            .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(TideTunesTokens.shapes.lg))
            .background(MiuixTheme.colorScheme.surface)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        primaryLibraryCategories.forEach { category ->
            val isSelected = selected == category
            Text(
                text = category.label,
                color = if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                    .background(if (isSelected) MiuixTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(category) },
            )
        }
        val moreActive = moreExpanded || selected !in primaryLibraryCategories
        Row(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                .background(if (moreActive) MiuixTheme.colorScheme.tertiaryContainer else Color.Transparent)
                .clickable(onClick = onToggleMore),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "More",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                painter = painterResource(CoreRes.drawable.icon_collapse),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

@Composable
private fun LibraryMorePanel(
    selected: LibraryDesignCategory,
    onSelect: (LibraryDesignCategory) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        morePanelGroups.forEach { group ->
            Text(
                text = group.label,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                group.categories.forEach { category ->
                    val isSelected = selected == category
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent,
                            )
                            .clickable { onSelect(category) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(category.icon),
                            contentDescription = null,
                            tint = if (isSelected) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = category.label,
                            color = if (isSelected) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.footnote2,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private val morePanelGroups = listOf(
    SidebarGroup("Browse", listOf(LibraryDesignCategory.Genres)),
    SidebarGroup("Storage", listOf(LibraryDesignCategory.Folders)),
    SidebarGroup("More", listOf(
        LibraryDesignCategory.Favorites,
        LibraryDesignCategory.Downloads,
    )),
)

@Composable
private fun LibraryContent(
    state: LibraryState,
    selectedCategory: LibraryDesignCategory,
    currentPlayingTrackId: Long?,
    onNavigateToLibraryFolderImport: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    onSelectCategory: (LibraryDesignCategory) -> Unit,
    songQuery: String,
    onSongQueryChange: (String) -> Unit,
    sortBy: LibrarySortBy,
    onSortByChange: (LibrarySortBy) -> Unit,
    activeArtistLetter: String?,
    onArtistLetterChange: (String?) -> Unit,
    compact: Boolean,
    pagePadding: Dp,
) {
    Box(modifier = Modifier.fillMaxSize()) {
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp)
                .padding(horizontal = pagePadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!compact) {
                item {
                    TidePageHeader(title = "Library", subtitle = null)
                }
            }

            LibraryCategoryItems(
                state = state,
                selectedCategory = selectedCategory,
                currentPlayingTrackId = currentPlayingTrackId,
                onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                onAction = onAction,
                onSelectCategory = onSelectCategory,
                songQuery = songQuery,
                onSongQueryChange = onSongQueryChange,
                sortBy = sortBy,
                onSortByChange = onSortByChange,
                activeArtistLetter = activeArtistLetter,
                onArtistLetterChange = onArtistLetterChange,
            )
        }
        TideStickyGlassActionBar(
            title = "Library",
            collapseFraction = actionBarProgress,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private fun LazyListScope.LibraryCategoryItems(
    state: LibraryState,
    selectedCategory: LibraryDesignCategory,
    currentPlayingTrackId: Long?,
    onNavigateToLibraryFolderImport: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    onSelectCategory: (LibraryDesignCategory) -> Unit,
    songQuery: String,
    onSongQueryChange: (String) -> Unit,
    sortBy: LibrarySortBy,
    onSortByChange: (LibrarySortBy) -> Unit,
    activeArtistLetter: String?,
    onArtistLetterChange: (String?) -> Unit,
) = with(state) {
    val tracksForCategory = getTracksForCategory(selectedCategory, tracks)
    val filteredTracks = tracksForCategory
        .filter { track ->
            songQuery.isBlank() || listOfNotNull(track.title, track.artist)
                .any { it.contains(songQuery, ignoreCase = true) }
        }
        .sortedWith(sortBy.comparator)

    val isSongTab = selectedCategory in songLibraryCategories

    // Category header with actions
    item {
        CategorySectionHeader(
            title = selectedCategory.label,
            metadata = libraryMetadata(selectedCategory, state),
            showShuffle = isSongTab && filteredTracks.isNotEmpty(),
            showPlayAll = isSongTab && filteredTracks.isNotEmpty(),
            showNewPlaylist = selectedCategory == LibraryDesignCategory.Playlists,
            onShuffle = {
                if (filteredTracks.isNotEmpty()) {
                    onAction(LibraryAction.PlayTrack(filteredTracks.first().id))
                }
            },
            onPlayAll = {
                if (filteredTracks.isNotEmpty()) {
                    onAction(LibraryAction.PlayTrack(filteredTracks.first().id))
                }
            },
            onNewPlaylist = { onSelectCategory(LibraryDesignCategory.Playlists) },
        )
    }

    // Search + Sort for song tabs
    if (isSongTab && tracksForCategory.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TideSearchBar(
                        value = songQuery,
                        onValueChange = onSongQueryChange,
                        placeholder = "Search ${selectedCategory.label.lowercase()}",
                        onSearch = {},
                        onClear = { onSongQueryChange("") },
                    )
                }
                SortDropdown(
                    current = sortBy,
                    onChange = onSortByChange,
                )
            }
        }
    }

    when (selectedCategory) {
        LibraryDesignCategory.Songs,
        LibraryDesignCategory.Favorites,
        LibraryDesignCategory.History,
        LibraryDesignCategory.RecentlyPlayed,
        LibraryDesignCategory.RecentlyAdded,
        LibraryDesignCategory.Lossless,
        LibraryDesignCategory.HiRes -> {
            if (filteredTracks.isEmpty()) {
                item {
                    TideCardSurface(contentPadding = PaddingValues(24.dp)) {
                        LibraryEmptyContent(
                            title = if (songQuery.isNotBlank()) "No matches"
                            else "No tracks",
                            message = if (songQuery.isNotBlank()) "Try a different search."
                            else selectedCategory.emptyMessage,
                            action = if (songQuery.isNotBlank()) "Clear search" to { onSongQueryChange("") }
                            else null,
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredTracks,
                    key = { _, track -> track.id.takeIf { it > 0L } ?: track.hashCode() },
                ) { index, track ->
                    LibrarySongRow(
                        track = track,
                        rank = if (selectedCategory == LibraryDesignCategory.Songs) index + 1 else null,
                        playing = track.id == currentPlayingTrackId,
                        onPlay = { onAction(LibraryAction.PlayTrack(track.id)) },
                        onDownload = { onAction(LibraryAction.DownloadTrack(track)) },
                    )
                }
            }
        }

        LibraryDesignCategory.Albums -> {
            if (albums.isEmpty()) {
                item {
                    TideCardSurface(contentPadding = PaddingValues(24.dp)) {
                        LibraryEmptyContent(
                            title = "No albums",
                            message = selectedCategory.emptyMessage,
                        )
                    }
                }
            } else {
                item {
                    LibraryAlbumGrid(albums = albums.take(24))
                }
            }
        }

        LibraryDesignCategory.Artists -> {
            if (artists.isEmpty()) {
                item {
                    TideCardSurface(contentPadding = PaddingValues(24.dp)) {
                        LibraryEmptyContent(
                            title = "No artists",
                            message = selectedCategory.emptyMessage,
                        )
                    }
                }
            } else {
                item {
                    LibraryArtistGrouped(
                        artists = artists,
                        activeLetter = activeArtistLetter,
                        onLetterChange = onArtistLetterChange,
                    )
                }
            }
        }

        LibraryDesignCategory.Genres -> item {
            GenreGridView()
        }

        LibraryDesignCategory.Folders -> item {
            FolderListView(onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport)
        }

        LibraryDesignCategory.Playlists -> item {
            PlaylistListView(onNewPlaylist = { onSelectCategory(LibraryDesignCategory.Playlists) })
        }

        LibraryDesignCategory.Downloads -> item {
            TideCardSurface(contentPadding = PaddingValues(24.dp)) {
                LibraryEmptyContent(
                    title = "No downloads yet",
                    message = "Keep music available when you are offline.",
                    action = "Browse songs" to { onSelectCategory(LibraryDesignCategory.Songs) },
                    painter = painterResource(CoreRes.drawable.icon_download),
                )
            }
        }

        LibraryDesignCategory.Sources -> item {
            TideCardSurface(contentPadding = PaddingValues(24.dp)) {
                LibraryEmptyContent(
                    title = "One library, every source",
                    message = "Manage Local and WebDAV sources from Settings.",
                    painter = painterResource(CoreRes.drawable.icon_cloud),
                )
            }
        }
    }
}

// ── Category Section Header ──

@Composable
private fun CategorySectionHeader(
    title: String,
    metadata: String,
    showShuffle: Boolean,
    showPlayAll: Boolean,
    showNewPlaylist: Boolean,
    onShuffle: () -> Unit,
    onPlayAll: () -> Unit,
    onNewPlaylist: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = metadata,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showShuffle) {
                ShuffleButton(onClick = onShuffle)
            }
            if (showPlayAll) {
                PlayAllButton(onClick = onPlayAll)
            }
            if (showNewPlaylist) {
                NewPlaylistButton(onClick = onNewPlaylist)
            }
        }
    }
}

@Composable
private fun ShuffleButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_dashboard),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = "Shuffle",
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PlayAllButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(MiuixTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_play),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Play all",
            color = Color.White,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NewPlaylistButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(MiuixTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_plus),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "New",
            color = Color.White,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Sort Dropdown ──

@Composable
private fun SortDropdown(
    current: LibrarySortBy,
    onChange: (LibrarySortBy) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(CoreRes.drawable.icon_collapse),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = current.label,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(top = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            ) {
                LibrarySortBy.entries.forEach { sort ->
                    Text(
                        text = sort.label,
                        color = if (sort == current) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable {
                                onChange(sort)
                                expanded = false
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

// ── Song Row ──

@Composable
private fun LibrarySongRow(
    track: LibraryTrackItem,
    rank: Int?,
    playing: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (playing) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent,
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                style = MiuixTheme.textStyles.footnote2.copy(fontFamily = TideTunesFontFamilies.Mono),
                modifier = Modifier.width(20.dp),
            )
        }
        LibraryArtwork(index = rank ?: 0, size = 40.dp, playing = playing)
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
                    color = if (playing) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (track.qualityBadgeType() != null) {
                    QualityBadge(type = track.qualityBadgeType()!!)
                }
            }
            Text(
                text = track.artist ?: "Unknown Artist",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDuration(track.durationMs),
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            style = MiuixTheme.textStyles.footnote2.copy(fontFamily = TideTunesFontFamilies.Mono),
        )
    }
}

// ── Album Grid ──

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryAlbumGrid(albums: List<LibraryAlbumItem>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 800.dp -> 4
            maxWidth >= 500.dp -> 3
            else -> 2
        }
        val gap = 16.dp
        val width = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            maxItemsInEachRow = columns,
        ) {
            albums.forEachIndexed { index, album ->
                AlbumCard(album, index, width)
            }
        }
    }
}

@Composable
private fun AlbumCard(album: LibraryAlbumItem, index: Int, width: Dp) {
    Column(modifier = Modifier.width(width)) {
        Box(
            modifier = Modifier
                .size(width)
                .shadow(TideTunesTokens.elevation.card, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(libraryArtworkBrush(index)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = album.name.take(2).uppercase(),
                color = Color.White.copy(alpha = 0.5f),
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.year?.toString() ?: "Unknown",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

// ── Artist Grouped ──

@Composable
private fun LibraryArtistGrouped(
    artists: List<LibraryArtistItem>,
    activeLetter: String?,
    onLetterChange: (String?) -> Unit,
) {
    val grouped = remember(artists) {
        artists
            .sortedBy { it.name }
            .groupBy { it.name.first().uppercaseChar().toString() }
            .entries
            .sortedBy { it.key }
    }
    val availableLetters = grouped.map { it.key }.toSet()
    val selectedLetter = activeLetter ?: grouped.firstOrNull()?.key

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            grouped.forEach { (letter, group) ->
                Column {
                    Text(
                        text = letter,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(horizontal = 8.dp),
                    )
                    group.forEach { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { /* navigate */ }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(libraryArtworkBrush(artist.name.hashCode())),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = artistInitials(artist.name),
                                    color = Color.White.copy(alpha = 0.9f),
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
                                Text(
                                    text = "Artist",
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                    style = MiuixTheme.textStyles.footnote1,
                                )
                            }
                            Icon(
                                painter = painterResource(CoreRes.drawable.icon_chevron_right),
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        // Alphabet index
        if (grouped.size > 1) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                    .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
                    .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(TideTunesTokens.shapes.full))
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ".forEach { letter ->
                    val letterStr = letter.toString()
                    val available = letterStr in availableLetters
                    val isSelected = letterStr == selectedLetter
                    Text(
                        text = letterStr,
                        color = when {
                            !available -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.25f)
                            isSelected -> MiuixTheme.colorScheme.primary
                            else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent,
                            )
                            .then(
                                if (available) Modifier.clickable { onLetterChange(letterStr) }
                                else Modifier
                            ),
                    )
                }
            }
        }
    }
}

// ── Genre Grid ──

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GenreGridView() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 600.dp -> 3
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
            libraryGenres.forEachIndexed { index, genre ->
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(genre.colorStart, genre.colorEnd),
                            ),
                        )
                        .padding(16.dp),
                ) {
                    Text(
                        text = "${genre.albumCount} albums",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                    Text(
                        text = genre.name,
                        color = Color.White,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
        }
    }
}

// ── Folder List ──

@Composable
private fun FolderListView(onNavigateToLibraryFolderImport: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        demoFolders.forEachIndexed { index, folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToLibraryFolderImport)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(CoreRes.drawable.icon_folder),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.split("/").last(),
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = folder,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Folder",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote2,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            if (index < demoFolders.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

// ── Playlist List ──

@Composable
private fun PlaylistListView(onNewPlaylist: () -> Unit) {
    Column {
        designPlaylists.forEachIndexed { index, playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNewPlaylist)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(playlist.cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = playlist.description,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${playlist.trackCount} tracks",
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = playlist.duration,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote2,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(CoreRes.drawable.icon_play),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            if (index < designPlaylists.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 76.dp, end = 8.dp)
                        .height(1.dp)
                        .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.3f)),
                )
            }
        }
    }
}

// ── Empty Content ──

@Composable
private fun LibraryEmptyContent(
    title: String,
    message: String,
    action: Pair<String, () -> Unit>? = null,
    painter: Painter? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (painter != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Text(
                text = action.first,
                color = Color.White,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                    .background(
                        Brush.linearGradient(
                            listOf(TideTunesBrand.Primary, TideTunesBrand.Secondary),
                        ),
                    )
                    .clickable(onClick = action.second)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

// ── Artwork ──

@Composable
private fun LibraryArtwork(index: Int, size: Dp, playing: Boolean) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(libraryArtworkBrush(index)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                if (playing) CoreRes.drawable.icon_pause else CoreRes.drawable.icon_music_note,
            ),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(size * 0.4f),
        )
    }
}

// ── Metadata ──

private fun libraryMetadata(category: LibraryDesignCategory, state: LibraryState): String {
    val tracks = state.tracks
    return when (category) {
        LibraryDesignCategory.Songs -> {
            val totalMin = tracks.sumOf { (it.durationMs ?: 0L).coerceAtLeast(0L) } / 60_000L
            "${tracks.size} songs · ~$totalMin min"
        }
        LibraryDesignCategory.Albums -> "${state.albums.size} albums"
        LibraryDesignCategory.Artists -> "${state.artists.size} artists"
        LibraryDesignCategory.Genres -> "${libraryGenres.size} genres"
        LibraryDesignCategory.Folders -> "${demoFolders.size} folders"
        LibraryDesignCategory.Playlists -> "${designPlaylists.size} playlists"
        LibraryDesignCategory.Downloads -> "Available offline"
        else -> {
            val catTracks = getCategoryTracks(category, tracks)
            "${catTracks.size} songs"
        }
    }
}

private fun getTracksForCategory(category: LibraryDesignCategory, tracks: List<LibraryTrackItem>): List<LibraryTrackItem> {
    return when (category) {
        LibraryDesignCategory.Favorites -> tracks.take(6)
        LibraryDesignCategory.History -> tracks.reversed()
        LibraryDesignCategory.RecentlyAdded,
        LibraryDesignCategory.RecentlyPlayed -> tracks.take(12)
        LibraryDesignCategory.Lossless -> tracks.take(8)
        LibraryDesignCategory.HiRes -> tracks.take(5)
        else -> tracks
    }
}

private fun getCategoryTracks(category: LibraryDesignCategory, tracks: List<LibraryTrackItem>): List<LibraryTrackItem> {
    return tracks
}

// ── Utilities ──

private fun libraryArtworkBrush(index: Int): Brush {
    val safeIdx = index.mod(libraryArtworkColors.size)
    val first = libraryArtworkColors[safeIdx]
    val second = libraryArtworkColors[(safeIdx + 1).mod(libraryArtworkColors.size)]
    return Brush.linearGradient(listOf(first, second))
}

private fun formatDuration(durationMs: Long?): String {
    val totalSeconds = ((durationMs ?: 0L) / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun artistInitials(name: String): String = name
    .split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1).uppercase() }
    .ifBlank { "?" }

private fun LibraryTrackItem.qualityBadgeType(): QualityBadgeType? = null

// ── Sort By ──

private enum class LibrarySortBy(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Album("Album");

    val comparator: Comparator<LibraryTrackItem>
        get() = when (this) {
            Title -> compareBy { it.title }
            Artist -> compareBy { it.artist ?: "" }
            Album -> compareBy { it.title }
        }
}

// ── Constants ──

private enum class LibraryDesignCategory(
    val label: String,
    val icon: DrawableResource,
    val emptyMessage: String = "No music is available in this collection yet.",
) {
    Playlists("Playlists", CoreRes.drawable.icon_music_note),
    Songs("Songs", CoreRes.drawable.icon_music_note),
    Albums("Albums", CoreRes.drawable.icon_album),
    Artists("Artists", CoreRes.drawable.icon_music_note),
    Genres("Genres", CoreRes.drawable.icon_album),
    Folders("Folders", CoreRes.drawable.icon_folder),
    Favorites("Favorites", CoreRes.drawable.icon_music_note, "Favorite songs will appear here."),
    Downloads("Downloads", CoreRes.drawable.icon_download),
    History("History", CoreRes.drawable.icon_log),
    RecentlyAdded("Recently Added", CoreRes.drawable.icon_album),
    RecentlyPlayed("Recently Played", CoreRes.drawable.icon_log),
    Lossless("Lossless", CoreRes.drawable.icon_music_note, "Lossless tracks appear after scan."),
    HiRes("Hi-Res", CoreRes.drawable.icon_music_note, "Hi-Res tracks appear after scan."),
    Sources("Sources", CoreRes.drawable.icon_cloud),
}

private val primaryLibraryCategories = listOf(
    LibraryDesignCategory.Playlists,
    LibraryDesignCategory.Songs,
    LibraryDesignCategory.Albums,
    LibraryDesignCategory.Artists,
)

private val songLibraryCategories = setOf(
    LibraryDesignCategory.Songs,
    LibraryDesignCategory.Favorites,
    LibraryDesignCategory.History,
    LibraryDesignCategory.RecentlyAdded,
    LibraryDesignCategory.RecentlyPlayed,
    LibraryDesignCategory.Lossless,
    LibraryDesignCategory.HiRes,
)

private val libraryArtworkColors = listOf(
    TideTunesBrand.Primary,
    TideTunesBrand.Secondary,
    TideTunesBrand.SupportBlue,
    TideTunesBrand.SupportOrange,
    TideTunesBrand.SupportGreen,
    TideTunesBrand.SupportYellow,
)

private data class DesignPlaylist(
    val title: String,
    val description: String,
    val cover: DrawableResource,
    val trackCount: Int,
    val duration: String,
)

private val designPlaylists = listOf(
    DesignPlaylist("My Favorites", "Your liked songs", HomeRes.drawable.home_cover_8, 24, "1h 38m"),
    DesignPlaylist("Evening Frequencies", "Deep electronic for golden hour", HomeRes.drawable.home_cover_1, 18, "1h 12m"),
    DesignPlaylist("Spatial Audio Mix", "Hi-Res Dolby Atmos collection", HomeRes.drawable.home_cover_2, 12, "52m"),
    DesignPlaylist("Deep Focus", "Minimal ambient for concentration", HomeRes.drawable.home_cover_3, 16, "1h 4m"),
    DesignPlaylist("Night Drive", "Synthwave for late-night cruising", HomeRes.drawable.home_cover_4, 22, "1h 28m"),
    DesignPlaylist("Sunrise Protocol", "Gentle morning electronic", HomeRes.drawable.home_cover_5, 14, "56m"),
    DesignPlaylist("System Override", "High-energy techno and industrial", HomeRes.drawable.home_cover_6, 20, "1h 20m"),
)

private data class GenreItem(
    val name: String,
    val colorStart: Color,
    val colorEnd: Color,
    val albumCount: Int,
)

private val libraryGenres = listOf(
    GenreItem("Electronic", TideTunesBrand.Primary, TideTunesBrand.Secondary, 14),
    GenreItem("Ambient", TideTunesBrand.Secondary, TideTunesBrand.SupportBlue, 9),
    GenreItem("Synthwave", TideTunesBrand.SupportOrange, TideTunesBrand.Primary, 11),
    GenreItem("Techno", TideTunesBrand.SupportGreen, TideTunesBrand.SupportBlue, 8),
    GenreItem("IDM", TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange, 6),
    GenreItem("Post-Rock", TideTunesBrand.SupportBlue, TideTunesBrand.Secondary, 7),
    GenreItem("Shoegaze", TideTunesBrand.Primary, TideTunesBrand.SupportOrange, 5),
    GenreItem("Experimental", TideTunesBrand.Secondary, TideTunesBrand.SupportGreen, 12),
    GenreItem("Jazz", TideTunesBrand.SupportOrange, TideTunesBrand.SupportYellow, 10),
    GenreItem("Classical", TideTunesBrand.SupportBlue, TideTunesBrand.SupportGreen, 15),
)

private val demoFolders = listOf(
    "/Music/Electronic",
    "/Music/Ambient",
    "/Downloads/Music",
    "/Synced/WebDAV",
    "/SD Card/Music",
)
