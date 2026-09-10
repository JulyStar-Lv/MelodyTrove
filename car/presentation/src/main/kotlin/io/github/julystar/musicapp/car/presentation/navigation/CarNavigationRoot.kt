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
import androidx.compose.ui.platform.LocalFocusManager
import io.github.julystar.musicapp.car.presentation.component.CarAppWindowHeader
import io.github.julystar.musicapp.car.presentation.component.CarMiniPlayer
import io.github.julystar.musicapp.car.presentation.component.CarNavigationItem
import io.github.julystar.musicapp.car.presentation.focus.CarFocusHost
import io.github.julystar.musicapp.car.presentation.focus.CarFocusId
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.focus.carInputRouter
import io.github.julystar.musicapp.car.presentation.focus.rememberCarFocusCoordinator
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfile
import io.github.julystar.musicapp.car.presentation.nowplaying.CarNowPlayingScreen
import io.github.julystar.musicapp.car.presentation.nowplaying.CarFullscreenNowPlayingScreen
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
        CarPageState("无法读取当前窗口布局", modifier)
        return
    }
    if (safeMetrics.profile == CarLayoutProfile.FullscreenCockpit) {
        CarFullscreenNowPlayingScreen(safeMetrics, modifier)
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
    val libraryError by library.loadError.collectAsState()
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
    val focusCoordinator = rememberCarFocusCoordinator()
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = detailTarget != null || route != CarRoute.Home) {
        if (detailTarget != null) detailTarget = null
        else route = if (route == CarRoute.NowPlaying) previousRootRoute else CarRoute.Home
    }

    val focusRoute = detailTarget?.focusRoute ?: route.name
    val initialFocus = when {
        detailTarget != null -> CarFocusIds.DetailBack
        route == CarRoute.NowPlaying -> CarFocusIds.NowPlayingCollapse
        else -> route.navigationFocusId
    }
    CarFocusHost(focusCoordinator, focusRoute, initialFocus, safeMetrics.profile) {
        if (route == CarRoute.NowPlaying) {
            CarNowPlayingScreen(
                metrics = safeMetrics,
                focusCoordinator = focusCoordinator,
                onCollapse = { route = previousRootRoute },
                modifier = modifier.carInputRouter(
                    focusManager = focusManager,
                    onPlayPause = playbackController::togglePlayPause,
                    onNext = playbackController::skipNext,
                    onPrevious = playbackController::skipPrevious,
                    onStop = playbackController::pause,
                ),
            )
        } else Box(modifier = modifier.carInputRouter(
            focusManager = focusManager,
            onPlayPause = playbackController::togglePlayPause,
            onNext = playbackController::skipNext,
            onPrevious = playbackController::skipPrevious,
            onStop = playbackController::pause,
        )) {
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
            is CarDetailTarget.Album -> CarAlbumDetailScreen(detail.id, safeMetrics, route.navigationFocusId, { detailTarget = null }, contentModifier)
            is CarDetailTarget.Artist -> CarArtistDetailScreen(detail.id, safeMetrics, route.navigationFocusId, { detailTarget = null }, contentModifier)
            is CarDetailTarget.Playlist -> CarPlaylistDetailScreen(detail.id, detail.title, safeMetrics, route.navigationFocusId, { detailTarget = null }, contentModifier)
            null -> when (route) {
            CarRoute.Home -> CarHomeScreen(
                metrics = safeMetrics,
                loading = !initialized,
                error = libraryError,
                tracks = tracks,
                albums = albums,
                artists = artists,
                playlists = playlists,
                statistics = statistics,
                artworkRepository = artworkRepository,
                playbackController = playbackController,
                onOpenAlbums = { route = CarRoute.Albums },
                onOpenArtists = { route = CarRoute.Artists },
                onOpenPlaylists = { route = CarRoute.Playlists },
                onAlbumClick = { detailTarget = CarDetailTarget.Album(it) },
                onArtistClick = { detailTarget = CarDetailTarget.Artist(it) },
                onPlaylistClick = { detailTarget = CarDetailTarget.Playlist(it.id, it.title) },
                onOpenSearch = { route = CarRoute.Search },
                modifier = contentModifier,
            )
            CarRoute.Songs -> CarSongsScreen(
                safeMetrics, !initialized, libraryError, tracks, playerState.currentItem?.libraryTrackId,
                playbackController, contentModifier,
            )
            CarRoute.Albums -> CarAlbumsScreen(
                safeMetrics, !initialized, libraryError, albums, artworkRepository,
                onAlbumClick = { detailTarget = CarDetailTarget.Album(it) },
                modifier = contentModifier,
            )
            CarRoute.Artists -> CarArtistsScreen(
                safeMetrics, !initialized, libraryError, artists, albums, artworkRepository,
                onArtistClick = { detailTarget = CarDetailTarget.Artist(it) },
                modifier = contentModifier,
            )
            CarRoute.Playlists -> CarPlaylistsScreen(
                safeMetrics, !initialized, playlists,
                onPlaylistClick = { detailTarget = CarDetailTarget.Playlist(it.id, it.title) },
                modifier = contentModifier,
            )
            CarRoute.Settings -> CarSettingsScreen(
                safeMetrics,
                settings,
                settingsRepository,
                focusCoordinator,
                contentModifier,
            )
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
}

private sealed interface CarDetailTarget {
    data class Album(val id: Long) : CarDetailTarget
    data class Artist(val id: Long) : CarDetailTarget
    data class Playlist(val id: Long, val title: String) : CarDetailTarget
}

private val CarDetailTarget.focusRoute: String
    get() = when (this) {
        is CarDetailTarget.Album -> "detail.album.$id"
        is CarDetailTarget.Artist -> "detail.artist.$id"
        is CarDetailTarget.Playlist -> "detail.playlist.$id"
    }

private val CarRoute.navigationFocusId: CarFocusId
    get() = when (this) {
        CarRoute.Home -> CarFocusIds.Home
        CarRoute.Playlists -> CarFocusIds.Playlists
        CarRoute.Settings -> CarFocusIds.Settings
        CarRoute.Songs -> CarFocusIds.Songs
        CarRoute.Albums -> CarFocusIds.Albums
        CarRoute.Artists -> CarFocusIds.Artists
        CarRoute.Search -> CarFocusIds.SearchField
        CarRoute.NowPlaying -> CarFocusIds.NowPlayingCollapse
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
                    .carFocusTarget(
                        id = item.navigationFocusId,
                        up = if (index == 0) CarFocusIds.Exit else null,
                        down = when (item) {
                            CarRoute.Home -> CarFocusIds.Playlists
                            CarRoute.Playlists -> CarFocusIds.Settings
                            CarRoute.Settings -> CarFocusIds.Songs
                            else -> null
                        },
                        right = if (route == item) CarFocusIds.content(item.name) else null,
                    )
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
                    .carFocusTarget(
                        id = item.navigationFocusId,
                        up = when (item) {
                            CarRoute.Songs -> CarFocusIds.Settings
                            CarRoute.Albums -> CarFocusIds.Songs
                            CarRoute.Artists -> CarFocusIds.Albums
                            else -> null
                        },
                        down = when (item) {
                            CarRoute.Songs -> CarFocusIds.Albums
                            CarRoute.Albums -> CarFocusIds.Artists
                            CarRoute.Artists -> CarFocusIds.MiniPlayer
                            else -> null
                        },
                        right = if (route == item) CarFocusIds.content(item.name) else null,
                    )
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
                .carFocusTarget(
                    id = CarFocusIds.MiniPlayer,
                    up = CarFocusIds.Artists,
                    right = CarFocusIds.content(route.name),
                )
                .offset(
                    x = metrics.navigationRailInnerPadding,
                    y = railHeight - metrics.miniPlayerBottom - metrics.miniPlayerHeight,
                )
                .width(metrics.navigationRailWidth - metrics.navigationRailInnerPadding * 2f),
        )
    }
}
