package com.github.tidetunes.feature.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideIconBadge
import com.github.tidetunes.core.presentation.components.TideIconBadgeVariant
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideTabItem
import com.github.tidetunes.core.presentation.components.TideTabs
import com.github.tidetunes.core.presentation.components.TideTabsVariant
import com.github.tidetunes.core.presentation.components.TideEmptyState
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
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

    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded
        LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TidePageHeader(
                title = "Library",
                subtitle = "Songs, albums, artists and sources",
            ) {
                Text(
                    text = "${state.tracks.size} tracks",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                )
            }
        }
        item {
            LibraryCategoryStrip(
                trackCount = state.tracks.size,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    selectedCategory = category
                },
            )
        }
        when (selectedCategory) {
            LibraryCategory.Songs -> {
                if (state.tracks.isEmpty()) {
                    item {
                        LibraryEmptyState()
                    }
                } else {
                    itemsIndexed(
                        items = state.tracks,
                        key = { index, track -> track.lazyListKey(index) },
                    ) { _, track ->
                        LibraryTrackCard(
                            track = track,
                            playing = track.id == currentPlayingTrackId,
                            onPlay = {
                                onAction(LibraryAction.PlayTrack(track.id))
                            },
                            onDownload = {
                                onAction(LibraryAction.DownloadTrack(track))
                            },
                        )
                    }
                }
            }
            LibraryCategory.Folders -> {
                item {
                    LibraryFoldersState(
                        onImportFolder = onNavigateToLibraryFolderImport,
                    )
                }
            }
            else -> Unit
        }
    }
    }
}

@Composable
private fun LibraryCategoryStrip(
    trackCount: Int,
    selectedCategory: LibraryCategory,
    onCategorySelected: (LibraryCategory) -> Unit,
) {
    val categories = LibraryCategory.entries

    TideTabs(
        items = categories.map { category ->
            TideTabItem(
                label = category.label,
                badge = trackCount.toString().takeIf { category == LibraryCategory.Songs },
                enabled = category.enabled,
            )
        },
        selectedIndex = categories.indexOf(selectedCategory),
        onSelectedIndexChange = { index ->
            onCategorySelected(categories[index])
        },
        variant = TideTabsVariant.Pill,
    )
}

@Composable
private fun LibraryEmptyState() {
    TideEmptyState(
        title = "No tracks in library.",
        message = "Add a source or scan local music to start building your collection.",
        marker = "M",
    )
}

@Composable
private fun LibraryFoldersState(
    onImportFolder: () -> Unit,
) {
    TideEmptyState(
        title = "Browse folders",
        message = "Import a music folder to browse albums and tracks by directory.",
        marker = "F",
        action = {
            TideTextButton(
                text = "Add Folder",
                variant = TideTextButtonVariant.Primary,
                size = TideTextButtonSize.Medium,
                onClick = onImportFolder,
            )
        },
    )
}

@Composable
private fun LibraryTrackCard(
    track: LibraryTrackItem,
    playing: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes
    val primaryColor = MiuixTheme.colorScheme.primary
    val titleColor = if (playing) primaryColor else MiuixTheme.colorScheme.onSurface
    val secondaryColor = if (playing) primaryColor else MiuixTheme.colorScheme.onSurfaceVariantSummary

    TideCardSurface(
        cornerRadius = shapes.md,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        backgroundColor = if (playing) {
            MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.70f)
        } else {
            null
        },
        borderColor = if (playing) {
            primaryColor.copy(alpha = 0.38f)
        } else {
            null
        },
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TideIconBadge(
                marker = if (playing) ">" else "M",
                variant = if (playing) {
                    TideIconBadgeVariant.Surface
                } else {
                    TideIconBadgeVariant.Neutral
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = track.title,
                    color = titleColor,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist ?: "--",
                    color = secondaryColor,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (track.mediaId != null) {
                    TideTextButton(
                        text = "Download",
                        variant = TideTextButtonVariant.Primary,
                        size = TideTextButtonSize.Small,
                        onClick = onDownload,
                    )
                }
                Text(
                    text = durationLabel(track.durationMs),
                    color = secondaryColor,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
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

private enum class LibraryCategory(
    val label: String,
    val enabled: Boolean,
) {
    Songs("Songs", true),
    Albums("Albums", false),
    Artists("Artists", false),
    Genres("Genres", false),
    Folders("Folders", true),
    Playlists("Playlists", false),
    Favorites("Favorites", false),
    Downloads("Downloads", false),
    History("History", false),
    RecentlyAdded("Recently Added", false),
    RecentlyPlayed("Recently Played", false),
    Lossless("Lossless", false),
    HiRes("Hi-Res", false),
    Sources("Sources", false),
}
