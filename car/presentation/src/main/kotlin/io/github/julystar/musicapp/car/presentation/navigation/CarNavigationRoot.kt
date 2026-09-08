package io.github.julystar.musicapp.car.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.car.presentation.component.CarAppWindowHeader
import io.github.julystar.musicapp.car.presentation.component.CarMiniPlayer
import io.github.julystar.musicapp.car.presentation.component.CarNavigationItem
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.nowplaying.CarNowPlayingScreen
import io.github.julystar.musicapp.car.presentation.screen.CarAlbumsScreen
import io.github.julystar.musicapp.car.presentation.screen.CarAlbumDetailScreen
import io.github.julystar.musicapp.car.presentation.screen.CarArtistDetailScreen
import io.github.julystar.musicapp.car.presentation.screen.CarArtistsScreen
import io.github.julystar.musicapp.car.presentation.screen.CarHomeScreen
import io.github.julystar.musicapp.car.presentation.screen.CarPageState
import io.github.julystar.musicapp.car.presentation.screen.CarPlaylistsScreen
import io.github.julystar.musicapp.car.presentation.screen.CarPlaylistDetailScreen
import io.github.julystar.musicapp.car.presentation.screen.CarSearchScreen
import io.github.julystar.musicapp.car.presentation.screen.CarSettingsScreen
import io.github.julystar.musicapp.car.presentation.screen.CarSongsScreen
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.core.domain.repository.HomeStatisticsRepository
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import io.github.julystar.musicapp.core.domain.repository.PlaylistRepository
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import io.github.julystar.musicapp.core.domain.model.AppSettings
import io.github.julystar.musicapp.core.domain.search.SearchRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import io.github.julystar.musicapp.service.playback.domain.PlayerState
import org.koin.compose.koinInject

@Composable
fun CarNavigationRoot(
    metrics: CarLayoutMetrics?,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeMetrics = metrics?.takeIf { it.metricsAvailable }
    if (safeMetrics == null) {
        CarPageState("当前窗口布局尚未完成适配", modifier)
        return
    }
    val library = koinInject<LibraryRepository>()
    val playlistsRepository = koinInject<PlaylistRepository>()
    val artworkRepository = koinInject<ArtworkRepository>()
    val playbackController = koinInject<PlaybackController>()
    val statisticsRepository = koinInject<HomeStatisticsRepository>()
    val settingsRepository = koinInject<SettingsRepository>()
    val searchRepository = koinInject<SearchRepository>()
    val initialized by library.initialLoadComplete.collectAsState()
    val tracks by library.tracks.collectAsState()
    val albums by library.albums.collectAsState()
    val artists by library.artists.collectAsState()
    val playlists by playlistsRepository.playlistSummaries.collectAsState()
    val playerState by playbackController.state.collectAsState()
    val statistics by statisticsRepository.statistics.collectAsState()
    val settings by settingsRepository.settings.collectAsState(AppSettings())
    var route by remember { mutableStateOf(CarRoute.Home) }
    var previousRootRoute by remember { mutableStateOf(CarRoute.Home) }
    var detailTarget by remember { mutableStateOf<CarDetailTarget?>(null) }

    BackHandler(enabled = detailTarget != null || route != CarRoute.Home) {
        if (detailTarget != null) detailTarget = null
        else route = if (route == CarRoute.NowPlaying) previousRootRoute else CarRoute.Home
    }

    if (route == CarRoute.NowPlaying) {
        CarNowPlayingScreen(
            metrics = safeMetrics,
            onCollapse = { route = previousRootRoute },
            modifier = modifier,
        )
        return
    }

    Box(modifier = modifier) {
        CarAppWindowHeader(
            onExit = onExit,
            modifier = Modifier
                .offset(safeMetrics.headerStart, safeMetrics.headerTop)
                .width(safeMetrics.navigationRailWidth)
                .height(safeMetrics.headerHeight),
        )
        NavigationRail(
            metrics = safeMetrics,
            route = route,
            playerState = playerState,
            artworkRepository = artworkRepository,
            playbackController = playbackController,
            onRoute = { route = it },
            onOpenNowPlaying = {
                previousRootRoute = route
                route = CarRoute.NowPlaying
            },
            modifier = Modifier
                .offset(safeMetrics.navigationRailStart, safeMetrics.navigationRailTop)
                .size(
                    safeMetrics.navigationRailWidth,
                    safeMetrics.contentSize.height - safeMetrics.navigationRailTop - safeMetrics.navigationRailBottom,
                ),
        )
        val contentModifier = Modifier
            .offset(x = safeMetrics.shellWidth)
            .width(safeMetrics.contentSize.width - safeMetrics.shellWidth)
            .fillMaxHeight()
        when (val detail = detailTarget) {
            is CarDetailTarget.Album -> CarAlbumDetailScreen(detail.id, safeMetrics, { detailTarget = null }, contentModifier)
            is CarDetailTarget.Artist -> CarArtistDetailScreen(detail.id, safeMetrics, { detailTarget = null }, contentModifier)
            is CarDetailTarget.Playlist -> CarPlaylistDetailScreen(detail.id, detail.title, safeMetrics, { detailTarget = null }, contentModifier)
            null -> when (route) {
            CarRoute.Home -> CarHomeScreen(
                metrics = safeMetrics,
                loading = !initialized,
                tracks = tracks,
                albums = albums,
                artists = artists,
                statistics = statistics,
                artworkRepository = artworkRepository,
                playbackController = playbackController,
                onOpenAlbums = { route = CarRoute.Albums },
                onOpenArtists = { route = CarRoute.Artists },
                onOpenSearch = { route = CarRoute.Search },
                modifier = contentModifier,
            )
            CarRoute.Songs -> CarSongsScreen(
                safeMetrics, !initialized, tracks, playerState.currentItem?.libraryTrackId,
                playbackController, contentModifier,
            )
            CarRoute.Albums -> CarAlbumsScreen(
                safeMetrics, !initialized, albums, artworkRepository,
                onAlbumClick = { detailTarget = CarDetailTarget.Album(it) },
                modifier = contentModifier,
            )
            CarRoute.Artists -> CarArtistsScreen(
                safeMetrics, !initialized, artists, albums, artworkRepository,
                onArtistClick = { detailTarget = CarDetailTarget.Artist(it) },
                modifier = contentModifier,
            )
            CarRoute.Playlists -> CarPlaylistsScreen(
                safeMetrics, !initialized, playlists,
                onPlaylistClick = { detailTarget = CarDetailTarget.Playlist(it.id, it.title) },
                modifier = contentModifier,
            )
            CarRoute.Settings -> CarSettingsScreen(safeMetrics, settings, settingsRepository, contentModifier)
            CarRoute.Search -> CarSearchScreen(
                safeMetrics,
                searchRepository,
                playbackController,
                playerState.currentItem?.libraryTrackId,
                contentModifier,
            )
            CarRoute.NowPlaying -> Unit
            }
        }
    }
}

private sealed interface CarDetailTarget {
    data class Album(val id: Long) : CarDetailTarget
    data class Artist(val id: Long) : CarDetailTarget
    data class Playlist(val id: Long, val title: String) : CarDetailTarget
}

@Composable
private fun NavigationRail(
    metrics: CarLayoutMetrics,
    route: CarRoute,
    playerState: PlayerState,
    artworkRepository: ArtworkRepository,
    playbackController: PlaybackController,
    onRoute: (CarRoute) -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalCarColors.current
    Box(modifier = modifier.background(colors.backgroundSubtle, RoundedCornerShape(metrics.cardGap))) {
        listOf(CarRoute.Home, CarRoute.Playlists, CarRoute.Settings).forEachIndexed { index, item ->
            CarNavigationItem(
                label = item.label,
                icon = item.icon,
                selected = route == item,
                enabled = true,
                iconSize = metrics.iconSize,
                onClick = { onRoute(item) },
                modifier = Modifier
                    .offset(
                        x = metrics.navigationRailInnerPadding,
                        y = metrics.navigationPrimaryTop + metrics.navigationItemInterval * index.toFloat(),
                    )
                    .width(metrics.navigationRailWidth - metrics.navigationRailInnerPadding * 2f)
                    .height(metrics.navigationItemHeight),
            )
        }
        BasicText(
            text = "音乐库",
            style = LocalCarTypography.current.body.copy(color = colors.textSummary),
            modifier = Modifier.offset(
                x = metrics.navigationRailInnerPadding * 2f,
                y = metrics.navigationLibraryLabelTop,
            ),
        )
        listOf(CarRoute.Songs, CarRoute.Albums, CarRoute.Artists).forEachIndexed { index, item ->
            CarNavigationItem(
                label = item.label,
                icon = item.icon,
                selected = route == item,
                enabled = true,
                iconSize = metrics.iconSize,
                onClick = { onRoute(item) },
                modifier = Modifier
                    .offset(
                        x = metrics.navigationRailInnerPadding,
                        y = metrics.navigationLibraryTop + metrics.navigationItemHeight * index.toFloat() +
                            LocalCarSpacing.current.small * index.toFloat(),
                    )
                    .width(metrics.navigationRailWidth - metrics.navigationRailInnerPadding * 2f)
                    .height(metrics.navigationItemHeight),
            )
        }
        val railHeight = metrics.contentSize.height - metrics.navigationRailTop - metrics.navigationRailBottom
        CarMiniPlayer(
            state = playerState,
            artworkRepository = artworkRepository,
            height = metrics.miniPlayerHeight,
            controlSize = metrics.iconSize,
            onOpen = onOpenNowPlaying,
            onPrevious = playbackController::skipPrevious,
            onToggle = playbackController::togglePlayPause,
            onNext = playbackController::skipNext,
            modifier = Modifier
                .offset(
                    x = metrics.navigationRailInnerPadding,
                    y = railHeight - metrics.miniPlayerBottom - metrics.miniPlayerHeight,
                )
                .width(metrics.navigationRailWidth - metrics.navigationRailInnerPadding * 2f),
        )
    }
}
