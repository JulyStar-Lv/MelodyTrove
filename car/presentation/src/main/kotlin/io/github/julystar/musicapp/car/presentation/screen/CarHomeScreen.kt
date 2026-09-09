package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.car.presentation.component.CarAlbumCard
import io.github.julystar.musicapp.car.presentation.component.CarArtistCard
import io.github.julystar.musicapp.car.presentation.component.CarQuickActionCard
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.home.HomeStatistics
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CarHomeScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    tracks: List<LibraryTrackItem>,
    albums: List<LibraryAlbumItem>,
    artists: List<LibraryArtistItem>,
    playlists: List<PlaylistSummary>,
    statistics: HomeStatistics,
    artworkRepository: ArtworkRepository,
    playbackController: PlaybackController,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarHomeViewModel = koinViewModel(),
) {
    val homeState by viewModel.state.collectAsState()
    LaunchedEffect(loading, tracks.size) {
        if (!loading) viewModel.refresh()
    }
    if (error != null) {
        CarPageState("音乐库载入失败：$error", modifier)
        return
    }
    if (loading) {
        CarPageState("正在载入音乐库…", modifier)
        return
    }
    if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()) {
        CarPageState("音乐库为空，请先在设置中添加音乐来源", modifier)
        return
    }
    val scope = rememberCoroutineScope()
    fun play(items: List<LibraryTrackItem>, index: Int) {
        if (index in items.indices) {
            val request = items.toCarPlaybackRequest(index)
            scope.launch { playbackController.play(request.items, request.startIndex) }
        }
    }
    val recentPlayed = homeState.recentlyPlayed.dataOrEmpty()
    val spacing = LocalCarSpacing.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing.pane),
        contentPadding = PaddingValues(
            start = metrics.contentHorizontalPadding,
            top = metrics.contentTop,
            end = metrics.contentHorizontalPadding,
            bottom = spacing.wide,
        ),
        modifier = modifier,
    ) {
        item(key = "quick-actions") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
                modifier = Modifier.fillMaxWidth().height(metrics.quickActionHeight),
            ) {
                CarQuickActionCard(
                    title = "猜你喜欢",
                    summary = "暂不支持智能推荐",
                    icon = CarIcon.Play,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                CarQuickActionCard(
                    title = "每日推荐",
                    summary = "暂不支持每日推荐",
                    icon = CarIcon.Albums,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                CarQuickActionCard(
                    title = "播放历史",
                    summary = homeState.recentlyPlayed.summary("暂无播放历史"),
                    icon = CarIcon.Songs,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    enabled = recentPlayed.isNotEmpty(),
                    onClick = { play(recentPlayed, 0) },
                    modifier = Modifier.weight(1f),
                )
                CarQuickActionCard(
                    title = "搜索音乐",
                    summary = "歌曲、专辑和歌手",
                    icon = CarIcon.Search,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.content("Home"), left = CarFocusIds.Home)
                        .weight(0.45f),
                )
            }
        }
        item(key = "library-shortcuts-heading") { CarSectionTitle("音乐库") }
        item(key = "library-shortcuts") {
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap), modifier = Modifier.fillMaxWidth()) {
                CarQuickActionCard(
                    "专辑", "${albums.size} 张", CarIcon.Albums, metrics.headerHeight, metrics.iconSize,
                    onClick = onOpenAlbums,
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.item("home_shortcut", "albums"), left = CarFocusIds.Home)
                        .weight(1f)
                        .height(metrics.compactCardHeight),
                )
                CarQuickActionCard(
                    "歌单", "${playlists.size} 个", CarIcon.Playlists, metrics.headerHeight, metrics.iconSize,
                    onClick = onOpenPlaylists,
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.item("home_shortcut", "playlists"), left = CarFocusIds.Home)
                        .weight(1f)
                        .height(metrics.compactCardHeight),
                )
                CarQuickActionCard(
                    "歌手", "${artists.size} 位", CarIcon.Artists, metrics.headerHeight, metrics.iconSize,
                    onClick = onOpenArtists,
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.item("home_shortcut", "artists"), left = CarFocusIds.Home)
                        .weight(1f)
                        .height(metrics.compactCardHeight),
                )
            }
        }
        homeTrackSection(
            key = "recently-added",
            title = "最近添加",
            state = homeState.recentlyAdded,
            metrics = metrics,
            emptyMessage = "暂无最近添加歌曲",
            onPlay = ::play,
        )
        homeTrackSection(
            key = "recently-played",
            title = "最近播放 · 累计 ${statistics.totalTracksEverPlayed} 首",
            state = homeState.recentlyPlayed,
            metrics = metrics,
            emptyMessage = "暂无播放历史",
            onPlay = ::play,
        )
        homeTrackSection(
            key = "favorites",
            title = "收藏歌曲",
            state = homeState.favorites,
            metrics = metrics,
            emptyMessage = "暂无收藏歌曲",
            onPlay = ::play,
        )
        if (albums.isNotEmpty()) {
            item(key = "album-heading") { CarSectionTitle("专辑") }
            item(key = "album-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)) {
                    items(albums.take(HOME_MEDIA_LIMIT), key = { it.id }) { album ->
                        CarAlbumCard(
                            album = album,
                            artworkRepository = artworkRepository,
                            artworkSize = metrics.recommendationCardWidth - spacing.section,
                            onClick = { onAlbumClick(album.id) },
                            modifier = Modifier
                                .carFocusTarget(CarFocusIds.item("home_album", album.id), left = CarFocusIds.Home)
                                .fillParentMaxWidth(0.18f)
                                .height(metrics.recommendationCardHeight),
                        )
                    }
                }
            }
        }
        if (playlists.isNotEmpty()) {
            item(key = "playlist-heading") { CarSectionTitle("歌单") }
            item(key = "playlist-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)) {
                    items(playlists.take(HOME_MEDIA_LIMIT), key = { it.id }) { playlist ->
                        CarQuickActionCard(
                            playlist.title,
                            "${playlist.musicCount} 首歌曲",
                            CarIcon.Playlists,
                            metrics.headerHeight,
                            metrics.iconSize,
                            onClick = { onPlaylistClick(playlist) },
                            modifier = Modifier
                                .carFocusTarget(CarFocusIds.item("home_playlist", playlist.id), left = CarFocusIds.Home)
                                .fillParentMaxWidth(0.3f)
                                .height(metrics.compactCardHeight),
                        )
                    }
                }
            }
        }
        if (artists.isNotEmpty()) {
            item(key = "artist-heading") { CarSectionTitle("歌手") }
            item(key = "artist-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)) {
                    items(artists.take(HOME_MEDIA_LIMIT), key = { it.id }) { artist ->
                        CarArtistCard(
                            artist = artist,
                            artwork = albums.firstOrNull { it.artist == artist.name }?.let { Artwork.LibraryAlbum(it.id) },
                            artworkRepository = artworkRepository,
                            artworkSize = metrics.recommendationCardWidth * 0.72f,
                            onClick = { onArtistClick(artist.id) },
                            modifier = Modifier
                                .carFocusTarget(CarFocusIds.item("home_artist", artist.id), left = CarFocusIds.Home)
                                .fillParentMaxWidth(0.18f)
                                .height(metrics.artistCardHeight),
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeTrackSection(
    key: String,
    title: String,
    state: RepositoryState<List<LibraryTrackItem>>,
    metrics: CarLayoutMetrics,
    emptyMessage: String,
    onPlay: (List<LibraryTrackItem>, Int) -> Unit,
) {
    item(key = "$key-heading") { CarSectionTitle(title) }
    when (state) {
        RepositoryState.Loading -> item(key = "$key-loading") { CarSectionMessage("正在载入…") }
        is RepositoryState.Empty -> item(key = "$key-empty") { CarSectionMessage(state.message ?: emptyMessage) }
        is RepositoryState.Error -> item(key = "$key-error") { CarSectionMessage(state.message ?: "载入失败") }
        is RepositoryState.Loaded -> item(key = "$key-row") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)) {
                itemsIndexed(state.data, key = { _, track -> track.id }) { index, track ->
                    CarQuickActionCard(
                        title = track.title,
                        summary = track.artist.orEmpty(),
                        icon = CarIcon.Songs,
                        tileSize = metrics.headerHeight,
                        iconSize = metrics.iconSize,
                        onClick = { onPlay(state.data, index) },
                        modifier = Modifier
                            .carFocusTarget(CarFocusIds.item("home_$key", track.id), left = CarFocusIds.Home)
                            .fillParentMaxWidth(0.3f)
                            .height(metrics.compactCardHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun CarSectionMessage(message: String) {
    BasicText(
        text = message,
        style = LocalCarTypography.current.bodyLarge.copy(color = LocalCarColors.current.textSecondary),
        modifier = Modifier.padding(horizontal = LocalCarSpacing.current.pane),
    )
}

private fun RepositoryState<List<LibraryTrackItem>>.dataOrEmpty(): List<LibraryTrackItem> =
    (this as? RepositoryState.Loaded)?.data.orEmpty()

private fun RepositoryState<List<LibraryTrackItem>>.summary(emptyMessage: String): String = when (this) {
    RepositoryState.Loading -> "正在载入…"
    is RepositoryState.Empty -> message ?: emptyMessage
    is RepositoryState.Error -> message ?: "载入失败"
    is RepositoryState.Loaded -> data.firstOrNull()?.let { "${it.title} · ${it.artist.orEmpty()}" } ?: emptyMessage
}

@Composable
internal fun CarSectionTitle(title: String) {
    BasicText(
        text = title,
        style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary),
        modifier = Modifier.padding(horizontal = LocalCarSpacing.current.pane),
    )
}

private const val HOME_MEDIA_LIMIT = 8
