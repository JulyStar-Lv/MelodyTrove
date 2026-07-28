package io.github.julystar.musicapp.feature.artist.presentation

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.presentation.components.DesignCardSurface
import io.github.julystar.musicapp.core.presentation.components.DesignDetailHeaderSurface
import io.github.julystar.musicapp.core.presentation.components.LocalDesignBottomContentInset
import io.github.julystar.musicapp.core.presentation.components.DesignSectionHeader
import io.github.julystar.musicapp.core.presentation.components.DesignStatusCard
import io.github.julystar.musicapp.core.presentation.components.DesignTrackNumberBadge
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.media.ArtworkImage
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ArtistScreen(
    state: ArtistState,
    onAction: (ArtistAction) -> Unit,
) {
    val spacing = DesignTokens.spacing
    val bottomContentInset = LocalDesignBottomContentInset.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
        ) {
            when {
                state.isLoading -> DesignStatusCard(
                    title = "Loading artist",
                    message = state.name.ifBlank { "Artist" },
                    loading = true,
                    loadingColor = DesignPalette.Secondary,
                    modifier = Modifier.weight(1f),
                )
                state.error != null -> DesignStatusCard(
                    title = "Artist unavailable",
                    message = state.error,
                    modifier = Modifier.weight(1f),
                    actionText = "Retry",
                    onAction = { onAction(ArtistAction.Retry) },
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = spacing.xl + bottomContentInset),
                    ) {
                        item {
                            ArtistHeader(
                                name = state.name.ifBlank { "Artist" },
                                artwork = state.artwork,
                                albumCount = state.albums.size,
                                trackCount = state.tracks.size,
                                onPlayAll = if (state.tracks.isNotEmpty()) {
                                    { onAction(ArtistAction.PlayAll) }
                                } else null,
                            )
                        }
                        if (state.albums.isNotEmpty()) {
                            item {
                                DesignSectionHeader(
                                    title = "Albums",
                                    metadata = "${state.albums.size} releases",
                                    titleWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    itemsIndexed(state.albums, key = { index, album -> album.lazyListKey(index) }) { _, album ->
                                        ArtistAlbumCard(album = album, onClick = { onAction(ArtistAction.NavigateToAlbum(album.id)) })
                                    }
                                }
                            }
                        }
                        if (state.tracks.isNotEmpty()) {
                            item {
                                DesignSectionHeader(
                                    title = "Top Tracks",
                                    metadata = "${state.tracks.size} songs",
                                    titleWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            itemsIndexed(state.tracks, key = { index, track -> track.lazyListKey(index) }) { _, track ->
                                ArtistTrackRow(
                                    track = track,
                                    onPlay = { onAction(ArtistAction.PlayTrack(track.id)) },
                                    onAlbumClick = track.albumId?.let { id -> { onAction(ArtistAction.NavigateToAlbum(id)) } },
                                    onDownload = { if (track.canDownload) onAction(ArtistAction.DownloadTrack(track)) },
                                )
                            }
                        }
                        if (state.albums.isEmpty() && state.tracks.isEmpty()) {
                            item {
                                DesignStatusCard(title = "No artist content", message = state.name.ifBlank { "Artist" }, modifier = Modifier.heightIn(min = 260.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    name: String,
    artwork: Artwork?,
    albumCount: Int,
    trackCount: Int,
    onPlayAll: (() -> Unit)?,
) {
    val shapes = DesignTokens.shapes

    DesignDetailHeaderSurface(accentColor = DesignPalette.Secondary, accentAlpha = 0.66f, borderAlpha = 0.18f) {
        Box(modifier = Modifier.size(220.dp).clip(RoundedCornerShape(shapes.full)).background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
            ArtworkImage(artwork = artwork, modifier = Modifier.fillMaxSize())
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = name,
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 620.dp),
            )
            Text(
                text = artistSummary(albumCount = albumCount, trackCount = trackCount),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        onPlayAll?.let {
            DesignTextButton(text = "Play All", variant = DesignTextButtonVariant.PrimaryFilled, size = DesignTextButtonSize.Medium, onClick = it)
        }
    }
}

@Composable
private fun ArtistAlbumCard(album: ArtistAlbumItem, onClick: () -> Unit) {
    val shapes = DesignTokens.shapes
    DesignCardSurface(modifier = Modifier.width(156.dp), contentPadding = PaddingValues(10.dp), fillMaxWidth = false, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(136.dp).clip(RoundedCornerShape(shapes.md)).background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))) {
                ArtworkImage(artwork = album.artwork, modifier = Modifier.fillMaxSize())
            }
            Text(text = album.name, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = album.year?.toString() ?: "Album", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1)
        }
    }
}

@Composable
private fun ArtistTrackRow(
    track: ArtistTrackItem,
    onPlay: () -> Unit,
    onAlbumClick: (() -> Unit)?,
    onDownload: () -> Unit,
) {
    val trackLabel = track.trackLabel()
    DesignCardSurface(modifier = Modifier.heightIn(min = 64.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), onClick = onPlay) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DesignTrackNumberBadge(label = trackLabel)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = track.title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                track.albumName?.let { album ->
                    Text(
                        text = album, style = MiuixTheme.textStyles.footnote1,
                        color = if (onAlbumClick != null) DesignPalette.Secondary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = if (onAlbumClick != null) Modifier.clickable(onClick = onAlbumClick) else Modifier,
                    )
                }
                track.durationMs?.let {
                    Text(text = durationLabel(it), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1)
                }
            }
            if (track.canDownload) {
                DesignTextButton(text = "DL", variant = DesignTextButtonVariant.Default, size = DesignTextButtonSize.Small, onClick = onDownload)
            }
        }
    }
}

internal fun ArtistAlbumItem.lazyListKey(index: Int): String = "artist-album-$index-$id"
internal fun ArtistTrackItem.lazyListKey(index: Int): String = "artist-track-$index-$id"

private fun ArtistTrackItem.trackLabel(): String = buildString {
    if (discNumber != null && discNumber > 1) append("$discNumber.")
    if (trackNumber != null) { if (isNotEmpty()) append("-"); append("$trackNumber") }
}

private fun artistSummary(albumCount: Int, trackCount: Int): String {
    val albumLabel = if (albumCount == 1) "1 album" else "$albumCount albums"
    val trackLabel = if (trackCount == 1) "1 song" else "$trackCount songs"
    return "$albumLabel, $trackLabel"
}

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
