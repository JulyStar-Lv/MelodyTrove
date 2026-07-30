package io.github.julystar.musicapp.feature.library.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.repository.LibraryFolderItem
import io.github.julystar.musicapp.core.presentation.components.DesignCardSurface
import io.github.julystar.musicapp.core.presentation.components.DesignEmptyState
import io.github.julystar.musicapp.core.presentation.components.DesignPageHeader
import io.github.julystar.musicapp.core.presentation.components.DesignStatusCard
import io.github.julystar.musicapp.core.presentation.components.DesignTabItem
import io.github.julystar.musicapp.core.presentation.components.DesignTabs
import io.github.julystar.musicapp.core.presentation.components.DesignTabsVariant
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import musicapp.core.presentation.generated.resources.Res as CorePresentationRes
import musicapp.core.presentation.generated.resources.icon_adjust
import musicapp.core.presentation.generated.resources.icon_album
import musicapp.core.presentation.generated.resources.icon_chevron_right
import musicapp.core.presentation.generated.resources.icon_download
import musicapp.core.presentation.generated.resources.icon_folder
import musicapp.core.presentation.generated.resources.icon_mode_list
import musicapp.core.presentation.generated.resources.icon_music_note
import musicapp.core.presentation.generated.resources.icon_pause
import musicapp.feature.library.generated.resources.Res
import musicapp.feature.library.generated.resources.library_add_folder
import musicapp.feature.library.generated.resources.library_category_albums
import musicapp.feature.library.generated.resources.library_category_artists
import musicapp.feature.library.generated.resources.library_category_downloads
import musicapp.feature.library.generated.resources.library_category_favorites
import musicapp.feature.library.generated.resources.library_category_folders
import musicapp.feature.library.generated.resources.library_category_genres
import musicapp.feature.library.generated.resources.library_category_hires
import musicapp.feature.library.generated.resources.library_category_history
import musicapp.feature.library.generated.resources.library_category_lossless
import musicapp.feature.library.generated.resources.library_category_playlists
import musicapp.feature.library.generated.resources.library_category_recently_added
import musicapp.feature.library.generated.resources.library_category_recently_played
import musicapp.feature.library.generated.resources.library_category_songs
import musicapp.feature.library.generated.resources.library_category_sources
import musicapp.feature.library.generated.resources.library_download
import musicapp.feature.library.generated.resources.library_duration_hours
import musicapp.feature.library.generated.resources.library_duration_minutes
import musicapp.feature.library.generated.resources.library_empty_albums
import musicapp.feature.library.generated.resources.library_empty_artists
import musicapp.feature.library.generated.resources.library_empty_downloads
import musicapp.feature.library.generated.resources.library_empty_favorites
import musicapp.feature.library.generated.resources.library_empty_folders
import musicapp.feature.library.generated.resources.library_empty_genres
import musicapp.feature.library.generated.resources.library_empty_hires
import musicapp.feature.library.generated.resources.library_empty_history
import musicapp.feature.library.generated.resources.library_empty_lossless
import musicapp.feature.library.generated.resources.library_empty_message
import musicapp.feature.library.generated.resources.library_empty_playlists
import musicapp.feature.library.generated.resources.library_empty_recently_added
import musicapp.feature.library.generated.resources.library_empty_recently_played
import musicapp.feature.library.generated.resources.library_empty_sources
import musicapp.feature.library.generated.resources.library_empty_title
import musicapp.feature.library.generated.resources.library_filter
import musicapp.feature.library.generated.resources.library_folder_track_count
import musicapp.feature.library.generated.resources.library_list_view
import musicapp.feature.library.generated.resources.library_loading
import musicapp.feature.library.generated.resources.library_playlist_track_count
import musicapp.feature.library.generated.resources.library_retry
import musicapp.feature.library.generated.resources.library_song_summary
import musicapp.feature.library.generated.resources.library_title
import musicapp.feature.library.generated.resources.library_unavailable
import musicapp.feature.library.generated.resources.library_unknown_artist
import musicapp.feature.library.generated.resources.library_unknown_year
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
    val categories = LibraryCategory.entries
    val categoryLabels = categories.map { stringResource(it.labelRes) }

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
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showPageHeader) {
                item {
                    DesignPageHeader(
                        title = stringResource(Res.string.library_title),
                        subtitle = null,
                    )
                }
            }
            item {
                DesignTabs(
                    items = categoryLabels.map { label -> DesignTabItem(label = label) },
                    selectedIndex = categories.indexOf(selectedCategory),
                    onSelectedIndexChange = { index -> selectedCategory = categories[index] },
                    variant = DesignTabsVariant.Pill,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            when (selectedCategory) {
                LibraryCategory.Songs -> {
                    item { LibrarySongHeader(state.tracks) }
                    if (state.tracks.isEmpty()) {
                        item { LibraryRootEmptyState() }
                    } else {
                        itemsIndexed(
                            items = state.tracks,
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

                LibraryCategory.Albums -> {
                    if (state.albums.isEmpty()) {
                        item { LibraryCategoryEmpty(selectedCategory) }
                    } else {
                        items(state.albums, key = { album -> album.id }) { album ->
                            LibraryAlbumRow(album)
                        }
                    }
                }

                LibraryCategory.Artists -> {
                    if (state.artists.isEmpty()) {
                        item { LibraryCategoryEmpty(selectedCategory) }
                    } else {
                        items(state.artists, key = { artist -> artist.id }) { artist ->
                            LibraryArtistRow(artist)
                        }
                    }
                }

                LibraryCategory.Genres -> when (val repositoryState = state.genreNames) {
                    RepositoryState.Loading -> item { LibraryLoadingState() }
                    is RepositoryState.Error -> item { LibraryErrorState { onAction(LibraryAction.Refresh) } }
                    is RepositoryState.Empty -> item { LibraryCategoryEmpty(selectedCategory) }
                    is RepositoryState.Loaded -> {
                        if (repositoryState.data.isEmpty()) {
                            item { LibraryCategoryEmpty(selectedCategory) }
                        } else {
                            items(repositoryState.data, key = { genre -> genre }) { genre ->
                                LibraryNavigationRow(
                                    title = genre,
                                    painter = painterResource(CorePresentationRes.drawable.icon_music_note),
                                    onClick = { onAction(LibraryAction.SelectGenre(genre)) },
                                )
                            }
                        }
                    }
                }

                LibraryCategory.Folders -> when (val repositoryState = state.folders) {
                    RepositoryState.Loading -> item { LibraryLoadingState() }
                    is RepositoryState.Error -> item { LibraryErrorState { onAction(LibraryAction.Refresh) } }
                    is RepositoryState.Empty -> item { LibraryCategoryEmpty(selectedCategory) }
                    is RepositoryState.Loaded -> {
                        if (repositoryState.data.isEmpty()) {
                            item { LibraryCategoryEmpty(selectedCategory) }
                        } else {
                            items(repositoryState.data, key = { folder -> folder.path }) { folder ->
                                LibraryFolderRow(
                                    folder = folder,
                                    onClick = { onAction(LibraryAction.BrowseFolder(folder.path)) },
                                )
                            }
                        }
                    }
                    item {
                        DesignTextButton(
                            text = stringResource(Res.string.library_add_folder),
                            variant = DesignTextButtonVariant.Tonal,
                            size = DesignTextButtonSize.Medium,
                            onClick = onNavigateToLibraryFolderImport,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                LibraryCategory.Playlists -> {
                    if (state.playlists.isEmpty()) {
                        item { LibraryCategoryEmpty(selectedCategory) }
                    } else {
                        items(state.playlists, key = { playlist -> playlist.id }) { playlist ->
                            LibraryPlaylistRow(playlist)
                        }
                    }
                }

                LibraryCategory.RecentlyAdded -> {
                    val tracks = state.tracks.take(50)
                    if (tracks.isEmpty()) {
                        item { LibraryCategoryEmpty(selectedCategory) }
                    } else {
                        itemsIndexed(tracks, key = { index, track -> track.lazyListKey(index) }) { index, track ->
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

                LibraryCategory.Sources -> item { LibraryCategoryEmpty(selectedCategory) }

                else -> {
                    val repositoryState = selectedCategory.trackRepositoryState(state)
                    when (repositoryState) {
                        null -> item { LibraryCategoryEmpty(selectedCategory) }
                        RepositoryState.Loading -> item { LibraryLoadingState() }
                        is RepositoryState.Error -> item { LibraryErrorState { onAction(LibraryAction.Refresh) } }
                        is RepositoryState.Empty -> item { LibraryCategoryEmpty(selectedCategory) }
                        is RepositoryState.Loaded -> {
                            if (repositoryState.data.isEmpty()) {
                                item { LibraryCategoryEmpty(selectedCategory) }
                            } else {
                                itemsIndexed(
                                    repositoryState.data,
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
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySongHeader(tracks: List<LibraryTrackItem>) {
    val totalMinutes = tracks.mapNotNull { it.durationMs }.sum() / 60_000L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.library_song_summary, tracks.size, totalMinutes),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.body2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LibraryRoundAction(
                painter = painterResource(CorePresentationRes.drawable.icon_adjust),
                contentDescription = stringResource(Res.string.library_filter),
            )
            LibraryRoundAction(
                painter = painterResource(CorePresentationRes.drawable.icon_mode_list),
                contentDescription = stringResource(Res.string.library_list_view),
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
            .size(36.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            contentDescription = contentDescription,
            modifier = Modifier.size(17.dp),
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
                    if (playing) CorePresentationRes.drawable.icon_pause
                    else CorePresentationRes.drawable.icon_music_note,
                ),
                tint = Color.White.copy(alpha = 0.86f),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = titleColor,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist ?: stringResource(Res.string.library_unknown_artist),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = durationClockLabel(track.durationMs),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
        )
        if (track.mediaId != null) {
            Icon(
                painter = painterResource(CorePresentationRes.drawable.icon_download),
                tint = MiuixTheme.colorScheme.primary,
                contentDescription = stringResource(Res.string.library_download),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDownload)
                    .padding(6.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun LibraryAlbumRow(album: LibraryAlbumItem) {
    DesignCardSurface(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LibraryArtworkTile(painterResource(CorePresentationRes.drawable.icon_album))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.year?.toString() ?: stringResource(Res.string.library_unknown_year),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
}

@Composable
private fun LibraryArtistRow(artist: LibraryArtistItem) {
    DesignCardSurface(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(DesignPalette.Primary, DesignPalette.Secondary))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = artist.name.initials(),
                    color = Color.White,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = artist.name,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LibraryFolderRow(folder: LibraryFolderItem, onClick: () -> Unit) {
    LibraryNavigationRow(
        title = folder.displayName.ifBlank { folder.path.substringAfterLast('/') },
        subtitle = stringResource(Res.string.library_folder_track_count, folder.trackCount),
        painter = painterResource(CorePresentationRes.drawable.icon_folder),
        onClick = onClick,
    )
}

@Composable
private fun LibraryPlaylistRow(playlist: PlaylistSummary) {
    LibraryNavigationRow(
        title = playlist.title,
        subtitle = buildString {
            append(stringResource(Res.string.library_playlist_track_count, playlist.musicCount))
            append(" · ")
            append(libraryDurationLabel(playlist.durationMs))
        },
        painter = painterResource(CorePresentationRes.drawable.icon_mode_list),
        onClick = {},
    )
}

@Composable
private fun LibraryNavigationRow(
    title: String,
    painter: Painter,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LibraryArtworkTile(painter)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            painter = painterResource(CorePresentationRes.drawable.icon_chevron_right),
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LibraryArtworkTile(painter: Painter) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(DesignPalette.Primary, DesignPalette.Secondary))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            tint = Color.White.copy(alpha = 0.84f),
            contentDescription = null,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun LibraryRootEmptyState() {
    DesignEmptyState(
        title = stringResource(Res.string.library_empty_title),
        message = stringResource(Res.string.library_empty_message),
        marker = "M",
    )
}

@Composable
private fun LibraryCategoryEmpty(category: LibraryCategory) {
    val label = stringResource(category.labelRes)
    DesignEmptyState(
        title = label,
        message = stringResource(category.emptyMessageRes),
        marker = label.take(1),
    )
}

@Composable
private fun LibraryLoadingState() {
    DesignStatusCard(
        title = stringResource(Res.string.library_loading),
        message = stringResource(Res.string.library_title),
        loading = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    )
}

@Composable
private fun LibraryErrorState(onRetry: () -> Unit) {
    DesignStatusCard(
        title = stringResource(Res.string.library_unavailable),
        message = stringResource(Res.string.library_empty_message),
        actionText = stringResource(Res.string.library_retry),
        onAction = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    )
}

private fun LibraryCategory.trackRepositoryState(
    state: LibraryState,
): RepositoryState<List<LibraryTrackItem>>? = when (this) {
    LibraryCategory.Favorites -> state.favorites
    LibraryCategory.Downloads -> state.downloads
    LibraryCategory.History,
    LibraryCategory.RecentlyPlayed -> state.history
    LibraryCategory.Lossless -> state.lossless
    LibraryCategory.HiRes -> state.hiRes
    else -> null
}

private fun LibraryTrackItem.lazyListKey(index: Int): String {
    val mediaKey = mediaId?.let { media ->
        "${media.sourceId.value}:${media.mediaType}:${media.remoteId}"
    } ?: "no-media-id"
    return "library-track-$id-$mediaKey-$index"
}

private fun durationClockLabel(durationMs: Long?): String {
    if (durationMs == null) return "--:--"
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun libraryDurationLabel(durationMs: Long): String {
    val totalMinutes = (durationMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        stringResource(Res.string.library_duration_hours, hours, minutes)
    } else {
        stringResource(Res.string.library_duration_minutes, minutes)
    }
}

private fun String.initials(): String = split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { take(2).uppercase() }

private enum class LibraryCategory(
    val labelRes: StringResource,
    val emptyMessageRes: StringResource,
) {
    Songs(Res.string.library_category_songs, Res.string.library_empty_message),
    Albums(Res.string.library_category_albums, Res.string.library_empty_albums),
    Artists(Res.string.library_category_artists, Res.string.library_empty_artists),
    Genres(Res.string.library_category_genres, Res.string.library_empty_genres),
    Folders(Res.string.library_category_folders, Res.string.library_empty_folders),
    Playlists(Res.string.library_category_playlists, Res.string.library_empty_playlists),
    Favorites(Res.string.library_category_favorites, Res.string.library_empty_favorites),
    Downloads(Res.string.library_category_downloads, Res.string.library_empty_downloads),
    History(Res.string.library_category_history, Res.string.library_empty_history),
    RecentlyAdded(Res.string.library_category_recently_added, Res.string.library_empty_recently_added),
    RecentlyPlayed(Res.string.library_category_recently_played, Res.string.library_empty_recently_played),
    Lossless(Res.string.library_category_lossless, Res.string.library_empty_lossless),
    HiRes(Res.string.library_category_hires, Res.string.library_empty_hires),
    Sources(Res.string.library_category_sources, Res.string.library_empty_sources),
}

private val libraryGradientColors = listOf(
    DesignPalette.Primary,
    DesignPalette.Secondary,
    DesignPalette.SupportBlue,
    DesignPalette.SupportOrange,
    DesignPalette.SupportGreen,
    DesignPalette.SupportYellow,
)
