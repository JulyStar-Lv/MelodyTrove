package com.github.tidetunes.feature.library.presentation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_album
import tidetunes.core.presentation.generated.resources.icon_cloud
import tidetunes.core.presentation.generated.resources.icon_download
import tidetunes.core.presentation.generated.resources.icon_folder
import tidetunes.core.presentation.generated.resources.icon_log
import tidetunes.core.presentation.generated.resources.icon_music_note
import tidetunes.core.presentation.generated.resources.icon_pause
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

    BoxWithConstraints(
        modifier = Modifier
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
                top = 8.dp,
                end = pagePadding,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                TidePageHeader(
                    title = "Library",
                    subtitle = "${state.tracks.size} songs · ${state.albums.size} albums · ${state.artists.size} artists",
                )
            }
            item {
                PrimaryLibraryTabs(
                    selected = selectedCategory,
                    onSelected = { selectedCategory = it },
                )
            }
            item {
                SecondaryLibraryCategories(
                    selected = selectedCategory,
                    onSelected = { selectedCategory = it },
                )
            }

            when (selectedCategory) {
                LibraryDesignCategory.Songs -> {
                    item {
                        LibrarySummaryRow(
                            title = "All Songs",
                            detail = libraryDurationSummary(state.tracks),
                        )
                    }
                    if (state.tracks.isEmpty()) {
                        item {
                            LibraryEmptyPanel(
                                title = "Your library is empty",
                                message = "Scan local storage or add a WebDAV source to begin building your unified library.",
                                action = "Scan a folder",
                                onAction = onNavigateToLibraryFolderImport,
                                painter = painterResource(CoreRes.drawable.icon_music_note),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = state.tracks,
                            key = { index, track -> track.id.takeIf { it > 0L } ?: index },
                        ) { index, track ->
                            LibrarySongRow(
                                track = track,
                                index = index,
                                playing = track.id == currentPlayingTrackId,
                                onPlay = { onAction(LibraryAction.PlayTrack(track.id)) },
                                onDownload = { onAction(LibraryAction.DownloadTrack(track)) },
                            )
                        }
                    }
                }

                LibraryDesignCategory.Albums -> item {
                    LibraryAlbumGrid(albums = state.albums)
                }

                LibraryDesignCategory.Artists -> item {
                    LibraryArtistGrid(artists = state.artists)
                }

                LibraryDesignCategory.Playlists -> item {
                    PlaylistOverview(
                        trackCount = state.tracks.size,
                        onOpenSongs = { selectedCategory = LibraryDesignCategory.Songs },
                    )
                }

                LibraryDesignCategory.Folders -> item {
                    LibraryEmptyPanel(
                        title = "Music folders",
                        message = "Choose folders to scan and keep your local library organized by its original structure.",
                        action = "Add folder",
                        onAction = onNavigateToLibraryFolderImport,
                        painter = painterResource(CoreRes.drawable.icon_folder),
                    )
                }

                LibraryDesignCategory.Sources -> item {
                    LibraryEmptyPanel(
                        title = "One library, every source",
                        message = "Manage Local and WebDAV sources from Settings. New scans merge into this library without duplicating its navigation.",
                        painter = painterResource(CoreRes.drawable.icon_cloud),
                    )
                }

                LibraryDesignCategory.Downloads -> item {
                    LibraryEmptyPanel(
                        title = "Downloads",
                        message = "Downloaded tracks will appear here for offline playback.",
                        painter = painterResource(CoreRes.drawable.icon_download),
                    )
                }

                LibraryDesignCategory.History,
                LibraryDesignCategory.RecentlyPlayed -> item {
                    LibraryTrackCollection(
                        title = selectedCategory.label,
                        tracks = state.tracks.take(12),
                        currentPlayingTrackId = currentPlayingTrackId,
                        onAction = onAction,
                    )
                }

                LibraryDesignCategory.RecentlyAdded -> item {
                    LibraryAlbumGrid(albums = state.albums.take(12))
                }

                LibraryDesignCategory.Favorites,
                LibraryDesignCategory.Lossless,
                LibraryDesignCategory.HiRes,
                LibraryDesignCategory.Genres -> item {
                    LibraryEmptyPanel(
                        title = selectedCategory.label,
                        message = selectedCategory.emptyMessage,
                        painter = painterResource(selectedCategory.icon),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryLibraryTabs(
    selected: LibraryDesignCategory,
    onSelected: (LibraryDesignCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        primaryLibraryCategories.forEach { category ->
            LibraryCategoryChip(
                category = category,
                selected = selected == category,
                onClick = { onSelected(category) },
                prominent = true,
            )
        }
    }
}

@Composable
private fun SecondaryLibraryCategories(
    selected: LibraryDesignCategory,
    onSelected: (LibraryDesignCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        secondaryLibraryCategories.forEach { category ->
            LibraryCategoryChip(
                category = category,
                selected = selected == category,
                onClick = { onSelected(category) },
                prominent = false,
            )
        }
    }
}

@Composable
private fun LibraryCategoryChip(
    category: LibraryDesignCategory,
    selected: Boolean,
    prominent: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        Brush.linearGradient(
            listOf(TideTunesBrand.Primary, TideTunesBrand.Secondary),
        )
    } else {
        Brush.linearGradient(
            listOf(
                MiuixTheme.colorScheme.surfaceVariant,
                MiuixTheme.colorScheme.surfaceVariant,
            ),
        )
    }
    Row(
        modifier = Modifier
            .height(if (prominent) 42.dp else 36.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = if (prominent) 16.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            painter = painterResource(category.icon),
            contentDescription = null,
            tint = if (selected) Color.White else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(if (prominent) 17.dp else 15.dp),
        )
        Text(
            text = category.label,
            color = if (selected) Color.White else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = if (prominent) MiuixTheme.textStyles.body1 else MiuixTheme.textStyles.body2,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun LibrarySummaryRow(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = detail,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun LibrarySongRow(
    track: LibraryTrackItem,
    index: Int,
    playing: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (playing) MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                else Color.Transparent,
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryArtwork(
            index = index,
            size = 48.dp,
            playing = playing,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (playing) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            style = MiuixTheme.textStyles.footnote1,
        )
        if (track.mediaId != null) {
            Icon(
                painter = painterResource(CoreRes.drawable.icon_download),
                contentDescription = "Download",
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDownload)
                    .padding(7.dp)
                    .size(17.dp),
            )
        }
    }
}

@Composable
private fun LibraryTrackCollection(
    title: String,
    tracks: List<LibraryTrackItem>,
    currentPlayingTrackId: Long?,
    onAction: (LibraryAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LibrarySummaryRow(title, "${tracks.size} songs")
        Spacer(modifier = Modifier.height(4.dp))
        if (tracks.isEmpty()) {
            LibraryEmptyPanel(
                title = title,
                message = "No music is available in this collection yet.",
                painter = painterResource(CoreRes.drawable.icon_log),
            )
        } else {
            tracks.forEachIndexed { index, track ->
                LibrarySongRow(
                    track = track,
                    index = index,
                    playing = track.id == currentPlayingTrackId,
                    onPlay = { onAction(LibraryAction.PlayTrack(track.id)) },
                    onDownload = { onAction(LibraryAction.DownloadTrack(track)) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryAlbumGrid(albums: List<LibraryAlbumItem>) {
    if (albums.isEmpty()) {
        LibraryEmptyPanel(
            title = "Albums",
            message = "Albums appear after music metadata has been scanned.",
            painter = painterResource(CoreRes.drawable.icon_album),
        )
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 960.dp -> 5
            maxWidth >= 700.dp -> 4
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
        val shape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .size(width)
                .shadow(TideTunesTokens.elevation.card, shape, clip = false)
                .clip(shape)
                .background(libraryArtworkBrush(index)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CoreRes.drawable.icon_album),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.30f),
                modifier = Modifier.size(width * 0.34f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = album.name,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.year?.toString() ?: "Unknown year",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryArtistGrid(artists: List<LibraryArtistItem>) {
    if (artists.isEmpty()) {
        LibraryEmptyPanel(
            title = "Artists",
            message = "Artists appear after music metadata has been scanned.",
            painter = painterResource(CoreRes.drawable.icon_music_note),
        )
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 960.dp -> 6
            maxWidth >= 700.dp -> 4
            else -> 2
        }
        val gap = 18.dp
        val width = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            maxItemsInEachRow = columns,
        ) {
            artists.forEachIndexed { index, artist ->
                Column(
                    modifier = Modifier.width(width),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width)
                            .shadow(TideTunesTokens.elevation.card, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(libraryArtworkBrush(index)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = artistInitials(artist.name),
                            color = Color.White.copy(alpha = 0.92f),
                            style = MiuixTheme.textStyles.title1,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
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
            }
        }
    }
}

@Composable
private fun PlaylistOverview(
    trackCount: Int,
    onOpenSongs: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LibrarySummaryRow("Playlists", "Unified library")
        PlaylistOverviewRow(
            title = "My Favorites",
            description = "Songs you have marked as favorites",
            meta = "$trackCount available songs",
            index = 0,
            onClick = onOpenSongs,
        )
        PlaylistOverviewRow(
            title = "Recently Played",
            description = "Continue from your listening history",
            meta = "Updated automatically",
            index = 1,
            onClick = onOpenSongs,
        )
        PlaylistOverviewRow(
            title = "Recently Added",
            description = "The newest music across every source",
            meta = "Sorted by import time",
            index = 2,
            onClick = onOpenSongs,
        )
    }
}

@Composable
private fun PlaylistOverviewRow(
    title: String,
    description: String,
    meta: String,
    index: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(libraryArtworkBrush(index)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CoreRes.drawable.icon_music_note),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = meta,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            style = MiuixTheme.textStyles.footnote2,
            maxLines = 1,
        )
    }
}

@Composable
private fun LibraryEmptyPanel(
    title: String,
    message: String,
    painter: Painter,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    TideCardSurface(
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MiuixTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(23.dp),
                )
            }
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (action != null && onAction != null) {
                Text(
                    text = action,
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
                        .clickable(onClick = onAction)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryArtwork(index: Int, size: Dp, playing: Boolean) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(libraryArtworkBrush(index)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                if (playing) CoreRes.drawable.icon_pause else CoreRes.drawable.icon_music_note,
            ),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.86f),
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun libraryArtworkBrush(index: Int): Brush {
    val first = libraryArtworkColors[index.mod(libraryArtworkColors.size)]
    val second = libraryArtworkColors[(index + 1).mod(libraryArtworkColors.size)]
    return Brush.linearGradient(listOf(first, second))
}

private fun formatDuration(durationMs: Long?): String {
    val totalSeconds = ((durationMs ?: 0L) / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun libraryDurationSummary(tracks: List<LibraryTrackItem>): String {
    val totalMinutes = tracks.sumOf { (it.durationMs ?: 0L).coerceAtLeast(0L) } / 60_000L
    return "${tracks.size} songs · ~$totalMinutes min"
}

private fun artistInitials(name: String): String = name
    .split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1).uppercase() }
    .ifBlank { "?" }

private enum class LibraryDesignCategory(
    val label: String,
    val icon: org.jetbrains.compose.resources.DrawableResource,
    val emptyMessage: String = "No music is available in this collection yet.",
) {
    Playlists("Playlists", CoreRes.drawable.icon_music_note),
    Songs("Songs", CoreRes.drawable.icon_music_note),
    Albums("Albums", CoreRes.drawable.icon_album),
    Artists("Artists", CoreRes.drawable.icon_music_note),
    Genres("Genres", CoreRes.drawable.icon_album, "Genres appear after metadata has been indexed."),
    Folders("Folders", CoreRes.drawable.icon_folder),
    Favorites("Favorites", CoreRes.drawable.icon_music_note, "Favorite songs will appear here."),
    Downloads("Downloads", CoreRes.drawable.icon_download),
    History("History", CoreRes.drawable.icon_log),
    RecentlyAdded("Recently Added", CoreRes.drawable.icon_album),
    RecentlyPlayed("Recently Played", CoreRes.drawable.icon_log),
    Lossless("Lossless", CoreRes.drawable.icon_music_note, "Lossless tracks will appear after format metadata has been scanned."),
    HiRes("Hi-Res", CoreRes.drawable.icon_music_note, "Hi-Res tracks will appear after quality metadata has been scanned."),
    Sources("Sources", CoreRes.drawable.icon_cloud),
}

private val primaryLibraryCategories = listOf(
    LibraryDesignCategory.Playlists,
    LibraryDesignCategory.Songs,
    LibraryDesignCategory.Albums,
    LibraryDesignCategory.Artists,
)

private val secondaryLibraryCategories = listOf(
    LibraryDesignCategory.Genres,
    LibraryDesignCategory.Folders,
    LibraryDesignCategory.Favorites,
    LibraryDesignCategory.Downloads,
    LibraryDesignCategory.History,
    LibraryDesignCategory.RecentlyAdded,
    LibraryDesignCategory.RecentlyPlayed,
    LibraryDesignCategory.Lossless,
    LibraryDesignCategory.HiRes,
    LibraryDesignCategory.Sources,
)

private val libraryArtworkColors = listOf(
    TideTunesBrand.Primary,
    TideTunesBrand.Secondary,
    TideTunesBrand.SupportBlue,
    TideTunesBrand.SupportOrange,
    TideTunesBrand.SupportGreen,
    TideTunesBrand.SupportYellow,
)
