package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.component.CarAlbumCard
import io.github.julystar.musicapp.car.presentation.component.CarArtwork
import io.github.julystar.musicapp.car.presentation.component.CarQuickActionCard
import io.github.julystar.musicapp.car.presentation.component.CarSongRow
import io.github.julystar.musicapp.car.presentation.component.carInteractiveSurface
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as CarIconView
import io.github.julystar.musicapp.car.presentation.focus.CarFocusId
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfile
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.DomainAlbumDetail
import io.github.julystar.musicapp.core.domain.model.DomainArtistAlbum
import io.github.julystar.musicapp.core.domain.model.DomainArtistDetail
import io.github.julystar.musicapp.core.domain.model.DomainPlaylistTrack
import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.repository.AlbumDetailRepository
import io.github.julystar.musicapp.core.domain.repository.ArtistDetailRepository
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.PlaylistRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import org.koin.compose.koinInject

@Composable
fun CarAlbumDetailScreen(
    albumId: Long,
    metrics: CarLayoutMetrics,
    navigationFocusId: CarFocusId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = koinInject<AlbumDetailRepository>()
    var retry by remember { mutableIntStateOf(0) }
    var result by remember(albumId, retry) { mutableStateOf<DetailResult<DomainAlbumDetail>>(DetailResult.Loading) }
    LaunchedEffect(albumId, retry) {
        result = try {
            DetailResult.Content(repository.loadAlbumDetail(albumId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DetailResult.Error(error.message ?: "专辑载入失败")
        }
    }
    when (val value = result) {
        DetailResult.Loading -> CarPageState("正在载入专辑…", modifier)
        is DetailResult.Error -> ErrorDetail(value.message, metrics, navigationFocusId, { retry++ }, onBack, modifier)
        is DetailResult.Content -> DetailLayout(
            title = value.value.albumTitle,
            subtitle = listOfNotNull(value.value.albumArtist, value.value.year?.toString(), value.value.genre).joinToString(" · "),
            artwork = Artwork.LibraryAlbum(albumId),
            tracks = value.value.tracks.map(DomainTrackBrowserItem::toLibraryTrackItem),
            playlistId = null,
            metrics = metrics,
            navigationFocusId = navigationFocusId,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
fun CarArtistDetailScreen(
    artistId: Long,
    metrics: CarLayoutMetrics,
    navigationFocusId: CarFocusId,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = koinInject<ArtistDetailRepository>()
    var retry by remember { mutableIntStateOf(0) }
    var result by remember(artistId, retry) { mutableStateOf<DetailResult<DomainArtistDetail>>(DetailResult.Loading) }
    LaunchedEffect(artistId, retry) {
        result = try {
            DetailResult.Content(repository.loadArtistDetail(artistId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DetailResult.Error(error.message ?: "歌手载入失败")
        }
    }
    when (val value = result) {
        DetailResult.Loading -> CarPageState("正在载入歌手…", modifier)
        is DetailResult.Error -> ErrorDetail(value.message, metrics, navigationFocusId, { retry++ }, onBack, modifier)
        is DetailResult.Content -> ArtistDetailLayout(
            detail = value.value,
            metrics = metrics,
            navigationFocusId = navigationFocusId,
            onBack = onBack,
            onAlbumClick = onAlbumClick,
            modifier = modifier,
        )
    }
}

@Composable
fun CarPlaylistDetailScreen(
    playlistId: Long,
    title: String,
    metrics: CarLayoutMetrics,
    navigationFocusId: CarFocusId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = koinInject<PlaylistRepository>()
    var retry by remember { mutableIntStateOf(0) }
    val result by produceState<DetailResult<List<DomainPlaylistTrack>>>(
        initialValue = DetailResult.Loading,
        key1 = playlistId,
        key2 = retry,
    ) {
        value = DetailResult.Loading
        repository.observePlaylistTracks(playlistId)
            .catch { error -> value = DetailResult.Error(error.message ?: "歌单载入失败") }
            .collect { tracks ->
                value = DetailResult.Content(tracks)
            }
    }
    when (val value = result) {
        DetailResult.Loading -> CarPageState("正在载入歌单…", modifier)
        is DetailResult.Error -> ErrorDetail(value.message, metrics, navigationFocusId, { retry++ }, onBack, modifier)
        is DetailResult.Content -> DetailLayout(
            title = title,
            subtitle = "${value.value.size} 首歌曲",
            artwork = value.value.firstOrNull()?.trackId?.let { Artwork.LibraryCover(it) },
            tracks = value.value.map(DomainPlaylistTrack::toLibraryTrackItem),
            playlistId = playlistId,
            metrics = metrics,
            navigationFocusId = navigationFocusId,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun DetailLayout(
    title: String,
    subtitle: String,
    artwork: Artwork?,
    tracks: List<LibraryTrackItem>,
    playlistId: Long?,
    metrics: CarLayoutMetrics,
    navigationFocusId: CarFocusId,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val artworkRepository = koinInject<ArtworkRepository>()
    val playbackController = koinInject<PlaybackController>()
    val favoritesRepository = koinInject<FavoritesRepository>()
    val playerState by playbackController.state.collectAsState()
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    val scope = rememberCoroutineScope()
    val queue = remember(tracks, playlistId) {
        if (tracks.isEmpty()) emptyList() else tracks.toCarPlaybackRequest(0, playlistId).items
    }
    DetailScaffold(
        metrics = metrics,
        title = title,
        subtitle = subtitle,
        secondary = "${tracks.size} 首歌曲",
        artwork = artwork,
        artworkRepository = artworkRepository,
        navigationFocusId = navigationFocusId,
        onBack = onBack,
        onPlayAll = { if (queue.isNotEmpty()) scope.launch { playbackController.play(queue, 0) } },
        onShuffle = {
            if (queue.isNotEmpty()) scope.launch { playbackController.play(queue.shuffled(), 0) }
        },
        playEnabled = queue.isNotEmpty(),
        modifier = modifier,
    ) {
        DetailTrackList(
            tracks = tracks,
            currentTrackId = playerState.currentItem?.libraryTrackId,
            metrics = metrics,
            artworkRepository = artworkRepository,
            favoriteTrackIds = favoriteTrackIds,
            onToggleFavorite = { trackId -> scope.launch { favoritesRepository.toggleFavorite(trackId) } },
            onPlay = { index -> scope.launch { playbackController.play(queue, index) } },
            layout = if (playlistId == null) DetailTrackLayout.Album else DetailTrackLayout.Playlist,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ArtistDetailLayout(
    detail: DomainArtistDetail,
    metrics: CarLayoutMetrics,
    navigationFocusId: CarFocusId,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier,
) {
    val artworkRepository = koinInject<ArtworkRepository>()
    val playbackController = koinInject<PlaybackController>()
    val favoritesRepository = koinInject<FavoritesRepository>()
    val playerState by playbackController.state.collectAsState()
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    val scope = rememberCoroutineScope()
    val tracks = remember(detail.tracks) { detail.tracks.map(DomainTrackBrowserItem::toLibraryTrackItem) }
    val queue = remember(tracks) {
        if (tracks.isEmpty()) emptyList() else tracks.toCarPlaybackRequest(0).items
    }
    DetailScaffold(
        metrics = metrics,
        title = detail.name ?: "未知歌手",
        subtitle = "${tracks.size} 首歌曲 · ${detail.albums.size} 张专辑",
        secondary = "本地乐库",
        artwork = detail.albums.firstOrNull()?.id?.let(Artwork::LibraryAlbum),
        artworkRepository = artworkRepository,
        navigationFocusId = navigationFocusId,
        onBack = onBack,
        onPlayAll = { if (queue.isNotEmpty()) scope.launch { playbackController.play(queue, 0) } },
        onShuffle = {
            if (queue.isNotEmpty()) scope.launch { playbackController.play(queue.shuffled(), 0) }
        },
        playEnabled = queue.isNotEmpty(),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize()) {
            BasicText("歌曲  ›", style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary))
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            DetailTrackList(
                tracks = tracks.take(metrics.artistVisibleTrackCount),
                currentTrackId = playerState.currentItem?.libraryTrackId,
                metrics = metrics,
                artworkRepository = artworkRepository,
                favoriteTrackIds = favoriteTrackIds,
                onToggleFavorite = { trackId -> scope.launch { favoritesRepository.toggleFavorite(trackId) } },
                onPlay = { index -> scope.launch { playbackController.play(queue, index) } },
                layout = DetailTrackLayout.Artist,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            BasicText("专辑  ›", style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary))
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            if (detail.albums.isEmpty()) {
                BasicText("暂无专辑", style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textSecondary))
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
                    modifier = Modifier.height(metrics.recommendationCardHeight * 0.72f),
                ) {
                    itemsIndexed(detail.albums, key = { _, album -> album.id }) { _, album ->
                        CarAlbumCard(
                            album = album.toLibraryAlbumItem(detail.name),
                            artworkRepository = artworkRepository,
                            artworkSize = metrics.artistAlbumArtworkSize,
                            onClick = { onAlbumClick(album.id) },
                            modifier = Modifier.width(metrics.artistAlbumCardWidth).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScaffold(
    metrics: CarLayoutMetrics,
    title: String,
    subtitle: String,
    secondary: String,
    artwork: Artwork?,
    artworkRepository: ArtworkRepository,
    navigationFocusId: CarFocusId,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    playEnabled: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalCarColors.current
    val heroTint = colors.backgroundSubtle
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.detailPaneGap),
        modifier = modifier.fillMaxSize().padding(
            start = metrics.detailContentMargin,
            top = metrics.contentTop,
            end = metrics.detailContentEndMargin,
            bottom = metrics.navigationRailBottom,
        ),
    ) {
        Box(
            modifier = Modifier.width(metrics.detailHeroWidth).fillMaxHeight()
                .clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle),
        ) {
            CarArtwork(
                artwork = artwork,
                repository = artworkRepository,
                size = metrics.detailHeroWidth,
                shape = RectangleShape,
                fillBounds = true,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = 1.08f
                    scaleY = 1.08f
                }.blur(36.dp),
            )
            CarArtwork(
                artwork = artwork,
                repository = artworkRepository,
                size = metrics.detailHeroWidth,
                shape = RectangleShape,
                modifier = Modifier.align(Alignment.TopCenter)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                0.68f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
            Box(
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.42f to Color.Transparent,
                            0.58f to heroTint.copy(alpha = 0.08f),
                            0.72f to heroTint.copy(alpha = 0.58f),
                            1f to heroTint.copy(alpha = 0.9f),
                        ),
                    ),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(LocalCarSpacing.current.content).size(metrics.headerHeight)
                    .carFocusTarget(CarFocusIds.DetailBack, left = navigationFocusId, down = CarFocusIds.DetailPlayAll)
                    .carInteractiveSurface(LocalCarShapes.current.control, defaultColor = Color.Black.copy(alpha = 0.5f), onClick = onBack),
            ) {
                CarIconView(CarIcon.Back, "返回", Color.White, Modifier.size(metrics.iconSize * 0.7f))
            }
            Column(
                modifier = Modifier.align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(
                        start = if (metrics.profile == CarLayoutProfile.Expanded) 40.dp else 24.dp,
                        top = 600.dp,
                        end = if (metrics.profile == CarLayoutProfile.Expanded) 40.dp else 24.dp,
                    )
                    .height(280.dp),
            ) {
                BasicText(title, style = LocalCarTypography.current.pageTitle.copy(color = colors.textPrimary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(LocalCarSpacing.current.section))
                BasicText(subtitle, style = LocalCarTypography.current.body.copy(color = colors.textPrimary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(LocalCarSpacing.current.small))
                BasicText(secondary, style = LocalCarTypography.current.body.copy(color = colors.textSecondary))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.small)) {
                    DetailAction("播放全部", CarIcon.Play, playEnabled, onPlayAll, Modifier.width(176.dp))
                    DetailAction("随机", CarIcon.Shuffle, playEnabled, onShuffle, Modifier.width(metrics.headerHeight))
                    DetailAction("收藏", CarIcon.Heart, false, {}, Modifier.width(metrics.headerHeight))
                    DetailAction("更多", CarIcon.More, false, {}, Modifier.width(metrics.headerHeight))
                }
            }
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle).padding(
                    horizontal = metrics.detailContentPadding,
                    vertical = LocalCarSpacing.current.section,
                ),
        ) { content() }
    }
}

@Composable
private fun DetailAction(
    title: String,
    icon: CarIcon,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalCarColors.current
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(72.dp).carInteractiveSurface(
            LocalCarShapes.current.control, enabled = enabled, defaultColor = colors.surfaceContainerHigh, onClick = onClick,
        ),
    ) {
        CarIconView(icon, title, if (enabled) colors.accentPrimary else colors.textDisabled, Modifier.size(28.dp))
        if (title == "播放全部") {
            Spacer(Modifier.width(LocalCarSpacing.current.small))
            BasicText(
                title,
                style = LocalCarTypography.current.body.copy(
                    color = if (enabled) colors.accentPrimary else colors.textDisabled,
                ),
            )
        }
    }
}

@Composable
private fun DetailTrackList(
    tracks: List<LibraryTrackItem>,
    currentTrackId: Long?,
    metrics: CarLayoutMetrics,
    artworkRepository: ArtworkRepository,
    favoriteTrackIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onPlay: (Int) -> Unit,
    layout: DetailTrackLayout,
    modifier: Modifier,
) {
    if (tracks.isEmpty()) return CarPageState("这里还没有歌曲", modifier)
    Column(modifier) {
        if (layout != DetailTrackLayout.Playlist) {
            DetailTrackHeader(layout, metrics)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.weight(1f)) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                CarSongRow(
                    track = track,
                    index = index,
                    playing = currentTrackId == track.id,
                    height = metrics.detailRowHeight,
                    artworkRepository = artworkRepository,
                    showArtwork = layout != DetailTrackLayout.Album && track.albumId != null,
                    showAlbum = layout != DetailTrackLayout.Album && track.albumName != null,
                    showQuality = layout == DetailTrackLayout.Album,
                    showDuration = metrics.profile == CarLayoutProfile.Expanded,
                    showActions = metrics.profile == CarLayoutProfile.Expanded,
                    favorite = track.id in favoriteTrackIds,
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onClick = { onPlay(index) },
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.item("detail_track", track.id), left = CarFocusIds.DetailPlayAll)
                        .height(metrics.detailRowHeight),
                )
            }
        }
    }
}

@Composable
private fun DetailTrackHeader(layout: DetailTrackLayout, metrics: CarLayoutMetrics) {
    val colors = LocalCarColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(metrics.headerHeight).padding(horizontal = LocalCarSpacing.current.section),
    ) {
        Spacer(Modifier.width(metrics.detailRowHeight * 0.5f))
        if (layout == DetailTrackLayout.Artist) {
            Spacer(Modifier.width(metrics.detailRowHeight * 0.64f + LocalCarSpacing.current.content))
        }
        BasicText(
            "歌曲",
            style = LocalCarTypography.current.body.copy(color = colors.textSecondary),
            modifier = Modifier.weight(1.3f),
        )
        BasicText(
            if (layout == DetailTrackLayout.Album) "音质" else "专辑",
            style = LocalCarTypography.current.body.copy(color = colors.textSecondary),
            modifier = Modifier.weight(0.8f),
        )
        if (metrics.profile == CarLayoutProfile.Expanded) {
            BasicText(
                "时长",
                style = LocalCarTypography.current.body.copy(color = colors.textSecondary),
                modifier = Modifier.width(metrics.detailRowHeight),
            )
            Spacer(Modifier.width(metrics.detailRowHeight * 1.2f + LocalCarSpacing.current.small))
        }
    }
}

@Composable
private fun ErrorDetail(
    message: String,
    metrics: CarLayoutMetrics,
    navigationFocusId: CarFocusId,
    retry: () -> Unit,
    back: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.padding(metrics.contentHorizontalPadding, metrics.contentTop)) {
        CarPageState(message, Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)) {
            CarQuickActionCard("返回", "回到列表", CarIcon.Back, metrics.headerHeight, metrics.iconSize, onClick = back, modifier = Modifier.carFocusTarget(CarFocusIds.DetailBack, left = navigationFocusId).weight(1f).height(metrics.navigationItemHeight))
            CarQuickActionCard("重试", "重新载入", CarIcon.Play, metrics.headerHeight, metrics.iconSize, onClick = retry, modifier = Modifier.carFocusTarget(CarFocusIds.item("detail", "retry"), left = CarFocusIds.DetailBack).weight(1f).height(metrics.navigationItemHeight))
        }
    }
}

private sealed interface DetailResult<out T> {
    data object Loading : DetailResult<Nothing>
    data class Content<T>(val value: T) : DetailResult<T>
    data class Error(val message: String) : DetailResult<Nothing>
}

private fun DomainTrackBrowserItem.toLibraryTrackItem() =
    LibraryTrackItem(
        id = id,
        title = title,
        artist = artist,
        durationMs = durationMs,
        mediaId = mediaId,
        albumName = albumName,
        albumId = albumId,
        codec = codec,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
    )

private fun DomainPlaylistTrack.toLibraryTrackItem() =
    LibraryTrackItem(trackId, title, artist, durationMs, mediaId, albumName)

private fun DomainArtistAlbum.toLibraryAlbumItem(artist: String?) =
    LibraryAlbumItem(id, name ?: "未知专辑", year, artist)

private enum class DetailTrackLayout { Album, Artist, Playlist }
