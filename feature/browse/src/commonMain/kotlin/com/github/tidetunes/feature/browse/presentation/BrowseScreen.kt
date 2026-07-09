package com.github.tidetunes.feature.browse.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideChipSection
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideSectionHeader
import com.github.tidetunes.core.presentation.components.TideSectionHeaderMetadataTone
import com.github.tidetunes.core.presentation.components.TideSectionHeaderVariant
import com.github.tidetunes.core.presentation.components.TideStatusCard
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BrowseScreen(
    state: BrowseState,
    onAction: (BrowseAction) -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TidePageHeader(
                title = "Browse",
                subtitle = "${state.albums.size} albums / ${state.artists.size} artists / ${state.genres.size} genres",
            )
            when {
                state.isLoading -> TideStatusCard(title = "Loading browse", message = "Scanning library facets", loading = true, modifier = Modifier.weight(1f))
                state.error != null -> TideStatusCard(title = "Browse unavailable", message = state.error, actionText = "Retry", onAction = { onAction(BrowseAction.Retry) }, modifier = Modifier.weight(1f))
                else -> BrowseContent(state = state, onAction = onAction, modifier = Modifier.weight(1f))
            }
        }
    }
}

internal fun BrowseAlbumItem.lazyListKey(index: Int): String = "browse-album-$index-$id"
internal fun BrowseArtistItem.lazyListKey(index: Int): String = "browse-artist-$index-$id"

@Composable
private fun BrowseContent(state: BrowseState, onAction: (BrowseAction) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (state.albums.isNotEmpty()) {
            item { BrowseSectionTitle(title = "Albums", count = state.albums.size) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(state.albums, key = { index, album -> album.lazyListKey(index) }) { _, album ->
                        BrowseAlbumCard(album = album, onClick = { onAction(BrowseAction.NavigateToAlbum(album.id)) })
                    }
                }
            }
        }
        if (state.artists.isNotEmpty()) {
            item { BrowseSectionTitle(title = "Artists", count = state.artists.size) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(state.artists, key = { index, artist -> artist.lazyListKey(index) }) { _, artist ->
                        BrowseArtistCard(artist = artist, onClick = { onAction(BrowseAction.NavigateToArtist(artist.id)) })
                    }
                }
            }
        }
        if (state.genres.isNotEmpty()) {
            item {
                TideChipSection(
                    title = "Genres",
                    labels = state.genres,
                    metadata = "${state.genres.size}",
                    metadataTone = TideSectionHeaderMetadataTone.Accent,
                    onLabelClick = { genre -> onAction(BrowseAction.NavigateToGenre(genre)) },
                )
            }
        }
        if (state.albums.isEmpty() && state.artists.isEmpty() && state.genres.isEmpty()) {
            item { TideStatusCard(title = "No browse content", message = "Import music first") }
        }
    }
}

@Composable
private fun BrowseSectionTitle(title: String, count: Int) {
    TideSectionHeader(title = title, metadata = "$count", variant = TideSectionHeaderVariant.Compact, metadataTone = TideSectionHeaderMetadataTone.Accent)
}

@Composable
private fun BrowseAlbumCard(album: BrowseAlbumItem, onClick: () -> Unit) {
    val shapes = TideTunesTokens.shapes
    TideCardSurface(modifier = Modifier.width(156.dp), contentPadding = PaddingValues(10.dp), fillMaxWidth = false, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtworkImage(artwork = album.artwork, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(136.dp).clip(RoundedCornerShape(shapes.md)))
            Text(text = album.name, color = MiuixTheme.colorScheme.onSurface, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = albumMeta(album), color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BrowseArtistCard(artist: BrowseArtistItem, onClick: () -> Unit) {
    val shapes = TideTunesTokens.shapes
    TideCardSurface(modifier = Modifier.width(140.dp), contentPadding = PaddingValues(12.dp), fillMaxWidth = false, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(86.dp).clip(RoundedCornerShape(shapes.full)).background(TideTunesBrand.Secondary), contentAlignment = Alignment.Center) {
                Text(text = artist.name.take(1).uppercase(), style = MiuixTheme.textStyles.title2, color = MiuixTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(text = artist.name, color = MiuixTheme.colorScheme.onSurface, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "${artist.trackCount} tracks", color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1, maxLines = 1)
        }
    }
}

private fun albumMeta(album: BrowseAlbumItem): String =
    if (album.year == null) "${album.trackCount} tracks" else "${album.year} / ${album.trackCount} tracks"
