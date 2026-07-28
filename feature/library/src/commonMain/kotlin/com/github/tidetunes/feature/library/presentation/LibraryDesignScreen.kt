package com.github.tidetunes.feature.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.PlaylistSummary
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideGlassScene
import com.github.tidetunes.core.presentation.components.LocalTideBottomContentInset
import com.github.tidetunes.core.presentation.components.TideStickyGlassActionBar
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_album
import tidetunes.core.presentation.generated.resources.icon_cloud
import tidetunes.core.presentation.generated.resources.icon_download
import tidetunes.core.presentation.generated.resources.icon_filter
import tidetunes.core.presentation.generated.resources.icon_folder
import tidetunes.core.presentation.generated.resources.icon_heart
import tidetunes.core.presentation.generated.resources.icon_heart_filled
import tidetunes.core.presentation.generated.resources.icon_dashboard
import tidetunes.core.presentation.generated.resources.icon_log
import tidetunes.core.presentation.generated.resources.icon_music_note
import tidetunes.core.presentation.generated.resources.icon_play
import tidetunes.core.presentation.generated.resources.icon_plus
import tidetunes.core.presentation.generated.resources.icon_chevron_right
import tidetunes.core.presentation.generated.resources.icon_pin
import tidetunes.core.presentation.generated.resources.icon_pin_filled
import tidetunes.core.presentation.generated.resources.icon_search
import tidetunes.core.presentation.generated.resources.icon_vertialcal_more
import tidetunes.feature.home.generated.resources.Res as HomeRes
import tidetunes.feature.home.generated.resources.home_cover_1
import tidetunes.feature.home.generated.resources.home_cover_2
import tidetunes.feature.home.generated.resources.home_cover_3
import tidetunes.feature.home.generated.resources.home_cover_4
import tidetunes.feature.home.generated.resources.home_cover_5
import tidetunes.feature.home.generated.resources.home_cover_6
import tidetunes.feature.home.generated.resources.home_cover_7
import tidetunes.feature.home.generated.resources.home_cover_8
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LibraryDesignScreen(
    state: LibraryState,
    currentPlayingTrackId: Long? = null,
    onNavigateToLibraryFolderImport: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    onNavigateToPlaylist: (Long) -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onAction: (LibraryAction) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(LibraryDesignCategory.Playlists) }
    var songQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(LibrarySortBy.Title) }
    var activeArtistLetter by remember { mutableStateOf<String?>(null) }
    val bottomContentInset = LocalTideBottomContentInset.current

    TideGlassScene(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
        val compact = maxWidth < TideTunesTokens.adaptive.largeMinWidth
        val pagePadding = if (compact) 24.dp else TideTunesTokens.spacing.pageExpanded
        val isDesktop = maxWidth >= TideTunesTokens.adaptive.largeMinWidth
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
        val pageTitleAlpha = (1f - actionBarProgress / 0.70f).coerceIn(0f, 1f)

        if (isDesktop) {
            Row(modifier = Modifier.fillMaxSize()) {
                LibrarySidebar(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
                Box(modifier = Modifier.weight(1f)) {
                    LibraryContent(
                        state = state,
                        selectedCategory = selectedCategory,
                        currentPlayingTrackId = currentPlayingTrackId,
                        onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        onNavigateToPlaylist = onNavigateToPlaylist,
                        onNavigateToPlaylists = onNavigateToPlaylists,
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
                    top = 0.dp,
                    end = pagePadding,
                    bottom = 28.dp + bottomContentInset,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    LibraryMobileHeader(modifier = Modifier.alpha(pageTitleAlpha))
                }
                item {
                    LibraryMobileTabs(
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it },
                    )
                }
                // Category content
                LibraryCategoryItems(
                    state = state,
                    selectedCategory = selectedCategory,
                    currentPlayingTrackId = currentPlayingTrackId,
                    onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                    onNavigateToAlbum = onNavigateToAlbum,
                    onNavigateToArtist = onNavigateToArtist,
                    onNavigateToPlaylist = onNavigateToPlaylist,
                    onNavigateToPlaylists = onNavigateToPlaylists,
                    onAction = onAction,
                    onSelectCategory = { selectedCategory = it },
                    songQuery = songQuery,
                    onSongQueryChange = { songQuery = it },
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it },
                    activeArtistLetter = activeArtistLetter,
                    onArtistLetterChange = { activeArtistLetter = it },
                    showPlaylistMetadata = false,
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
                        .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget)
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
private fun LibraryMobileHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = "Library",
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title1.copy(
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryMobileTabs(
    selected: LibraryDesignCategory,
    onSelect: (LibraryDesignCategory) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .border(1.dp, MiuixTheme.colorScheme.outline, shape)
            .background(MiuixTheme.colorScheme.surface)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        primaryLibraryCategories.forEach { category ->
            val isSelected = selected == category
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MiuixTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(category) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = category.label,
                    color = if (isSelected) {
                        MiuixTheme.colorScheme.onPrimary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: LibraryState,
    selectedCategory: LibraryDesignCategory,
    currentPlayingTrackId: Long?,
    onNavigateToLibraryFolderImport: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToPlaylists: () -> Unit,
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
        val pageTitleAlpha = (1f - actionBarProgress / 0.70f).coerceIn(0f, 1f)

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
                    TidePageHeader(
                        title = "Library",
                        subtitle = null,
                        modifier = Modifier.alpha(pageTitleAlpha),
                    )
                }
            }

            LibraryCategoryItems(
                state = state,
                selectedCategory = selectedCategory,
                currentPlayingTrackId = currentPlayingTrackId,
                onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                onNavigateToAlbum = onNavigateToAlbum,
                onNavigateToArtist = onNavigateToArtist,
                onNavigateToPlaylist = onNavigateToPlaylist,
                onNavigateToPlaylists = onNavigateToPlaylists,
                onAction = onAction,
                onSelectCategory = onSelectCategory,
                songQuery = songQuery,
                onSongQueryChange = onSongQueryChange,
                sortBy = sortBy,
                onSortByChange = onSortByChange,
                activeArtistLetter = activeArtistLetter,
                onArtistLetterChange = onArtistLetterChange,
                showPlaylistMetadata = true,
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
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    onSelectCategory: (LibraryDesignCategory) -> Unit,
    songQuery: String,
    onSongQueryChange: (String) -> Unit,
    sortBy: LibrarySortBy,
    onSortByChange: (LibrarySortBy) -> Unit,
    activeArtistLetter: String?,
    onArtistLetterChange: (String?) -> Unit,
    showPlaylistMetadata: Boolean,
) = with(state) {
    val favoriteTracks = favorites.dataOrNull.orEmpty()
    val tracksForCategory = getTracksForCategory(selectedCategory, tracks, favoriteTracks)
    val filteredTracks = tracksForCategory
        .filter { track ->
            songQuery.isBlank() || listOfNotNull(track.title, track.artist)
                .any { it.contains(songQuery, ignoreCase = true) }
        }
        .sortedWith(sortBy.comparator)
    val favoriteTrackIds = favoriteTracks.mapTo(mutableSetOf()) { it.id }
    val albumCards = albums
        .take(24)
        .mapIndexed { index, album ->
            LibraryAlbumCardItem(
                id = album.id,
                title = album.name,
                year = album.year,
                cover = designAlbumCards[index % designAlbumCards.size].cover,
            )
        }
        .ifEmpty { designAlbumCards }
    val artistRows = artists
        .map { artist -> LibraryArtistRowItem(id = artist.id, name = artist.name) }
        .ifEmpty { designArtistRows }
    val genreCards = genreNames.dataOrNull
        .orEmpty()
        .map { genre -> LibraryGenreCardItem(name = genre) }
        .ifEmpty { designGenreCards }

    val isSongTab = selectedCategory in songLibraryCategories

    // Category header with actions
    item {
        CategorySectionHeader(
            title = selectedCategory.label,
            metadata = libraryMetadata(
                category = selectedCategory,
                state = state,
                albumCount = albumCards.size,
                artistCount = artistRows.size,
                genreCount = genreCards.size,
            ),
            showShuffle = isSongTab && filteredTracks.isNotEmpty() && showPlaylistMetadata,
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
            onNewPlaylist = onNavigateToPlaylists,
        )
    }

    // Search + Sort for song tabs
    if (isSongTab && tracksForCategory.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibrarySongSearchBar(
                    value = songQuery,
                    onValueChange = onSongQueryChange,
                    placeholder = if (selectedCategory == LibraryDesignCategory.Songs) {
                        "Search songs, artists, or albums"
                    } else {
                        "Search ${selectedCategory.label.lowercase()}"
                    },
                    modifier = Modifier.weight(1f),
                )
                SongFilterButton(
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
                        isFavorite = track.id in favoriteTrackIds,
                        onPlay = { onAction(LibraryAction.PlayTrack(track.id)) },
                        onToggleFavorite = { onAction(LibraryAction.ToggleFavorite(track.id)) },
                        onMore = {},
                    )
                }
            }
        }

        LibraryDesignCategory.Albums -> {
            item {
                LibraryAlbumGrid(
                    albums = albumCards,
                    onOpenAlbum = { onNavigateToAlbum(it.id) },
                )
            }
        }

        LibraryDesignCategory.Artists -> {
            item {
                LibraryArtistGrouped(
                    artists = artistRows,
                    activeLetter = activeArtistLetter,
                    onLetterChange = onArtistLetterChange,
                    onOpenArtist = { onNavigateToArtist(it.id) },
                )
            }
        }

        LibraryDesignCategory.Genres -> item {
            LibraryGenreGrid(
                genres = genreCards,
                onOpenGenre = { genre -> onAction(LibraryAction.SelectGenre(genre.name)) },
            )
        }

        LibraryDesignCategory.Folders -> item {
            TideCardSurface(contentPadding = PaddingValues(24.dp)) {
                LibraryEmptyContent(
                    title = "No folders added",
                    message = "Import a folder to add its music to your library.",
                    action = "Import folder" to onNavigateToLibraryFolderImport,
                    painter = painterResource(CoreRes.drawable.icon_folder),
                )
            }
        }

        LibraryDesignCategory.Playlists -> item {
            PlaylistListView(
                playlists = playlists.toLibraryPlaylistRows(favoriteTracks),
                onOpenPlaylist = { playlist ->
                    if (playlist.key == FavoritesPlaylistKey) {
                        onSelectCategory(LibraryDesignCategory.Favorites)
                    } else {
                        playlist.summary?.let { onNavigateToPlaylist(it.id) }
                            ?: onNavigateToPlaylists()
                    }
                },
                onManagePlaylists = onNavigateToPlaylists,
                showMetadata = showPlaylistMetadata,
            )
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
            .height(TideTunesTokens.adaptive.minimumTouchTarget)
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
            .width(88.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(MiuixTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_play),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
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
            .width(88.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(MiuixTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_plus),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "New",
            color = Color.White,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Song Search + Filter ──

@Composable
private fun LibrarySongSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surface)
            .border(1.dp, MiuixTheme.colorScheme.outline, shape)
            .padding(horizontal = 14.dp),
        singleLine = true,
        textStyle = MiuixTheme.textStyles.body2.copy(
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        ),
        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(CoreRes.drawable.icon_search),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(16.dp),
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "×",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SongFilterButton(
    current: LibrarySortBy,
    onChange: (LibrarySortBy) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surface)
            .border(1.dp, MiuixTheme.colorScheme.outline, shape)
            .clickable {
                val nextIndex = (LibrarySortBy.entries.indexOf(current) + 1) % LibrarySortBy.entries.size
                onChange(LibrarySortBy.entries[nextIndex])
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_filter),
            contentDescription = "Filter songs, sorted by ${current.label}",
            tint = if (current == LibrarySortBy.Title) {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            } else {
                MiuixTheme.colorScheme.primary
            },
            modifier = Modifier.size(16.dp),
        )
        if (current != LibrarySortBy.Title) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary),
            )
        }
    }
}

// ── Song Row ──

@Composable
private fun LibrarySongRow(
    track: LibraryTrackItem,
    rank: Int?,
    playing: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
) {
    val dividerColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                if (playing) MiuixTheme.colorScheme.primary.copy(alpha = 0.10f)
                else Color.Transparent,
            )
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onPlay),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rank?.toString() ?: "",
                    color = if (playing) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    style = MiuixTheme.textStyles.footnote2.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    maxLines = 1,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = track.title,
                    color = if (playing) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onBackground
                    },
                    style = MiuixTheme.textStyles.body1.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist ?: "Unknown Artist",
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.width(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) CoreRes.drawable.icon_heart_filled else CoreRes.drawable.icon_heart,
                    ),
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(CoreRes.drawable.icon_vertialcal_more),
                    contentDescription = "More actions for ${track.title}",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Album Grid ──

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryAlbumGrid(
    albums: List<LibraryAlbumCardItem>,
    onOpenAlbum: (LibraryAlbumCardItem) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 800.dp -> 4
            maxWidth >= 500.dp -> 3
            else -> 2
        }
        val itemPadding = TideTunesTokens.spacing.xxs
        val gap = 16.dp - itemPadding * 2
        val width = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            maxItemsInEachRow = columns,
        ) {
            albums.forEachIndexed { index, album ->
                AlbumCard(album, index, width, itemPadding, onOpenAlbum)
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: LibraryAlbumCardItem,
    index: Int,
    width: Dp,
    contentPadding: Dp,
    onOpenAlbum: (LibraryAlbumCardItem) -> Unit,
) {
    val artworkShape = RoundedCornerShape(14.dp)
    val artworkSize = width - contentPadding * 2
    val metadata = listOfNotNull(
        album.artist?.takeIf(String::isNotBlank),
        album.year?.toString(),
    ).joinToString(" · ")
    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenAlbum(album) }
            .padding(contentPadding),
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .shadow(TideTunesTokens.elevation.card, artworkShape, clip = false)
                .clip(artworkShape)
                .background(libraryArtworkBrush(index)),
            contentAlignment = Alignment.Center,
        ) {
            if (album.cover != null) {
                Image(
                    painter = painterResource(album.cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = album.title.take(2).uppercase(),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = album.title,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (metadata.isNotBlank()) {
            Text(
                text = metadata,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Artist Grouped ──

@Composable
private fun LibraryArtistGrouped(
    artists: List<LibraryArtistRowItem>,
    activeLetter: String?,
    onLetterChange: (String?) -> Unit,
    onOpenArtist: (LibraryArtistRowItem) -> Unit,
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
                                .clickable { onOpenArtist(artist) }
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
                                    text = artist.genre ?: "Artist",
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
private fun LibraryGenreGrid(
    genres: List<LibraryGenreCardItem>,
    onOpenGenre: (LibraryGenreCardItem) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 640.dp) 3 else 2
        val gap = 12.dp
        val width = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            maxItemsInEachRow = columns,
        ) {
            genres.forEachIndexed { index, genre ->
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(libraryArtworkBrush(index))
                        .clickable { onOpenGenre(genre) }
                        .padding(16.dp),
                ) {
                    Text(
                        text = genre.name,
                        color = Color.White,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    genre.albumCount?.let { albumCount ->
                        Text(
                            text = "$albumCount albums",
                            color = Color.White,
                            style = MiuixTheme.textStyles.footnote2,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── Playlist List ──

@Composable
private fun PlaylistListView(
    playlists: List<LibraryPlaylistRowItem>,
    onOpenPlaylist: (LibraryPlaylistRowItem) -> Unit,
    onManagePlaylists: () -> Unit,
    showMetadata: Boolean,
) {
    var pinnedPlaylistKeys by remember(playlists) {
        mutableStateOf<Set<String>>(playlists.filter { it.isInitiallyPinned }.mapTo(mutableSetOf()) { it.key })
    }

    Column {
        playlists.forEachIndexed { index, playlist ->
            val isPinned = playlist.key in pinnedPlaylistKeys
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onOpenPlaylist(playlist) },
                            onLongClick = onManagePlaylists,
                        )
                        .padding(start = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (playlist.designCover != null) {
                        Image(
                            painter = painterResource(playlist.designCover),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(3.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    } else {
                        ArtworkImage(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            artwork = playlist.summary?.coverArtwork,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = playlist.title,
                            color = MiuixTheme.colorScheme.onBackground,
                            style = MiuixTheme.textStyles.body1.copy(
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                            ),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = playlist.description,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            style = MiuixTheme.textStyles.footnote1.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (showMetadata) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "${playlist.musicCount} tracks",
                                color = MiuixTheme.colorScheme.onSurface,
                                style = MiuixTheme.textStyles.footnote2,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = playlist.durationLabel,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                style = MiuixTheme.textStyles.footnote2,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            pinnedPlaylistKeys = if (isPinned) {
                                pinnedPlaylistKeys - playlist.key
                            } else {
                                pinnedPlaylistKeys + playlist.key
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPinned) CoreRes.drawable.icon_pin_filled else CoreRes.drawable.icon_pin,
                        ),
                        contentDescription = if (isPinned) "Unpin playlist" else "Pin playlist",
                        tint = if (isPinned) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (index < playlists.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget)
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

// ── Metadata ──

private fun libraryMetadata(
    category: LibraryDesignCategory,
    state: LibraryState,
    albumCount: Int,
    artistCount: Int,
    genreCount: Int,
): String {
    val tracks = state.tracks
    return when (category) {
        LibraryDesignCategory.Songs -> {
            "${tracks.size} songs · ${formatLibraryDuration(tracks)}"
        }
        LibraryDesignCategory.Albums -> "$albumCount albums"
        LibraryDesignCategory.Artists -> "$artistCount artists"
        LibraryDesignCategory.Genres -> "$genreCount genres"
        LibraryDesignCategory.Folders -> "Import a music folder"
        LibraryDesignCategory.Playlists -> {
            val playlistCount = if (state.playlists.isEmpty()) designPlaylistRows.size else state.playlists.size + 1
            "$playlistCount playlists · Long press to edit"
        }
        LibraryDesignCategory.Downloads -> "Available offline"
        LibraryDesignCategory.Favorites -> {
            val favoriteTracks = state.favorites.dataOrNull.orEmpty()
            "${favoriteTracks.size} songs · ${formatLibraryDuration(favoriteTracks)}"
        }
        else -> "No collection data available"
    }
}

private fun getTracksForCategory(
    category: LibraryDesignCategory,
    tracks: List<LibraryTrackItem>,
    favoriteTracks: List<LibraryTrackItem>,
): List<LibraryTrackItem> {
    return when (category) {
        LibraryDesignCategory.Songs -> tracks
        LibraryDesignCategory.Favorites -> favoriteTracks
        else -> emptyList()
    }
}

// ── Utilities ──

private fun libraryArtworkBrush(index: Int): Brush {
    val safeIdx = index.mod(libraryArtworkColors.size)
    val first = libraryArtworkColors[safeIdx]
    val second = libraryArtworkColors[(safeIdx + 1).mod(libraryArtworkColors.size)]
    return Brush.linearGradient(listOf(first, second))
}

private fun formatLibraryDuration(tracks: List<LibraryTrackItem>): String {
    val totalMs = tracks.sumOf { (it.durationMs ?: 0L).coerceAtLeast(0L) }
    val totalMinutes = (totalMs + 59_999L) / 60_000L
    return if (totalMinutes >= 60L) {
        "${totalMinutes / 60L}h ${totalMinutes % 60L}m"
    } else {
        "$totalMinutes min"
    }
}

private fun PlaylistSummary.compactMetadata(): String {
    val trackLabel = if (musicCount == 1L) "track" else "tracks"
    return "$musicCount $trackLabel · ${formatPlaylistDuration(durationMs)}"
}

private fun List<PlaylistSummary>.toLibraryPlaylistRows(
    favoriteTracks: List<LibraryTrackItem>,
): List<LibraryPlaylistRowItem> {
    val favoritesRow = LibraryPlaylistRowItem(
        key = FavoritesPlaylistKey,
        title = "My Favorites",
        description = "Your liked songs",
        musicCount = favoriteTracks.size.toLong(),
        durationLabel = formatPlaylistDuration(favoriteTracks.sumOf { it.durationMs ?: 0L }),
        designCover = HomeRes.drawable.home_cover_1,
        isInitiallyPinned = true,
    )
    val playlistRows = if (isEmpty()) {
        designPlaylistRows.drop(1)
    } else {
        map { playlist ->
            LibraryPlaylistRowItem(
                key = "playlist-${playlist.id}",
                title = playlist.title,
                description = playlist.compactMetadata(),
                musicCount = playlist.musicCount,
                durationLabel = formatPlaylistDuration(playlist.durationMs),
                summary = playlist,
            )
        }
    }

    return listOf(favoritesRow) + playlistRows
}

private fun formatPlaylistDuration(durationMs: Long): String {
    val totalMinutes = (durationMs.coerceAtLeast(0L) + 59_999L) / 60_000L
    return if (totalMinutes >= 60L) {
        "${totalMinutes / 60L}h ${totalMinutes % 60L}m"
    } else {
        "$totalMinutes min"
    }
}

private fun artistInitials(name: String): String = name
    .split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1).uppercase() }
    .ifBlank { "?" }

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

private data class LibraryAlbumCardItem(
    val id: Long,
    val title: String,
    val artist: String? = null,
    val year: Int? = null,
    val cover: DrawableResource? = null,
)

private data class LibraryArtistRowItem(
    val id: Long,
    val name: String,
    val genre: String? = null,
)

private data class LibraryGenreCardItem(
    val name: String,
    val albumCount: Int? = null,
)

private data class LibraryPlaylistRowItem(
    val key: String,
    val title: String,
    val description: String,
    val musicCount: Long,
    val durationLabel: String,
    val summary: PlaylistSummary? = null,
    val designCover: DrawableResource? = null,
    val isInitiallyPinned: Boolean = false,
)

private const val FavoritesPlaylistKey = "favorites"

private val designAlbumCards = listOf(
    LibraryAlbumCardItem(1L, "Tidal Drift", "Luna Waves", 2024, HomeRes.drawable.home_cover_1),
    LibraryAlbumCardItem(2L, "Voltage Dreams", "Prism Circuit", 2024, HomeRes.drawable.home_cover_2),
    LibraryAlbumCardItem(3L, "Open Water", "Coastal Drift", 2023, HomeRes.drawable.home_cover_3),
    LibraryAlbumCardItem(4L, "Northern Lights", "Polar Echo", 2024, HomeRes.drawable.home_cover_4),
    LibraryAlbumCardItem(5L, "Subsonic", "Ocean Syntax", 2023, HomeRes.drawable.home_cover_5),
    LibraryAlbumCardItem(6L, "Glass Architecture", "Fractal Mind", 2024, HomeRes.drawable.home_cover_6),
    LibraryAlbumCardItem(7L, "Quantum", "Wave Function", 2024, HomeRes.drawable.home_cover_7),
    LibraryAlbumCardItem(8L, "Between", "Threshold", 2023, HomeRes.drawable.home_cover_8),
)

private val designArtistRows = listOf(
    LibraryArtistRowItem(1L, "Luna Waves", "Electronic"),
    LibraryArtistRowItem(2L, "Prism Circuit", "Synthwave"),
    LibraryArtistRowItem(3L, "Coastal Drift", "Ambient"),
    LibraryArtistRowItem(4L, "Polar Echo", "IDM"),
    LibraryArtistRowItem(5L, "Ocean Syntax", "Techno"),
    LibraryArtistRowItem(6L, "Fractal Mind", "Post-Rock"),
)

private val designGenreCards = listOf(
    LibraryGenreCardItem("Electronic", 6),
    LibraryGenreCardItem("Ambient", 13),
    LibraryGenreCardItem("Synthwave", 20),
    LibraryGenreCardItem("Techno", 8),
    LibraryGenreCardItem("IDM", 15),
    LibraryGenreCardItem("Post-Rock", 22),
    LibraryGenreCardItem("Shoegaze", 10),
    LibraryGenreCardItem("Experimental", 17),
    LibraryGenreCardItem("Jazz", 24),
    LibraryGenreCardItem("Classical", 12),
)

private val designPlaylistRows = listOf(
    LibraryPlaylistRowItem(
        key = "design-favorites",
        title = "My Favorites",
        description = "Your liked songs",
        musicCount = 4,
        durationLabel = "14m 22s",
        designCover = HomeRes.drawable.home_cover_1,
        isInitiallyPinned = true,
    ),
    LibraryPlaylistRowItem(
        key = "design-evening-frequencies",
        title = "Evening Frequencies",
        description = "Deep electronic for golden hour",
        musicCount = 24,
        durationLabel = "1h 32m",
        designCover = HomeRes.drawable.home_cover_1,
    ),
    LibraryPlaylistRowItem(
        key = "design-spatial-audio-mix",
        title = "Spatial Audio Mix",
        description = "Hi-Res Dolby Atmos collection",
        musicCount = 18,
        durationLabel = "1h 08m",
        designCover = HomeRes.drawable.home_cover_2,
    ),
    LibraryPlaylistRowItem(
        key = "design-deep-focus",
        title = "Deep Focus",
        description = "Minimal ambient for concentration",
        musicCount = 32,
        durationLabel = "2h 15m",
        designCover = HomeRes.drawable.home_cover_3,
    ),
    LibraryPlaylistRowItem(
        key = "design-night-drive",
        title = "Night Drive",
        description = "Synthwave for late-night cruising",
        musicCount = 20,
        durationLabel = "1h 22m",
        designCover = HomeRes.drawable.home_cover_4,
    ),
    LibraryPlaylistRowItem(
        key = "design-sunrise-protocol",
        title = "Sunrise Protocol",
        description = "Gentle morning electronic",
        musicCount = 16,
        durationLabel = "58m",
        designCover = HomeRes.drawable.home_cover_5,
    ),
    LibraryPlaylistRowItem(
        key = "design-system-override",
        title = "System Override",
        description = "High-energy techno and industrial",
        musicCount = 28,
        durationLabel = "1h 45m",
        designCover = HomeRes.drawable.home_cover_6,
    ),
)

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
    LibraryDesignCategory.Genres,
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
