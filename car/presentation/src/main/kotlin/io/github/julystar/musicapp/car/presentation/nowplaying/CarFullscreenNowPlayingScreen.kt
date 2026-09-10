package io.github.julystar.musicapp.car.presentation.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.julystar.musicapp.car.presentation.component.CarArtwork
import io.github.julystar.musicapp.car.presentation.component.carInteractiveSurface
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.CarFocusHost
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.focus.carInputRouter
import io.github.julystar.musicapp.car.presentation.focus.rememberCarFocusCoordinator
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as IconView
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.service.playback.domain.NowPlayingRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import io.github.julystar.musicapp.service.playback.domain.PlaybackStatus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Structural implementation of Figma 1480:1130 and 1524:1130. */
@Composable
fun CarFullscreenNowPlayingScreen(metrics: CarLayoutMetrics, modifier: Modifier = Modifier) {
    val controller = koinInject<PlaybackController>()
    val nowPlaying = koinInject<NowPlayingRepository>()
    val artworkRepository = koinInject<ArtworkRepository>()
    val state by controller.state.collectAsState()
    val queue by controller.queue.collectAsState()
    val trackInfo by nowPlaying.currentTrackInfo.collectAsState()
    var coverFlow by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusCoordinator = rememberCarFocusCoordinator()
    val focusManager = LocalFocusManager.current
    BackHandler(enabled = coverFlow) { coverFlow = false }
    val colors = LocalCarColors.current
    val centerIndex = queue.currentIndex.takeIf { it in queue.items.indices } ?: 0
    val focusRoute = if (coverFlow) "fullscreen.cover_flow" else "fullscreen.now_playing"
    val initialFocus = if (coverFlow) {
        CarFocusIds.item("fullscreen_cover", centerIndex)
    } else {
        CarFocusIds.FullscreenToggle
    }
    CarFocusHost(focusCoordinator, focusRoute, initialFocus, metrics.profile) {
        Box(
            modifier
                .fillMaxSize()
                .background(colors.backgroundBase)
                .carInputRouter(
                    focusManager = focusManager,
                    onPlayPause = controller::togglePlayPause,
                    onNext = controller::skipNext,
                    onPrevious = controller::skipPrevious,
                    onStop = controller::pause,
                ),
        ) {
            if (coverFlow) {
                val visible = (-2..2).mapNotNull { offset ->
                    val index = centerIndex + offset
                    queue.items.getOrNull(index)?.let { Triple(index, offset, it) }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = metrics.contentHorizontalPadding),
                ) {
                    visible.forEach { (index, offset, item) ->
                        val size = if (offset == 0) metrics.nowPlayingArtworkSize else metrics.nowPlayingArtworkSize * 0.82f
                        CarArtwork(
                            artwork = item.libraryTrackId?.let { Artwork.LibraryTrack(it, true) },
                            repository = artworkRepository,
                            size = size,
                            shape = LocalCarShapes.current.panel,
                            modifier = Modifier
                                .carFocusTarget(CarFocusIds.item("fullscreen_cover", index))
                                .carInteractiveSurface(LocalCarShapes.current.panel, selected = offset == 0) {
                                    scope.launch { controller.play(queue.items, index) }
                                },
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.nowPlayingPaneGap),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = metrics.nowPlayingHorizontalMargin),
                ) {
                    CarArtwork(
                        artwork = trackInfo?.artwork ?: state.currentItem?.libraryTrackId?.let { Artwork.LibraryTrack(it, true) },
                        repository = artworkRepository,
                        size = metrics.nowPlayingArtworkSize,
                        shape = LocalCarShapes.current.panel,
                    )
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(
                            state.currentItem?.title ?: "尚未播放",
                            style = LocalCarTypography.current.display.copy(color = colors.textPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            state.currentItem?.artist.orEmpty(),
                            style = LocalCarTypography.current.headline.copy(color = colors.textSecondary),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(LocalCarSpacing.current.wide))
                        val lyrics = trackInfo?.lyrics?.lines.orEmpty()
                        val current = lyrics.firstOrNull()?.text ?: "暂无歌词"
                        BasicText(
                            current,
                            style = LocalCarTypography.current.pageTitle.copy(
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                            ),
                        )
                        Spacer(Modifier.height(LocalCarSpacing.current.wide))
                        Row(horizontalArrangement = Arrangement.spacedBy(metrics.cardGap), verticalAlignment = Alignment.CenterVertically) {
                            FullscreenControl(metrics, CarIcon.PreviousLarge, "上一首", CarFocusIds.FullscreenPrevious) { controller.skipPrevious() }
                            FullscreenControl(
                                metrics,
                                if (state.status == PlaybackStatus.Playing) CarIcon.PauseLarge else CarIcon.Play,
                                if (state.status == PlaybackStatus.Playing) "暂停" else "播放",
                                CarFocusIds.FullscreenToggle,
                            ) { controller.togglePlayPause() }
                            FullscreenControl(metrics, CarIcon.NextLarge, "下一首", CarFocusIds.FullscreenNext) { controller.skipNext() }
                            FullscreenControl(metrics, CarIcon.Albums, "封面流", CarFocusIds.FullscreenMode) { coverFlow = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenControl(
    metrics: CarLayoutMetrics,
    icon: CarIcon,
    description: String,
    focusId: io.github.julystar.musicapp.car.presentation.focus.CarFocusId,
    action: () -> Unit,
) {
    val colors = LocalCarColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .carFocusTarget(focusId)
            .size(metrics.primaryTouchTarget)
            .carInteractiveSurface(LocalCarShapes.current.control, onClick = action),
    ) {
        IconView(icon, description, colors.textPrimary, Modifier.size(metrics.iconSize))
    }
}
