package io.github.julystar.musicapp.car.presentation.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

@JvmInline
value class CarFocusId(val value: String)

object CarFocusIds {
    val Exit = CarFocusId("shell.exit")
    val Home = CarFocusId("navigation.home")
    val Playlists = CarFocusId("navigation.playlists")
    val Settings = CarFocusId("navigation.settings")
    val Songs = CarFocusId("navigation.songs")
    val Albums = CarFocusId("navigation.albums")
    val Artists = CarFocusId("navigation.artists")
    val MiniPlayer = CarFocusId("shell.mini_player")
    val SearchField = CarFocusId("search.field")
    val DetailBack = CarFocusId("detail.back")
    val DetailPlayAll = CarFocusId("detail.play_all")
    val NowPlayingCollapse = CarFocusId("now_playing.collapse")
    val NowPlayingFullscreen = CarFocusId("now_playing.fullscreen")
    val NowPlayingShuffle = CarFocusId("now_playing.shuffle")
    val NowPlayingMore = CarFocusId("now_playing.more")
    val NowPlayingRepeat = CarFocusId("now_playing.repeat")
    val NowPlayingPrevious = CarFocusId("now_playing.previous")
    val NowPlayingToggle = CarFocusId("now_playing.toggle")
    val NowPlayingNext = CarFocusId("now_playing.next")
    val NowPlayingQueue = CarFocusId("now_playing.queue")
    val FullscreenExit = CarFocusId("fullscreen.exit")
    val FullscreenCoverFlow = CarFocusId("fullscreen.cover_flow")
    val FullscreenPrevious = CarFocusId("fullscreen.previous")
    val FullscreenToggle = CarFocusId("fullscreen.toggle")
    val FullscreenNext = CarFocusId("fullscreen.next")
    val FullscreenMode = CarFocusId("fullscreen.mode")

    fun content(route: String) = CarFocusId("content.$route")
    fun item(kind: String, stableId: Any) = CarFocusId("$kind.$stableId")
    fun queue(stableId: Any) = item("queue", stableId)
}

@Immutable
data class CarFocusRegistry(
    val ids: Set<CarFocusId> = emptySet(),
    val rememberedByRoute: Map<String, CarFocusId> = emptyMap(),
) {
    fun register(id: CarFocusId): CarFocusRegistry = copy(ids = ids + id)

    fun unregister(id: CarFocusId): CarFocusRegistry = copy(ids = ids - id)

    fun remember(route: String, id: CarFocusId): CarFocusRegistry =
        if (id in ids) copy(rememberedByRoute = rememberedByRoute + (route to id)) else this

    fun restore(route: String, fallback: CarFocusId): CarFocusId =
        rememberedByRoute[route]?.takeIf(ids::contains) ?: fallback
}

@Stable
class CarFocusCoordinator internal constructor() {
    private val requesters = mutableMapOf<CarFocusId, FocusRequester>()
    private var registry = CarFocusRegistry()

    fun requester(id: CarFocusId): FocusRequester = requesters.getOrPut(id) {
        registry = registry.register(id)
        FocusRequester()
    }

    fun remember(route: String, id: CarFocusId) {
        registry = registry.register(id).remember(route, id)
    }

    fun restore(route: String, fallback: CarFocusId): CarFocusId = registry.restore(route, fallback)

    fun requestFocus(id: CarFocusId): Boolean = runCatching { requester(id).requestFocus() }.getOrDefault(false)
}

private val LocalCarFocusCoordinator = staticCompositionLocalOf<CarFocusCoordinator> {
    error("CarFocusHost is missing")
}

private val LocalCarFocusRoute = compositionLocalOf { "root" }

@Composable
fun rememberCarFocusCoordinator(): CarFocusCoordinator = remember { CarFocusCoordinator() }

@Composable
fun CarFocusHost(
    coordinator: CarFocusCoordinator,
    route: String,
    initialFocus: CarFocusId,
    profileKey: Any? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCarFocusCoordinator provides coordinator,
        LocalCarFocusRoute provides route,
    ) {
        LaunchedEffect(route, initialFocus, profileKey) {
            repeat(3) {
                withFrameNanos { }
                val restored = coordinator.restore(route, initialFocus)
                if (coordinator.requestFocus(restored)) return@LaunchedEffect
                if (restored != initialFocus && coordinator.requestFocus(initialFocus)) return@LaunchedEffect
            }
        }
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.carFocusTarget(
    id: CarFocusId,
    up: CarFocusId? = null,
    down: CarFocusId? = null,
    left: CarFocusId? = null,
    right: CarFocusId? = null,
): Modifier {
    val coordinator = LocalCarFocusCoordinator.current
    val route = LocalCarFocusRoute.current
    val bringIntoViewRequester = remember(id) { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val requester = coordinator.requester(id)
    return this
        .focusRequester(requester)
        .focusProperties {
            up?.let { this.up = coordinator.requester(it) }
            down?.let { this.down = coordinator.requester(it) }
            left?.let { this.left = coordinator.requester(it) }
            right?.let { this.right = coordinator.requester(it) }
        }
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged { state ->
            if (state.isFocused) {
                coordinator.remember(route, id)
                scope.launch { bringIntoViewRequester.bringIntoView() }
            }
        }
        .semantics { carFocusId = id.value }
}

@Composable
fun Modifier.carInputRouter(
    focusManager: FocusManager,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
            Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
            Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Left)
            Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Right)
            Key.MediaPlayPause -> true.also { onPlayPause() }
            Key.MediaNext -> true.also { onNext() }
            Key.MediaPrevious -> true.also { onPrevious() }
            Key.MediaStop -> true.also { onStop() }
            else -> false
        }
    }
    .onRotaryScrollEvent { event ->
        when {
            event.verticalScrollPixels > 0f -> focusManager.moveFocus(FocusDirection.Down)
            event.verticalScrollPixels < 0f -> focusManager.moveFocus(FocusDirection.Up)
            else -> false
        }
    }

val CarFocusIdSemanticsKey = SemanticsPropertyKey<String>("CarFocusId")

var SemanticsPropertyReceiver.carFocusId by CarFocusIdSemanticsKey
