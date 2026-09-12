package io.github.julystar.musicapp.car.presentation.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.julystar.musicapp.car.presentation.component.CarArtwork
import io.github.julystar.musicapp.car.presentation.component.carInteractiveSurface
import io.github.julystar.musicapp.car.presentation.focus.CarFocusHost
import io.github.julystar.musicapp.car.presentation.focus.CarFocusId
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.focus.carInputRouter
import io.github.julystar.musicapp.car.presentation.focus.rememberCarFocusCoordinator
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as IconView
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.service.playback.domain.NowPlayingRepository
import io.github.julystar.musicapp.service.playback.domain.PlayableItem
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt

/** Figma 1480:1130 (minimal player) and 1524:1130 (fullscreen Cover Flow). */
@Composable
fun CarFullscreenNowPlayingScreen(
    metrics: CarLayoutMetrics,
    onExitPlayback: () -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = koinInject<PlaybackController>()
    val nowPlaying = koinInject<NowPlayingRepository>()
    val artworkRepository = koinInject<ArtworkRepository>()
    val state by controller.state.collectAsState()
    val position by controller.position.collectAsState()
    val queue by controller.queue.collectAsState()
    val trackInfo by nowPlaying.currentTrackInfo.collectAsState()
    var coverFlow by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusCoordinator = rememberCarFocusCoordinator()
    val focusManager = LocalFocusManager.current
    val currentIndex = queue.currentIndex.takeIf { it in queue.items.indices } ?: 0
    val focusRoute = if (coverFlow) "fullscreen.cover_flow" else "fullscreen.now_playing"
    val initialFocus = if (coverFlow && queue.items.isNotEmpty()) {
        CarFocusIds.item("fullscreen_cover", currentIndex)
    } else {
        CarFocusIds.FullscreenCoverFlow
    }
    val currentArtwork = trackInfo?.artwork
        ?: state.currentItem?.libraryTrackId?.let { Artwork.LibraryTrack(it, true) }
    val lyricLines = trackInfo?.lyrics?.lines.orEmpty()
    val lyricIndex = lyricLines.indexOfLast { it.duration.inWholeMilliseconds <= position.positionMs }

    BackHandler {
        if (coverFlow) coverFlow = false else onExitFullscreen()
    }

    CarFocusHost(focusCoordinator, focusRoute, initialFocus, metrics.profile) {
        Box(
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .carInputRouter(
                    focusManager = focusManager,
                    onPlayPause = controller::togglePlayPause,
                    onNext = controller::skipNext,
                    onPrevious = controller::skipPrevious,
                    onStop = controller::pause,
                ),
        ) {
            FullscreenArtworkBackground(currentArtwork, artworkRepository)
            FullscreenExitPlaybackButton(onExitPlayback, Modifier.offset(96.dp, 216.dp))
            FullscreenExitButton(onExitFullscreen, Modifier.offset(184.dp, 216.dp))
            FullscreenTrackMeta(
                title = state.currentItem?.title ?: "尚未播放",
                artist = trackInfo?.artist?.takeIf(String::isNotBlank) ?: state.currentItem?.artist.orEmpty(),
                modifier = Modifier.offset(280.dp, 228.5.dp),
            )

            if (coverFlow) {
                if (queue.items.isEmpty()) {
                    BasicText(
                        "播放队列为空",
                        style = TextStyle(fontSize = 64.sp, color = Color.White.copy(alpha = 0.56f)),
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    FullscreenCoverFlow(
                        items = queue.items,
                        currentIndex = currentIndex,
                        artworkRepository = artworkRepository,
                        onPlay = { queueIndex -> scope.launch { controller.play(queue.items, queueIndex) } },
                        modifier = Modifier.fillMaxWidth().height(820.dp).offset(y = 380.dp),
                    )
                    Box(
                        Modifier
                            .offset(2518.dp, 1260.dp)
                            .size(width = 84.dp, height = 8.dp)
                            .background(Color.White.copy(alpha = 0.78f), RoundedCornerShape(4.dp)),
                    )
                }
            } else {
                FullscreenMinimalContent(
                    artwork = currentArtwork,
                    artworkRepository = artworkRepository,
                    currentLyric = lyricLines.getOrNull(lyricIndex)?.text,
                    nextLyric = lyricLines.getOrNull(lyricIndex + 1)?.text,
                    onOpenCoverFlow = { coverFlow = true },
                    modifier = Modifier.offset(320.dp, 394.25.dp),
                )
            }
        }
    }
}

@Composable
private fun FullscreenArtworkBackground(
    artwork: Artwork?,
    artworkRepository: ArtworkRepository,
) {
    CarArtwork(
        artwork = artwork,
        repository = artworkRepository,
        size = 1.dp,
        shape = RectangleShape,
        fillBounds = true,
        modifier = Modifier.fillMaxSize().graphicsLayer {
            scaleX = 5600f / 5120f
            scaleY = 1784f / 1304f
        }.blur(120.dp),
    )
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.64f)))
}

@Composable
private fun FullscreenExitPlaybackButton(
    onExitPlayback: () -> Unit,
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .carFocusTarget(CarFocusIds.NowPlayingCollapse, right = CarFocusIds.FullscreenExit)
            .size(72.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .carInteractiveSurface(
                shape = RoundedCornerShape(20.dp),
                defaultColor = Color.Black.copy(alpha = 0.22f),
                onClick = onExitPlayback,
            ),
    ) {
        IconView(CarIcon.Collapse, "退出播放界面", Color.White, Modifier.size(56.dp))
    }
}

@Composable
private fun FullscreenExitButton(
    onExitFullscreen: () -> Unit,
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .carFocusTarget(
                CarFocusIds.FullscreenExit,
                left = CarFocusIds.NowPlayingCollapse,
                right = CarFocusIds.FullscreenCoverFlow,
            )
            .size(72.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .carInteractiveSurface(
                shape = RoundedCornerShape(20.dp),
                defaultColor = Color.Black.copy(alpha = 0.22f),
                onClick = onExitFullscreen,
            ),
    ) {
        IconView(CarIcon.ExitFullscreen, "退出全屏", Color.White, Modifier.size(56.dp))
    }
}

@Composable
private fun FullscreenTrackMeta(
    title: String,
    artist: String,
    modifier: Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier.width(1600.dp)) {
        BasicText(
            title,
            style = TextStyle(
                color = Color(0xFFF7F7F7),
                fontSize = 48.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            artist,
            style = TextStyle(color = Color(0xFFC7C7C7), fontSize = 28.sp, lineHeight = 36.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FullscreenMinimalContent(
    artwork: Artwork?,
    artworkRepository: ArtworkRepository,
    currentLyric: String?,
    nextLyric: String?,
    onOpenCoverFlow: () -> Unit,
    modifier: Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(224.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.width(4544.dp).height(840.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .carFocusTarget(CarFocusIds.FullscreenCoverFlow, left = CarFocusIds.FullscreenExit)
                .size(720.dp)
                .carInteractiveSurface(RoundedCornerShape(36.dp), defaultColor = Color.Transparent, onClick = onOpenCoverFlow),
        ) {
            CarArtwork(
                artwork = artwork,
                repository = artworkRepository,
                size = 720.dp,
                shape = RoundedCornerShape(36.dp),
                modifier = Modifier,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.width(3528.dp).height(558.dp)) {
            BasicText(
                currentLyric ?: "暂无歌词",
                style = TextStyle(
                    color = Color(0xFFF7F7F7),
                    fontSize = 156.sp,
                    lineHeight = 190.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(380.dp),
            )
            BasicText(
                nextLyric.orEmpty(),
                style = TextStyle(
                    color = Color(0xFFF7F7F7).copy(alpha = 0.56f),
                    fontSize = 116.sp,
                    lineHeight = 150.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(150.dp),
            )
        }
    }
}

@Composable
private fun CoverFlowItem(
    item: PlayableItem,
    distanceFromCenter: Float,
    artworkRepository: ArtworkRepository,
    focusId: CarFocusId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val absoluteDistance = distanceFromCenter.absoluteValue
    val artworkWidth = coverFlowArtworkWidth(absoluteDistance)
    val artworkHeight = coverFlowArtworkHeight(absoluteDistance)
    val selected = absoluteDistance < 0.5f
    val artworkShape = RoundedCornerShape(COVER_FLOW_ARTWORK_CORNER_RADIUS.dp)
    val infoTop = coverFlowInfoTop(absoluteDistance)
    val infoWidth = (artworkWidth - 40f).coerceAtLeast(320f).dp
    Box(modifier = modifier.width(720.dp).height(832.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(720.dp)) {
            CarArtwork(
                artwork = item.libraryTrackId?.let { Artwork.LibraryTrack(it, true) },
                repository = artworkRepository,
                size = 720.dp,
                shape = artworkShape,
                modifier = Modifier
                    .requiredSize(720.dp)
                    .graphicsLayer {
                        scaleX = artworkWidth / 720f
                        scaleY = artworkHeight / 720f
                        rotationY = if (absoluteDistance < 0.01f) 0f else {
                            -distanceFromCenter.coerceIn(-1f, 1f) * 13f
                        }
                        cameraDistance = COVER_FLOW_CAMERA_DISTANCE * density
                        alpha = coverFlowArtworkAlpha(absoluteDistance)
                        shape = artworkShape
                        clip = true
                    }
                    .carFocusTarget(focusId)
                    .carInteractiveSurface(
                        artworkShape,
                        selected = selected,
                        defaultColor = Color.Transparent,
                        onClick = onClick,
                ),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = infoTop.dp).width(infoWidth),
        ) {
            BasicText(
                item.title,
                style = TextStyle(
                    color = Color(0xFFF7F7F7),
                    fontSize = 36.sp,
                    lineHeight = 46.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(46.dp),
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                item.artist.orEmpty(),
                style = TextStyle(
                    color = Color(0xFFF7F7F7).copy(alpha = 0.56f),
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(32.dp),
            )
        }
    }
}

@Composable
private fun FullscreenCoverFlow(
    items: List<PlayableItem>,
    currentIndex: Int,
    artworkRepository: ArtworkRepository,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val anchor = items.size * COVER_FLOW_ANCHOR_REPEAT + currentIndex
    var position by remember(items.size) { mutableFloatStateOf(anchor.toFloat()) }
    val dragIntervalPx = with(density) { COVER_FLOW_DRAG_INTERVAL.dp.toPx() }
    val draggableState = rememberDraggableState { deltaPx ->
        position -= deltaPx / dragIntervalPx
    }

    LaunchedEffect(currentIndex, items.size) {
        val nearestCycle = ((position - currentIndex) / items.size).roundToInt()
        val target = nearestCycle * items.size + currentIndex
        animate(
            initialValue = position,
            targetValue = target.toFloat(),
            animationSpec = tween(COVER_FLOW_SETTLE_MILLIS, easing = FastOutSlowInEasing),
        ) { value, _ -> position = value }
    }

    Box(
        modifier = modifier.draggable(
            state = draggableState,
            orientation = Orientation.Horizontal,
            onDragStopped = { velocityPxPerSecond ->
                val projected = position - velocityPxPerSecond / dragIntervalPx * COVER_FLOW_FLING_SECONDS
                val target = projected.roundToInt()
                animate(
                    initialValue = position,
                    targetValue = target.toFloat(),
                    animationSpec = tween(COVER_FLOW_SETTLE_MILLIS, easing = FastOutSlowInEasing),
                ) { value, _ -> position = value }
            },
        ),
    ) {
        val firstVisible = floor(position).toInt() - COVER_FLOW_SIDE_ITEMS
        val lastVisible = floor(position).toInt() + COVER_FLOW_SIDE_ITEMS + 1
        for (virtualIndex in firstVisible..lastVisible) {
            val distance = virtualIndex - position
            if (distance.absoluteValue > COVER_FLOW_SIDE_ITEMS + 0.75f) continue
            val queueIndex = virtualIndex.floorMod(items.size)
            val centerOffset = coverFlowCenterOffset(distance)
            key(virtualIndex) {
                CoverFlowItem(
                    item = items[queueIndex],
                    distanceFromCenter = distance,
                    artworkRepository = artworkRepository,
                    focusId = CarFocusIds.item("fullscreen_cover", virtualIndex),
                    onClick = {
                        onPlay(queueIndex)
                        position = virtualIndex.toFloat()
                    },
                    modifier = Modifier
                        .offset(x = 2200.dp + centerOffset.dp)
                        .zIndex(COVER_FLOW_SIDE_ITEMS + 1f - distance.absoluteValue),
                )
            }
        }
    }
}

private fun coverFlowCenterOffset(distance: Float): Float {
    val absolute = distance.absoluteValue.coerceAtMost(COVER_FLOW_SIDE_ITEMS.toFloat())
    val lower = floor(absolute).toInt()
    val upper = (lower + 1).coerceAtMost(COVER_FLOW_SIDE_ITEMS)
    val fraction = absolute - lower
    val offset = COVER_FLOW_CENTER_OFFSETS[lower] +
        (COVER_FLOW_CENTER_OFFSETS[upper] - COVER_FLOW_CENTER_OFFSETS[lower]) * fraction
    return if (distance < 0f) -offset else offset
}

private fun coverFlowArtworkWidth(distance: Float): Float =
    coverFlowInterpolated(distance, COVER_FLOW_ARTWORK_WIDTHS)

private fun coverFlowArtworkHeight(distance: Float): Float =
    coverFlowInterpolated(distance, COVER_FLOW_ARTWORK_HEIGHTS)

private fun coverFlowArtworkAlpha(distance: Float): Float =
    coverFlowInterpolated(distance, COVER_FLOW_ARTWORK_ALPHAS)

private fun coverFlowInfoTop(distance: Float): Float =
    750f + (684f - 750f) * distance.coerceIn(0f, 1f)

private fun coverFlowInterpolated(distance: Float, values: FloatArray): Float {
    val absolute = distance.coerceIn(0f, COVER_FLOW_SIDE_ITEMS.toFloat())
    val lower = floor(absolute).toInt()
    val upper = (lower + 1).coerceAtMost(COVER_FLOW_SIDE_ITEMS)
    val fraction = absolute - lower
    return values[lower] + (values[upper] - values[lower]) * fraction
}

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

private const val COVER_FLOW_SIDE_ITEMS = 5
private const val COVER_FLOW_ANCHOR_REPEAT = 100
private const val COVER_FLOW_DRAG_INTERVAL = 463f
private const val COVER_FLOW_FLING_SECONDS = 0.12f
private const val COVER_FLOW_SETTLE_MILLIS = 220
private const val COVER_FLOW_CAMERA_DISTANCE = 2400f
private const val COVER_FLOW_ARTWORK_CORNER_RADIUS = 28f
private val COVER_FLOW_CENTER_OFFSETS = floatArrayOf(0f, 582.5f, 1045.5f, 1461f, 1847f, 2194f)
private val COVER_FLOW_ARTWORK_WIDTHS = floatArrayOf(720f, 517f, 441f, 430f, 400f, 360f)
private val COVER_FLOW_ARTWORK_HEIGHTS = floatArrayOf(720f, 586f, 584f, 583f, 582f, 581f)
private val COVER_FLOW_ARTWORK_ALPHAS = floatArrayOf(1f, 1f, 0.96f, 0.92f, 0.88f, 0.84f)
