package com.github.tidetunes.feature.browse.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppEmptyState
import com.github.tidetunes.core.presentation.components.AppErrorState
import com.github.tidetunes.core.presentation.components.AppChip
import com.github.tidetunes.core.presentation.components.AppLoadingIndicator
import com.github.tidetunes.core.presentation.components.AppSectionHeader
import com.github.tidetunes.core.presentation.components.AppTopBar
import com.github.tidetunes.core.presentation.media.ArtworkImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseScreen(
    state: BrowseState,
    onAction: (BrowseAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Browse", navigationIcon = null)
        when {
            state.isLoading -> AppLoadingIndicator()
            state.error != null -> AppErrorState(message = state.error, onRetry = { onAction(BrowseAction.Retry) })
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (state.albums.isNotEmpty()) {
                    item { AppSectionHeader(title = "Albums") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.albums, key = { it.id }) { album ->
                                BrowseAlbumCard(
                                    album = album,
                                    onClick = { onAction(BrowseAction.NavigateToAlbum(album.id)) },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                if (state.artists.isNotEmpty()) {
                    item { AppSectionHeader(title = "Artists") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.artists, key = { it.id }) { artist ->
                                BrowseArtistCard(
                                    artist = artist,
                                    onClick = { onAction(BrowseAction.NavigateToArtist(artist.id)) },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                if (state.genres.isNotEmpty()) {
                    item { AppSectionHeader(title = "Genres") }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            state.genres.forEach { genre ->
                                AppChip(
                                    label = genre,
                                    onClick = { onAction(BrowseAction.NavigateToGenre(genre)) },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                if (state.albums.isEmpty() && state.artists.isEmpty() && state.genres.isEmpty()) {
                    item { AppEmptyState(message = "No content to browse. Import some music first.") }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun BrowseAlbumCard(
    album: BrowseAlbumItem,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.width(140.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(8.dp)),
        ) {
            ArtworkImage(artwork = album.artwork, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(text = album.name, style = MiuixTheme.textStyles.body2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        album.year?.let { year ->
            Text(text = "$year · ${album.trackCount} tracks", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1)
        }
    }
}

@Composable
private fun BrowseArtistCard(
    artist: BrowseArtistItem,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = artist.name, style = MiuixTheme.textStyles.body2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = "${artist.trackCount} tracks", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
