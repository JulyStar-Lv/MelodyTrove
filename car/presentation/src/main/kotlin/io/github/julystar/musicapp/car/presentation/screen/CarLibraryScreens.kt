package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.car.presentation.component.CarAlbumCard
import io.github.julystar.musicapp.car.presentation.component.CarArtistCard
import io.github.julystar.musicapp.car.presentation.component.CarQuickActionCard
import io.github.julystar.musicapp.car.presentation.component.CarSongRow
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.launch

@Composable
fun CarSongsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    tracks: List<LibraryTrackItem>,
    currentTrackId: Long?,
    playbackController: PlaybackController,
    modifier: Modifier = Modifier,
) {
    if (error != null) return CarPageState("音乐库载入失败：$error", modifier)
    if (loading) return CarPageState("正在载入歌曲…", modifier)
    if (tracks.isEmpty()) return CarPageState("音乐库中没有歌曲", modifier)
    val scope = rememberCoroutineScope()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact),
        contentPadding = pagePadding(metrics),
        modifier = modifier.fillMaxSize(),
    ) {
        item { CarSectionTitle("歌曲 · ${tracks.size}") }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            CarSongRow(
                track = track,
                index = index,
                playing = currentTrackId == track.id,
                height = metrics.compactCardHeight,
                onClick = {
                    val request = tracks.toCarPlaybackRequest(index)
                    scope.launch { playbackController.play(request.items, request.startIndex) }
                },
                modifier = Modifier
                    .carFocusTarget(
                        id = if (index == 0) CarFocusIds.content("Songs") else CarFocusIds.item("song", track.id),
                        left = CarFocusIds.Songs,
                    )
                    .height(metrics.compactCardHeight),
            )
        }
    }
}

@Composable
fun CarAlbumsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    albums: List<LibraryAlbumItem>,
    artworkRepository: ArtworkRepository,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (error != null) return CarPageState("音乐库载入失败：$error", modifier)
    if (loading) return CarPageState("正在载入专辑…", modifier)
    if (albums.isEmpty()) return CarPageState("音乐库中没有专辑", modifier)
    CarMediaGrid(
        metrics = metrics,
        title = "专辑 · ${albums.size}",
        content = {
            itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                CarAlbumCard(
                    album,
                    artworkRepository,
                    metrics.recommendationCardWidth - LocalCarSpacing.current.section,
                    onClick = { onAlbumClick(album.id) },
                    modifier = Modifier
                        .carFocusTarget(
                            id = if (index == 0) CarFocusIds.content("Albums") else CarFocusIds.item("album", album.id),
                            left = CarFocusIds.Albums,
                        )
                        .fillParentMaxWidth(0.2f)
                        .height(metrics.recommendationCardHeight),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CarArtistsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    artists: List<LibraryArtistItem>,
    albums: List<LibraryAlbumItem>,
    artworkRepository: ArtworkRepository,
    onArtistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (error != null) return CarPageState("音乐库载入失败：$error", modifier)
    if (loading) return CarPageState("正在载入歌手…", modifier)
    if (artists.isEmpty()) return CarPageState("音乐库中没有歌手", modifier)
    CarMediaGrid(
        metrics = metrics,
        title = "歌手 · ${artists.size}",
        content = {
            itemsIndexed(artists, key = { _, artist -> artist.id }) { index, artist ->
                CarArtistCard(
                    artist,
                    albums.firstOrNull { it.artist == artist.name }?.let {
                        io.github.julystar.musicapp.core.domain.model.Artwork.LibraryAlbum(it.id)
                    },
                    artworkRepository,
                    metrics.recommendationCardWidth * 0.72f,
                    onClick = { onArtistClick(artist.id) },
                    modifier = Modifier
                        .carFocusTarget(
                            id = if (index == 0) CarFocusIds.content("Artists") else CarFocusIds.item("artist", artist.id),
                            left = CarFocusIds.Artists,
                        )
                        .fillParentMaxWidth(0.2f)
                        .height(metrics.artistCardHeight),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CarPlaylistsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    playlists: List<PlaylistSummary>,
    onPlaylistClick: (PlaylistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (loading) return CarPageState("正在载入歌单…", modifier)
    if (playlists.isEmpty()) return CarPageState("还没有歌单", modifier)
    LazyColumn(contentPadding = pagePadding(metrics), verticalArrangement = Arrangement.spacedBy(metrics.cardGap), modifier = modifier) {
        item { CarSectionTitle("歌单 · ${playlists.size}") }
        itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { index, playlist ->
            CarQuickActionCard(
                title = playlist.title,
                summary = "${playlist.musicCount} 首歌曲",
                icon = CarIcon.Playlists,
                tileSize = metrics.headerHeight,
                iconSize = metrics.iconSize,
                onClick = { onPlaylistClick(playlist) },
                modifier = Modifier
                    .carFocusTarget(
                        id = if (index == 0) CarFocusIds.content("Playlists") else CarFocusIds.item("playlist", playlist.id),
                        left = CarFocusIds.Playlists,
                    )
                    .height(metrics.compactCardHeight),
            )
        }
    }
}

@Composable
private fun CarMediaGrid(
    metrics: CarLayoutMetrics,
    title: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
    modifier: Modifier,
) {
    LazyColumn(contentPadding = pagePadding(metrics), modifier = modifier) {
        item { CarSectionTitle(title) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap), content = content)
        }
    }
}

@Composable
private fun pagePadding(metrics: CarLayoutMetrics) = PaddingValues(
    start = metrics.contentHorizontalPadding,
    top = metrics.contentTop,
    end = metrics.contentHorizontalPadding,
    bottom = LocalCarSpacing.current.wide,
)
