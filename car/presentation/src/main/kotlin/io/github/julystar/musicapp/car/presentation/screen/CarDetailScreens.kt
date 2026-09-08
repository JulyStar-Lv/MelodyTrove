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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import io.github.julystar.musicapp.car.presentation.component.CarArtwork
import io.github.julystar.musicapp.car.presentation.component.CarQuickActionCard
import io.github.julystar.musicapp.car.presentation.component.CarSongRow
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.focus.CarFocusId
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.DomainAlbumDetail
import io.github.julystar.musicapp.core.domain.model.DomainArtistDetail
import io.github.julystar.musicapp.core.domain.model.DomainPlaylistTrack
import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.repository.AlbumDetailRepository
import io.github.julystar.musicapp.core.domain.repository.ArtistDetailRepository
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
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
        is DetailResult.Content -> DetailLayout(
            title = value.value.name ?: "未知歌手",
            subtitle = "${value.value.albums.size} 张专辑 · ${value.value.tracks.size} 首歌曲",
            artwork = value.value.albums.firstOrNull()?.id?.let { Artwork.LibraryAlbum(it) },
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
    val playerState by playbackController.state.collectAsState()
    val scope = rememberCoroutineScope()
    val queue = if (tracks.isEmpty()) emptyList() else tracks.toCarPlaybackRequest(0, playlistId).items
    Column(
        modifier = modifier.padding(
            start = metrics.detailContentMargin,
            top = metrics.detailContentMargin,
            end = metrics.detailContentMargin,
            bottom = metrics.navigationRailBottom,
        ),
    ) {
        CarQuickActionCard(
            title = "返回",
            summary = title,
            icon = CarIcon.Back,
            tileSize = metrics.headerHeight,
            iconSize = metrics.iconSize,
            onClick = onBack,
            modifier = Modifier
                .carFocusTarget(CarFocusIds.DetailBack, left = navigationFocusId, down = CarFocusIds.DetailPlayAll)
                .width(metrics.detailHeroWidth)
                .height(metrics.detailTopBarHeight),
        )
        Spacer(Modifier.height(metrics.detailContentMargin))
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.detailPaneGap), modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .width(metrics.detailHeroWidth)
                    .fillMaxHeight()
                    .background(LocalCarColors.current.backgroundSubtle, LocalCarShapes.current.panel)
                    .padding(LocalCarSpacing.current.pane),
            ) {
                CarArtwork(
                    artwork = artwork,
                    repository = artworkRepository,
                    size = metrics.detailHeroWidth - LocalCarSpacing.current.expansive,
                    shape = LocalCarShapes.current.panel,
                )
                Spacer(Modifier.height(LocalCarSpacing.current.pane))
                BasicText(
                    title,
                    style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(subtitle, style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textSecondary))
                Spacer(Modifier.height(LocalCarSpacing.current.section))
                CarQuickActionCard(
                    title = "播放全部",
                    summary = "${tracks.size} 首歌曲",
                    icon = CarIcon.Play,
                    tileSize = metrics.iconSize,
                    iconSize = metrics.iconSize * 0.6f,
                    enabled = queue.isNotEmpty(),
                    onClick = { if (queue.isNotEmpty()) scope.launch { playbackController.play(queue, 0) } },
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.DetailPlayAll, up = CarFocusIds.DetailBack, left = navigationFocusId)
                        .fillMaxWidth()
                        .height(metrics.headerHeight),
                )
            }
            if (tracks.isEmpty()) {
                CarPageState("这里还没有歌曲", Modifier.weight(1f).fillMaxHeight())
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.xSmall),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(LocalCarColors.current.panel, LocalCarShapes.current.panel)
                        .padding(LocalCarSpacing.current.wide),
                ) {
                    itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                        CarSongRow(
                            track = track,
                            index = index,
                            playing = playerState.currentItem?.libraryTrackId == track.id,
                            height = metrics.detailRowHeight,
                            onClick = { scope.launch { playbackController.play(queue, index) } },
                            modifier = Modifier
                                .carFocusTarget(
                                    CarFocusIds.item("detail_track", track.id),
                                    left = CarFocusIds.DetailPlayAll,
                                )
                                .height(metrics.detailRowHeight),
                        )
                    }
                }
            }
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

private fun DomainTrackBrowserItem.toLibraryTrackItem() = LibraryTrackItem(id, title, artist, durationMs, mediaId)
private fun DomainPlaylistTrack.toLibraryTrackItem() = LibraryTrackItem(trackId, title, artist, durationMs, mediaId)
