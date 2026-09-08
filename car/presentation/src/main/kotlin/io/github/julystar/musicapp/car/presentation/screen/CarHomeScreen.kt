package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.component.CarAlbumCard
import io.github.julystar.musicapp.car.presentation.component.CarArtistCard
import io.github.julystar.musicapp.car.presentation.component.CarQuickActionCard
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.home.HomeStatistics
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.launch

@Composable
fun CarHomeScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    tracks: List<LibraryTrackItem>,
    albums: List<LibraryAlbumItem>,
    artists: List<LibraryArtistItem>,
    statistics: HomeStatistics,
    artworkRepository: ArtworkRepository,
    playbackController: PlaybackController,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (error != null) {
        CarPageState("音乐库载入失败：$error", modifier)
        return
    }
    if (loading) {
        CarPageState("正在载入音乐库…", modifier)
        return
    }
    if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
        CarPageState("音乐库为空，请先在设置中添加音乐来源", modifier)
        return
    }
    val scope = rememberCoroutineScope()
    fun playAt(index: Int) {
        if (index in tracks.indices) {
            val request = tracks.toCarPlaybackRequest(index)
            scope.launch { playbackController.play(request.items, request.startIndex) }
        }
    }
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
                    summary = tracks.firstOrNull()?.let { "${it.title} · ${it.artist.orEmpty()}" } ?: "暂无歌曲",
                    icon = CarIcon.Play,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    enabled = tracks.isNotEmpty(),
                    onClick = { playAt(0) },
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.content("Home"), left = CarFocusIds.Home)
                        .weight(1f),
                )
                CarQuickActionCard(
                    title = "每日推荐",
                    summary = tracks.getOrNull(1)?.title ?: "从音乐库生成",
                    icon = CarIcon.Albums,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    enabled = tracks.isNotEmpty(),
                    onClick = { playAt(if (tracks.size > 1) 1 else 0) },
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.item("home_action", "daily"), left = CarFocusIds.Home)
                        .weight(1f),
                )
                CarQuickActionCard(
                    title = "播放历史",
                    summary = "累计播放 ${statistics.totalTracksEverPlayed} 首",
                    icon = CarIcon.Songs,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    enabled = statistics.totalTracksEverPlayed > 0,
                    onClick = {},
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.item("home_action", "history"), left = CarFocusIds.Home)
                        .weight(1f),
                )
                CarQuickActionCard(
                    title = "搜索音乐",
                    summary = "歌曲、专辑和歌手",
                    icon = CarIcon.Search,
                    tileSize = metrics.headerHeight,
                    iconSize = metrics.iconSize,
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.item("home_action", "search"), left = CarFocusIds.Home)
                        .weight(0.45f),
                )
            }
        }
        if (albums.isNotEmpty()) {
            item(key = "album-heading") { CarSectionTitle("最近添加") }
            item(key = "album-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)) {
                    items(albums.take(8), key = { it.id }) { album ->
                        CarAlbumCard(
                            album = album,
                            artworkRepository = artworkRepository,
                            artworkSize = metrics.recommendationCardWidth - spacing.section,
                            onClick = onOpenAlbums,
                            modifier = Modifier
                                .carFocusTarget(CarFocusIds.item("home_album", album.id), left = CarFocusIds.Home)
                                .fillParentMaxWidth(0.18f)
                                .height(metrics.recommendationCardHeight),
                        )
                    }
                }
            }
        }
        if (artists.isNotEmpty()) {
            item(key = "artist-heading") { CarSectionTitle("推荐歌手") }
            item(key = "artist-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)) {
                    items(artists.take(8), key = { it.id }) { artist ->
                        CarArtistCard(
                            artist = artist,
                            artwork = albums.firstOrNull { it.artist == artist.name }?.let { Artwork.LibraryAlbum(it.id) },
                            artworkRepository = artworkRepository,
                            artworkSize = metrics.recommendationCardWidth * 0.72f,
                            onClick = onOpenArtists,
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

@Composable
internal fun CarSectionTitle(title: String) {
    BasicText(
        text = title,
        style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary),
        modifier = Modifier.padding(horizontal = LocalCarSpacing.current.pane),
    )
}
