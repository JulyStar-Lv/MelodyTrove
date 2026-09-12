package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.component.CarAlbumCard
import io.github.julystar.musicapp.car.presentation.component.CarArtistCard
import io.github.julystar.musicapp.car.presentation.component.CarQuickActionCard
import io.github.julystar.musicapp.car.presentation.component.CarArtwork
import io.github.julystar.musicapp.car.presentation.component.carInteractiveSurface
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as CarIconView
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
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
    artworkRepository: ArtworkRepository,
    playbackController: PlaybackController,
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
                HomeHeroAction(
                    "猜你喜欢", homeState.favorites.summary("从收藏中挑选"), CarIcon.Songs,
                    albums.firstOrNull()?.id?.let(Artwork::LibraryAlbum), artworkRepository,
                    listOf(Color(0xFF7456D8), Color(0xFF4930A8)),
                    onClick = { play(homeState.favorites.dataOrEmpty(), 0) },
                    modifier = Modifier.width(metrics.homeQuickCardWidth),
                )
                HomeHeroAction(
                    "每日推荐", tracks.firstOrNull()?.title ?: "本地精选", CarIcon.Albums,
                    tracks.firstOrNull()?.id?.let { Artwork.LibraryCover(it) }, artworkRepository,
                    listOf(Color(0xFF168E78), Color(0xFF075C54)),
                    onClick = { play(tracks, 0) },
                    modifier = Modifier.width(metrics.homeQuickCardWidth),
                )
                HomeHeroAction(
                    "播放历史", homeState.recentlyPlayed.summary("暂无播放历史"), CarIcon.Songs,
                    recentPlayed.firstOrNull()?.id?.let { Artwork.LibraryCover(it) }, artworkRepository,
                    listOf(Color(0xFFE85D73), Color(0xFFB73D54)),
                    onClick = { play(recentPlayed, 0) },
                    modifier = Modifier.width(metrics.homeQuickCardWidth),
                )
                HomeSearchAction(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.content("Home"), left = CarFocusIds.Home)
                        .width(metrics.homeSearchCardWidth),
                )
            }
        }
        item(key = "recent-heading") { CarSectionTitle("最近添加", CarIcon.Songs) }
        item(key = "recent-grid") {
            val recent = homeState.recentlyAdded.dataOrEmpty()
            if (recent.isEmpty()) CarSectionMessage("暂无最近添加歌曲")
            else HomeRecentAdded(recent, metrics, artworkRepository, ::play)
        }
        if (albums.isNotEmpty()) {
            item(key = "album-heading") { CarSectionTitle("推荐专辑", CarIcon.Albums) }
            item(key = "album-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.homeRecommendationGap)) {
                    items(albums.take(HOME_MEDIA_LIMIT), key = { it.id }) { album ->
                        CarAlbumCard(
                            album = album,
                            artworkRepository = artworkRepository,
                            artworkSize = metrics.homeRecommendationArtworkSize,
                            onClick = { onAlbumClick(album.id) },
                            modifier = Modifier
                                .carFocusTarget(CarFocusIds.item("home_album", album.id), left = CarFocusIds.Home)
                                .width(metrics.homeRecommendationCardWidth)
                                .height(metrics.recommendationCardHeight),
                        )
                    }
                }
            }
        }
        if (playlists.isNotEmpty()) {
            item(key = "playlist-heading") { CarSectionTitle("推荐歌单", CarIcon.Playlists) }
            item(key = "playlist-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.homeRecommendationGap)) {
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
            item(key = "artist-heading") { CarSectionTitle("推荐歌手", CarIcon.Artists) }
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
                                .width(metrics.homeRecommendationCardWidth)
                                .height(metrics.artistCardHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeroAction(
    title: String,
    summary: String,
    icon: CarIcon,
    artwork: Artwork?,
    artworkRepository: ArtworkRepository,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(LocalCarShapes.current.card)
            .background(Brush.horizontalGradient(gradient))
            .carInteractiveSurface(LocalCarShapes.current.card, defaultColor = Color.Transparent, onClick = onClick)
            .padding(LocalCarSpacing.current.content),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp).clip(LocalCarShapes.current.control)
                .background(Color.White.copy(alpha = 0.16f)),
        ) {
            CarIconView(icon, null, Color.White, Modifier.size(40.dp))
        }
        Spacer(Modifier.width(LocalCarSpacing.current.small))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = LocalCarTypography.current.title.copy(color = Color.White), maxLines = 1)
            BasicText(summary, style = LocalCarTypography.current.supporting.copy(color = Color.White.copy(alpha = 0.82f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CarArtwork(artwork, artworkRepository, 112.dp, LocalCarShapes.current.artwork)
    }
}

@Composable
private fun HomeSearchAction(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(LocalCarShapes.current.card)
            .carInteractiveSurface(
                LocalCarShapes.current.card,
                defaultColor = LocalCarColors.current.panel,
                onClick = onClick,
            ),
    ) {
        CarIconView(CarIcon.Search, null, LocalCarColors.current.accentPrimary, Modifier.size(56.dp))
        Spacer(Modifier.height(LocalCarSpacing.current.small))
        BasicText(
            "搜索音乐",
            style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textPrimary),
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeRecentAdded(
    tracks: List<LibraryTrackItem>,
    metrics: CarLayoutMetrics,
    artworkRepository: ArtworkRepository,
    onPlay: (List<LibraryTrackItem>, Int) -> Unit,
) {
    val first = tracks.first()
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
        modifier = Modifier.fillMaxWidth().height(248.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(metrics.homeRecentFeatureWidth)
                .fillMaxHeight()
                .carFocusTarget(CarFocusIds.item("home_recent", first.id), left = CarFocusIds.Home)
                .carInteractiveSurface(LocalCarShapes.current.card, defaultColor = LocalCarColors.current.panel) {
                    onPlay(tracks, 0)
                }
                .padding(LocalCarSpacing.current.compact),
        ) {
            CarArtwork(
                first.albumId?.let(Artwork::LibraryAlbum) ?: Artwork.LibraryCover(first.id),
                artworkRepository,
                200.dp,
                LocalCarShapes.current.artwork,
            )
            Spacer(Modifier.width(LocalCarSpacing.current.content))
            Column(Modifier.weight(1f)) {
                BasicText(first.title, style = LocalCarTypography.current.title.copy(color = LocalCarColors.current.textPrimary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                BasicText(first.artist.orEmpty(), style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textSecondary), maxLines = 1)
                Spacer(Modifier.height(LocalCarSpacing.current.small))
                BasicText(first.albumName.orEmpty(), style = LocalCarTypography.current.supporting.copy(color = LocalCarColors.current.textSummary), maxLines = 1)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).background(LocalCarColors.current.accentPrimary, LocalCarShapes.current.control)) {
                CarIconView(CarIcon.Play, null, Color.White, Modifier.size(28.dp))
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(metrics.cardGap),
            modifier = Modifier.width(
                metrics.homeRecentCompactWidth * metrics.homeRecentColumns +
                    metrics.cardGap * (metrics.homeRecentColumns - 1),
            ),
        ) {
            tracks.drop(1).take(metrics.homeRecentColumns * 2).chunked(metrics.homeRecentColumns)
                .forEachIndexed { rowIndex, rowTracks ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
                    modifier = Modifier.fillMaxWidth().height((248.dp - metrics.cardGap) / 2f),
                ) {
                    rowTracks.forEachIndexed { columnIndex, track ->
                        HomeCompactTrack(
                            track, artworkRepository,
                            onClick = { onPlay(tracks, rowIndex * metrics.homeRecentColumns + columnIndex + 1) },
                            modifier = Modifier.width(metrics.homeRecentCompactWidth),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCompactTrack(
    track: LibraryTrackItem,
    artworkRepository: ArtworkRepository,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxHeight()
            .carInteractiveSurface(
                LocalCarShapes.current.card,
                defaultColor = LocalCarColors.current.panel,
                onClick = onClick,
            )
            .padding(LocalCarSpacing.current.small),
    ) {
        CarArtwork(track.albumId?.let(Artwork::LibraryAlbum) ?: Artwork.LibraryCover(track.id), artworkRepository, 56.dp, LocalCarShapes.current.artwork)
        Spacer(Modifier.width(LocalCarSpacing.current.compact))
        Column(Modifier.weight(1f)) {
            BasicText(track.title, style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(track.artist.orEmpty(), style = LocalCarTypography.current.supporting.copy(color = LocalCarColors.current.textSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
internal fun CarSectionTitle(title: String, icon: CarIcon? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.small),
        modifier = Modifier.padding(horizontal = LocalCarSpacing.current.content),
    ) {
        if (icon != null) {
            CarIconView(icon, null, LocalCarColors.current.accentPrimary, Modifier.size(40.dp))
        }
        BasicText(
            text = title,
            style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary),
        )
    }
}

private const val HOME_MEDIA_LIMIT = 8
