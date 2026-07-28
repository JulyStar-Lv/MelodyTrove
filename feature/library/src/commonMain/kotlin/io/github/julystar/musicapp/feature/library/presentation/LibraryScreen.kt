package io.github.julystar.musicapp.feature.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.presentation.components.DesignEmptyState
import io.github.julystar.musicapp.core.presentation.components.DesignPageHeader
import io.github.julystar.musicapp.core.presentation.components.QualityBadge
import io.github.julystar.musicapp.core.presentation.components.QualityBadgeType
import io.github.julystar.musicapp.core.presentation.components.DesignTabItem
import io.github.julystar.musicapp.core.presentation.components.DesignTabs
import io.github.julystar.musicapp.core.presentation.components.DesignTabsVariant
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import org.jetbrains.compose.resources.painterResource
import musicapp.core.presentation.generated.resources.Res as CorePresentationRes
import musicapp.core.presentation.generated.resources.icon_adjust
import musicapp.core.presentation.generated.resources.icon_album
import musicapp.core.presentation.generated.resources.icon_chevron_right
import musicapp.core.presentation.generated.resources.icon_cloud
import musicapp.core.presentation.generated.resources.icon_download
import musicapp.core.presentation.generated.resources.icon_folder
import musicapp.core.presentation.generated.resources.icon_log
import musicapp.core.presentation.generated.resources.icon_mode_list
import musicapp.core.presentation.generated.resources.icon_mode_repeat
import musicapp.core.presentation.generated.resources.icon_music_note
import musicapp.core.presentation.generated.resources.icon_onedrive
import musicapp.core.presentation.generated.resources.icon_pause
import musicapp.core.presentation.generated.resources.icon_vertialcal_more
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LibraryScreen(
    state: LibraryState,
    currentPlayingTrackId: Long? = null,
    onNavigateToLibraryFolderImport: () -> Unit = {},
    onAction: (LibraryAction) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(LibraryCategory.Songs) }
    val spacing = DesignTokens.spacing
    val tracks = state.tracks.ifEmpty { demoLibraryTracks }
    val categoryTracks = selectedCategory.selectTracks(tracks)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = spacing.pageCompact
        val showPageHeader = maxWidth < 1024.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = 8.dp,
                end = horizontalPadding,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showPageHeader) {
                item {
                    DesignPageHeader(
                        title = "Library",
                        subtitle = null,
                    )
                }
            }
            item {
                LibraryCategoryStrip(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                )
            }
            when (selectedCategory) {
                LibraryCategory.Songs -> {
                    item { LibrarySongHeader(tracks) }
                    if (tracks.isEmpty()) {
                        item { LibraryEmptyState() }
                    } else {
                        itemsIndexed(
                            items = tracks,
                            key = { index, track -> track.lazyListKey(index) },
                        ) { index, track ->
                            LibraryTrackRow(
                                track = track,
                                index = index,
                                playing = track.id == currentPlayingTrackId,
                                onPlay = { onAction(LibraryAction.PlayTrack(track.id)) },
                                onDownload = { onAction(LibraryAction.DownloadTrack(track)) },
                            )
                        }
                    }
                }
                LibraryCategory.Albums -> item { LibraryAlbumGrid(state.albums) }
                LibraryCategory.Artists -> item { LibraryArtistGrid(state.artists) }
                LibraryCategory.Genres -> item { LibraryGenreGrid() }
                LibraryCategory.Folders -> item {
                    LibraryFoldersState(onImportFolder = onNavigateToLibraryFolderImport)
                }
                LibraryCategory.Favorites,
                LibraryCategory.History,
                LibraryCategory.RecentlyAdded,
                LibraryCategory.RecentlyPlayed,
                LibraryCategory.Lossless,
                LibraryCategory.HiRes -> {
                    if (categoryTracks.isEmpty()) {
                        item { LibraryCategoryEmpty(selectedCategory) }
                    } else {
                        itemsIndexed(
                            items = categoryTracks,
                            key = { index, track -> "${selectedCategory.name}-${track.lazyListKey(index)}" },
                        ) { index, track ->
                            LibraryTrackRow(
                                track = track,
                                index = index,
                                playing = track.id == currentPlayingTrackId,
                                onPlay = { onAction(LibraryAction.PlayTrack(track.id)) },
                                onDownload = { onAction(LibraryAction.DownloadTrack(track)) },
                            )
                        }
                    }
                }
                LibraryCategory.Playlists -> item { LibraryPlaylistGrid() }
                LibraryCategory.Sources -> item { LibrarySourcesGrid() }
                LibraryCategory.Downloads -> item { LibraryCategoryEmpty(selectedCategory) }
            }
        }
    }
}

@Composable
private fun LibraryCategoryStrip(
    selectedCategory: LibraryCategory,
    onCategorySelected: (LibraryCategory) -> Unit,
) {
    val categories = LibraryCategory.entries
    DesignTabs(
        items = categories.map { category ->
            DesignTabItem(
                label = category.label,
                enabled = true,
            )
        },
        selectedIndex = categories.indexOf(selectedCategory),
        onSelectedIndexChange = { index -> onCategorySelected(categories[index]) },
        variant = DesignTabsVariant.Pill,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun LibrarySongHeader(tracks: List<LibraryTrackItem>) {
    val totalMinutes = tracks.mapNotNull { it.durationMs }.sum() / 60_000
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${tracks.size} songs · ~$totalMinutes min",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.body2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LibraryRoundAction(
                painter = painterResource(CorePresentationRes.drawable.icon_adjust),
                contentDescription = "Filter",
            )
            LibraryRoundAction(
                painter = painterResource(CorePresentationRes.drawable.icon_mode_list),
                contentDescription = "List view",
            )
        }
    }
}

@Composable
private fun LibraryRoundAction(
    painter: Painter,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LibraryTrackRow(
    track: LibraryTrackItem,
    index: Int,
    playing: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    val startColor = libraryGradientColors[index % libraryGradientColors.size]
    val endColor = libraryGradientColors[(index + 1) % libraryGradientColors.size]
    val titleColor = if (playing) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (playing) MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
                else Color.Transparent,
            )
            .clickable(onClick = onPlay)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(startColor, endColor))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (playing) {
                        CorePresentationRes.drawable.icon_pause
                    } else {
                        CorePresentationRes.drawable.icon_music_note
                    },
                ),
                tint = Color.White.copy(alpha = 0.86f),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = track.title,
                    color = titleColor,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                track.qualityBadgeType()?.let { badgeType ->
                    QualityBadge(type = badgeType)
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
            text = durationLabel(track.durationMs),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
        )
        if (track.mediaId != null) {
            Icon(
                painter = painterResource(CorePresentationRes.drawable.icon_download),
                tint = MiuixTheme.colorScheme.primary,
                contentDescription = "Download",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDownload)
                    .padding(4.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryAlbumGrid(albums: List<LibraryAlbumItem>) {
    if (albums.isEmpty()) {
        LibraryCategoryEmpty(LibraryCategory.Albums)
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 640.dp) 3 else 2
        val columnWidth = (maxWidth - 16.dp * (columns - 1)) / columns
        val cardWidth = if (columnWidth < 160.dp) columnWidth else 160.dp
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            maxItemsInEachRow = columns,
        ) {
            albums.forEachIndexed { index, album ->
                Box(
                    modifier = Modifier.width(columnWidth),
                ) {
                    LibraryAlbumCard(album, index, cardWidth)
                }
            }
        }
    }
}

@Composable
private fun LibraryAlbumCard(album: LibraryAlbumItem, index: Int, width: Dp) {
    val first = libraryGradientColors[index % libraryGradientColors.size]
    val second = libraryGradientColors[(index + 1) % libraryGradientColors.size]
    Column(
        modifier = Modifier.width(width),
    ) {
        val artworkShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .size(width)
                .shadow(8.dp, artworkShape, clip = false)
                .clip(artworkShape)
                .background(Brush.linearGradient(listOf(first, second))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CorePresentationRes.drawable.icon_album),
                tint = Color.White.copy(alpha = 0.24f),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = album.name,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = album.year?.toString() ?: "Unknown Year",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryArtistGrid(artists: List<LibraryArtistItem>) {
    if (artists.isEmpty()) {
        LibraryCategoryEmpty(LibraryCategory.Artists)
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = 3
        val cellWidth = (maxWidth - 24.dp) / columns
        val cardWidth = if (cellWidth < 128.dp) cellWidth else 128.dp
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            maxItemsInEachRow = columns,
        ) {
            artists.forEachIndexed { index, artist ->
                Box(
                    modifier = Modifier.width(cellWidth),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier.width(cardWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(cardWidth)
                                .shadow(8.dp, CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            libraryGradientColors[index % libraryGradientColors.size],
                                            libraryGradientColors[(index + 1) % libraryGradientColors.size],
                                        ),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = artist.name.initials(),
                                color = Color.White.copy(alpha = 0.90f),
                                style = MiuixTheme.textStyles.title2,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = artist.name,
                            color = MiuixTheme.colorScheme.onBackground,
                            style = MiuixTheme.textStyles.body1,
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryGenreGrid() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - 12.dp) / 2
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            libraryGenres.forEachIndexed { index, genre ->
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    libraryGradientColors[index % libraryGradientColors.size],
                                    libraryGradientColors[(index + 1) % libraryGradientColors.size],
                                ),
                            ),
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        text = genre,
                        color = Color.White,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryPlaylistGrid() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = 2
        val cellWidth = (maxWidth - 16.dp) / columns
        val cardWidth = if (cellWidth < 160.dp) cellWidth else 160.dp
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            maxItemsInEachRow = columns,
        ) {
            libraryPlaylists.forEachIndexed { index, playlist ->
                Box(
                    modifier = Modifier.width(cellWidth),
                ) {
                    Column(modifier = Modifier.width(cardWidth)) {
                    val artworkShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .size(cardWidth)
                            .shadow(8.dp, artworkShape, clip = false)
                            .clip(artworkShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        libraryGradientColors[index % libraryGradientColors.size],
                                        libraryGradientColors[(index + 1) % libraryGradientColors.size],
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(CorePresentationRes.drawable.icon_mode_list),
                            tint = Color.White.copy(alpha = 0.32f),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = playlist.title,
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.trackCount} songs · ${playlist.duration}",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySourcesGrid() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        librarySources.forEachIndexed { index, source ->
            LibrarySourceCard(source = source, index = index)
        }
        DesignTextButton(
            text = "+  Add Source",
            variant = DesignTextButtonVariant.Tonal,
            size = DesignTextButtonSize.Medium,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LibrarySourceCard(
    source: LibrarySourceDemo,
    index: Int,
) {
    val statusColor = when (source.status) {
        "Connected" -> DesignPalette.SupportGreen
        "Syncing" -> DesignPalette.SupportBlue
        "Error" -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.onBackgroundVariant
    }
    val sourcePainter = when (source.type) {
        "Local" -> painterResource(CorePresentationRes.drawable.icon_folder)
        "OneDrive" -> painterResource(CorePresentationRes.drawable.icon_onedrive)
        "Plex" -> painterResource(CorePresentationRes.drawable.icon_album)
        "Jellyfin",
        "Navidrome" -> painterResource(CorePresentationRes.drawable.icon_music_note)
        else -> painterResource(CorePresentationRes.drawable.icon_cloud)
    }
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .border(1.dp, MiuixTheme.colorScheme.outline, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                libraryGradientColors[index % libraryGradientColors.size],
                                libraryGradientColors[(index + 1) % libraryGradientColors.size],
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = sourcePainter,
                    tint = Color.White,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = source.type,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = source.status,
                    color = statusColor,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibrarySourceMetric(
                label = "Storage",
                value = source.storage,
                modifier = Modifier.weight(1f),
            )
            LibrarySourceMetric(
                label = "Tracks",
                value = source.trackCount.groupedCount(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(DesignTokens.shapes.sm))
                    .background(MiuixTheme.colorScheme.tertiaryContainer)
                    .clickable(onClick = {})
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(CorePresentationRes.drawable.icon_mode_repeat),
                    tint = MiuixTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sync",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LibrarySourceAction(
                painter = painterResource(CorePresentationRes.drawable.icon_log),
                contentDescription = "Logs",
            )
            LibrarySourceAction(
                painter = painterResource(CorePresentationRes.drawable.icon_adjust),
                contentDescription = "Settings",
            )
            LibrarySourceAction(
                painter = painterResource(CorePresentationRes.drawable.icon_vertialcal_more),
                contentDescription = "More",
            )
        }
    }
}

@Composable
private fun LibrarySourceMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote2,
        )
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LibrarySourceAction(
    painter: Painter,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(DesignTokens.shapes.sm))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LibraryEmptyState() {
    DesignEmptyState(
        title = "No tracks in library.",
        message = "Add a source or scan local music to start building your collection.",
        marker = "M",
    )
}

@Composable
private fun LibraryFoldersState(onImportFolder: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        libraryFolders.forEach { path ->
            val name = path.substringAfterLast('/')
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = {})
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(CorePresentationRes.drawable.icon_folder),
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = path,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    painter = painterResource(CorePresentationRes.drawable.icon_chevron_right),
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DesignTextButton(
            text = "+  Add Folder",
            variant = DesignTextButtonVariant.Tonal,
            size = DesignTextButtonSize.Medium,
            onClick = onImportFolder,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LibraryCategoryEmpty(category: LibraryCategory) {
    val message = when (category) {
        LibraryCategory.Sources -> "Connect local, WebDAV, OneDrive or server sources from Settings."
        LibraryCategory.Downloads -> "Downloaded songs will appear here."
        LibraryCategory.Playlists -> "Create a playlist to organize your music."
        else -> "Your ${category.label.lowercase()} will appear here."
    }
    DesignEmptyState(
        title = category.label,
        message = message,
        marker = category.label.take(1),
    )
}

internal fun LibraryTrackItem.lazyListKey(index: Int): String {
    val mediaKey = mediaId?.let { media ->
        "${media.sourceId.value}:${media.mediaType}:${media.remoteId}"
    } ?: "no-media-id"
    return "library-track-$id-$mediaKey-$index"
}

private fun durationLabel(durationMs: Long?): String {
    if (durationMs == null) return "--:--"
    val totalSeconds = durationMs / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

private fun String.initials(): String = split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { take(2).uppercase() }

private fun Int.groupedCount(): String = toString()
    .reversed()
    .chunked(3)
    .joinToString(",")
    .reversed()

private enum class LibraryCategory(val label: String) {
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists"),
    Genres("Genres"),
    Folders("Folders"),
    Playlists("Playlists"),
    Favorites("Favorites"),
    Downloads("Downloads"),
    History("History"),
    RecentlyAdded("Recently Added"),
    RecentlyPlayed("Recently Played"),
    Lossless("Lossless"),
    HiRes("Hi-Res"),
    Sources("Sources"),
}

private val libraryGradientColors = listOf(
    DesignPalette.Primary,
    DesignPalette.Secondary,
    DesignPalette.SupportBlue,
    DesignPalette.SupportOrange,
    DesignPalette.SupportGreen,
    DesignPalette.SupportYellow,
)

private val libraryGenres = listOf(
    "Electronic",
    "Ambient",
    "Synthwave",
    "Techno",
    "IDM",
    "Post-Rock",
    "Shoegaze",
    "Experimental",
    "Jazz",
    "Classical",
)

private data class LibraryPlaylistDemo(
    val title: String,
    val trackCount: Int,
    val duration: String,
)

private val libraryPlaylists = listOf(
    LibraryPlaylistDemo("Evening Frequencies", 24, "1h 32m"),
    LibraryPlaylistDemo("Spatial Audio Mix", 18, "1h 08m"),
    LibraryPlaylistDemo("Deep Focus", 32, "2h 15m"),
    LibraryPlaylistDemo("Night Drive", 20, "1h 22m"),
    LibraryPlaylistDemo("Sunrise Protocol", 16, "58m"),
    LibraryPlaylistDemo("System Override", 28, "1h 45m"),
)

private val libraryFolders = listOf(
    "/Music/Electronic",
    "/Music/Ambient",
    "/Downloads/Music",
    "/Synced/WebDAV",
    "/SD Card/Music",
)

private data class LibrarySourceDemo(
    val name: String,
    val type: String,
    val status: String,
    val storage: String,
    val trackCount: Int,
)

private val librarySources = listOf(
    LibrarySourceDemo("Local Storage", "Local", "Connected", "24.6 GB", 1284),
    LibrarySourceDemo("Personal NAS", "WebDAV", "Connected", "128 GB", 5820),
    LibrarySourceDemo("OneDrive Music", "OneDrive", "Syncing", "8.2 GB", 342),
    LibrarySourceDemo("Jellyfin Home", "Jellyfin", "Error", "512 GB", 18200),
    LibrarySourceDemo("Plex Server", "Plex", "Idle", "256 GB", 8400),
    LibrarySourceDemo("Navidrome", "Navidrome", "Connected", "64 GB", 3100),
)

private val demoLibraryTracks = listOf(
    LibraryTrackItem(1, "Midnight Cascade", "Luna Waves · Tidal Drift", 222_000),
    LibraryTrackItem(2, "Neon Undertow", "Prism Circuit · Voltage Dreams", 258_000),
    LibraryTrackItem(3, "Silver Tide", "Coastal Drift · Open Water", 235_000),
    LibraryTrackItem(4, "Aurora Sequence", "Polar Echo · Northern Lights", 302_000),
    LibraryTrackItem(5, "Depth Protocol", "Ocean Syntax · Subsonic", 210_000),
    LibraryTrackItem(6, "Glass Architecture", "Fractal Mind · Prism", 284_000),
    LibraryTrackItem(7, "Resonance Fields", "Wave Function · Quantum", 195_000),
    LibraryTrackItem(8, "Liminal Space", "Threshold · Between", 330_000),
)

private fun LibraryTrackItem.qualityBadgeType(): QualityBadgeType? = when (title) {
    "Midnight Cascade",
    "Liminal Space" -> QualityBadgeType.HiRes
    "Neon Undertow",
    "Glass Architecture" -> QualityBadgeType.Flac
    "Aurora Sequence" -> QualityBadgeType.DolbyAtmos
    else -> null
}

private fun LibraryCategory.selectTracks(tracks: List<LibraryTrackItem>): List<LibraryTrackItem> = when (this) {
    LibraryCategory.Favorites -> tracks.filterIndexed { index, _ -> index % 2 == 0 }
    LibraryCategory.History -> tracks.reversed()
    LibraryCategory.RecentlyAdded,
    LibraryCategory.RecentlyPlayed -> tracks.take(6)
    LibraryCategory.Lossless -> tracks.filter { it.title == "Neon Undertow" || it.title == "Glass Architecture" }
    LibraryCategory.HiRes -> tracks.filter { it.title == "Midnight Cascade" || it.title == "Liminal Space" }
    else -> tracks.take(12)
}
