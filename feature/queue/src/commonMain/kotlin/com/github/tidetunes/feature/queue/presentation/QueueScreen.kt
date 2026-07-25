package com.github.tidetunes.feature.queue.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.domain.model.PlayerInteractionSettings
import com.github.tidetunes.core.presentation.components.TideContextMenu
import com.github.tidetunes.core.presentation.components.TideContextMenuItem
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonColors
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.service.playback.domain.RepeatMode
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingAction
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingProgressPanel
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingProgressState
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.service.playback.presentation.generated.resources.Res as PlaybackRes
import tidetunes.service.playback.presentation.generated.resources.icon_back
import tidetunes.service.playback.presentation.generated.resources.icon_heart
import tidetunes.service.playback.presentation.generated.resources.icon_mode_repeat
import tidetunes.service.playback.presentation.generated.resources.icon_pause
import tidetunes.service.playback.presentation.generated.resources.icon_play
import tidetunes.service.playback.presentation.generated.resources.icon_play_next
import tidetunes.service.playback.presentation.generated.resources.icon_play_previous
import tidetunes.service.playback.presentation.generated.resources.icon_vertialcal_more
import tidetunes.service.playback.presentation.generated.resources.music_player_context_menu_remove
import tidetunes.service.playback.presentation.generated.resources.player_add_favorite
import tidetunes.service.playback.presentation.generated.resources.player_close
import tidetunes.service.playback.presentation.generated.resources.player_pause
import tidetunes.service.playback.presentation.generated.resources.player_play
import tidetunes.service.playback.presentation.generated.resources.player_remove_favorite
import tidetunes.service.playback.presentation.generated.resources.player_unknown_artist
import tidetunes.feature.queue.generated.resources.Res as QueueRes
import tidetunes.feature.queue.generated.resources.queue_clear
import tidetunes.feature.queue.generated.resources.queue_empty
import tidetunes.feature.queue.generated.resources.queue_history
import tidetunes.feature.queue.generated.resources.queue_track_count
import tidetunes.feature.queue.generated.resources.queue_up_next
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.time.Duration

private val QueueBackground = Color(0xFF0C0A14)

@Composable
fun QueueScreen(
    state: QueueState,
    nowPlayingState: NowPlayingState,
    currentDuration: Duration,
    bufferDuration: Duration,
    playerDuration: Duration,
    playerInteractionSettings: PlayerInteractionSettings,
    onAction: (QueueAction) -> Unit,
    onPlayerAction: (NowPlayingAction) -> Unit,
) {
    val track = nowPlayingState.currentTrack
    val currentItem = state.items.firstOrNull(QueueItemUi::isCurrent)
    val title = track?.title ?: currentItem?.title.orEmpty()
    val artist = track?.artist ?: currentItem?.artist

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QueueBackground)
            .clipToBounds(),
    ) {
        ArtworkImage(
            artwork = track?.artwork ?: currentItem?.artwork,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.46f
                    scaleX = 1.14f
                    scaleY = 1.14f
                    renderEffect = BlurEffect(88f, 88f, TileMode.Decal)
                },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF151126).copy(alpha = 0.42f),
                            QueueBackground.copy(alpha = 0.68f),
                            QueueBackground.copy(alpha = 0.94f),
                        ),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            QueueTrackHeader(
                title = title,
                artist = artist,
                artwork = track?.artwork ?: currentItem?.artwork,
                hasCurrentTrack = track != null,
                onNavigateBack = { onAction(QueueAction.NavigateBack) },
                onPlayerAction = onPlayerAction,
            )
            QueueModeControls(
                shuffleEnabled = state.isShuffleEnabled,
                repeatMode = nowPlayingState.controls.repeatMode,
                onShuffle = { onAction(QueueAction.ToggleShuffle) },
                onCycleRepeat = { onPlayerAction(NowPlayingAction.CycleRepeatMode) },
            )
            QueueContent(
                state = state,
                onAction = onAction,
                modifier = Modifier.weight(1f),
            )
            QueuePlaybackPanel(
                nowPlayingState = nowPlayingState,
                currentDuration = currentDuration,
                bufferDuration = bufferDuration,
                playerDuration = playerDuration,
                trackDurationMs = track?.durationMs,
                playerInteractionSettings = playerInteractionSettings,
                onPlayerAction = onPlayerAction,
            )
        }
    }
}

@Composable
private fun QueueTrackHeader(
    title: String,
    artist: String?,
    artwork: com.github.tidetunes.core.domain.model.Artwork?,
    hasCurrentTrack: Boolean,
    onNavigateBack: () -> Unit,
    onPlayerAction: (NowPlayingAction) -> Unit,
) {
    var liked by remember(title) { mutableStateOf(true) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(84.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(PlaybackRes.drawable.icon_back),
            contentDescription = stringResource(PlaybackRes.string.player_close),
            colors = TideIconButtonColors(iconTint = Color.White),
            onClick = onNavigateBack,
        )
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(13.dp)),
        ) {
            ArtworkImage(
                artwork = artwork,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MiuixTheme.textStyles.title3.copy(fontSize = 19.sp, lineHeight = 23.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist ?: stringResource(PlaybackRes.string.player_unknown_artist),
                color = Color.White.copy(alpha = 0.62f),
                style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp, lineHeight = 19.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hasCurrentTrack) {
            TideIconButton(
                size = TideIconButtonSize.Touch,
                variant = TideIconButtonVariant.Default,
                painter = painterResource(PlaybackRes.drawable.icon_heart),
                contentDescription = stringResource(
                    if (liked) PlaybackRes.string.player_remove_favorite else PlaybackRes.string.player_add_favorite,
                ),
                colors = queueHeaderButtonColors(
                    iconTint = if (liked) MiuixTheme.colorScheme.primary else Color.White,
                ),
                onClick = { liked = !liked },
            )
            Box {
                TideIconButton(
                    size = TideIconButtonSize.Touch,
                    variant = TideIconButtonVariant.Default,
                    painter = painterResource(PlaybackRes.drawable.icon_vertialcal_more),
                    contentDescription = null,
                    colors = queueHeaderButtonColors(),
                    onClick = { moreMenuExpanded = true },
                )
                TideContextMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                    items = listOf(
                        TideContextMenuItem(
                            label = PlaybackRes.string.music_player_context_menu_remove,
                            isError = true,
                            onClick = {
                                moreMenuExpanded = false
                                onPlayerAction(NowPlayingAction.RemoveCurrentTrack)
                            },
                        ),
                    ),
                )
            }
        }
    }
}

private fun queueHeaderButtonColors(iconTint: Color = Color.White): TideIconButtonColors =
    TideIconButtonColors(
        buttonBg = Color.White.copy(alpha = 0.10f),
        iconTint = iconTint,
    )

@Composable
private fun QueueModeControls(
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QueueModeButton(
            modifier = Modifier.weight(1f),
            active = shuffleEnabled,
            onClick = onShuffle,
        ) {
            Text(
                text = "⇄",
                color = Color.White.copy(alpha = if (shuffleEnabled) 1f else 0.74f),
                style = MiuixTheme.textStyles.title2.copy(fontSize = 25.sp, lineHeight = 28.sp),
            )
        }
        QueueModeButton(
            modifier = Modifier.weight(1f),
            painter = painterResource(PlaybackRes.drawable.icon_mode_repeat),
            active = repeatMode != RepeatMode.Off,
            onClick = onCycleRepeat,
        )
        QueueModeButton(
            modifier = Modifier.weight(1f),
            active = repeatMode == RepeatMode.All,
            onClick = onCycleRepeat,
        ) {
            Text(
                text = "∞",
                color = Color.White.copy(alpha = if (repeatMode == RepeatMode.All) 1f else 0.74f),
                style = MiuixTheme.textStyles.title2.copy(fontSize = 27.sp, lineHeight = 28.sp),
            )
        }
    }
}

@Composable
private fun QueueModeButton(
    painter: Painter? = null,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = if (active) 0.18f else 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (active) 1f else 0.74f),
                modifier = Modifier.size(23.dp),
            )
        } else {
            content?.invoke()
        }
    }
}

@Composable
private fun QueueContent(
    state: QueueState,
    onAction: (QueueAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentItems = state.items.filter(QueueItemUi::isCurrent)
    val upcomingItems = state.items.filterNot(QueueItemUi::isCurrent)

    if (state.items.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(QueueRes.string.queue_empty),
                color = Color.White.copy(alpha = 0.48f),
                style = MiuixTheme.textStyles.body1,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        item {
            QueueSectionHeader(
                title = stringResource(QueueRes.string.queue_history),
                trailing = stringResource(QueueRes.string.queue_clear),
                onTrailingClick = { onAction(QueueAction.ClearQueue) },
            )
        }
        items(currentItems, key = { item -> item.lazyListKey(item.index) }) { item ->
            QueueTrackRow(
                item = item,
                onPlay = { onAction(QueueAction.PlayItem(item.index)) },
            )
        }
        if (upcomingItems.isNotEmpty()) {
            item {
                QueueSectionHeader(
                    title = stringResource(QueueRes.string.queue_up_next),
                    subtitle = stringResource(QueueRes.string.queue_track_count, upcomingItems.size),
                )
            }
            items(upcomingItems, key = { item -> item.lazyListKey(item.index) }) { item ->
                QueueTrackRow(
                    item = item,
                    onPlay = { onAction(QueueAction.PlayItem(item.index)) },
                )
            }
        }
    }
}

@Composable
private fun QueueSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = MiuixTheme.textStyles.title3.copy(fontSize = 21.sp, lineHeight = 28.sp),
                fontWeight = FontWeight.Bold,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.42f),
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp, lineHeight = 20.sp),
                )
            }
        }
        if (trailing != null && onTrailingClick != null) {
            Text(
                text = trailing,
                color = Color.White.copy(alpha = 0.70f),
                style = MiuixTheme.textStyles.body2.copy(fontSize = 15.sp),
                modifier = Modifier
                    .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onTrailingClick)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun QueueTrackRow(
    item: QueueItemUi,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (item.isCurrent) Color(0xFF0A0712).copy(alpha = 0.20f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(11.dp)),
        ) {
            ArtworkImage(
                artwork = item.artwork,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = if (item.isCurrent) MiuixTheme.colorScheme.primary else Color.White,
                style = MiuixTheme.textStyles.body1.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.artist?.let { artist ->
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.46f),
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!item.isCurrent) {
            Text(
                text = "⋮⋮",
                color = Color.White.copy(alpha = 0.28f),
                style = MiuixTheme.textStyles.body2.copy(fontSize = 18.sp, letterSpacing = (-5).sp),
            )
        }
    }
}

@Composable
private fun QueuePlaybackPanel(
    nowPlayingState: NowPlayingState,
    currentDuration: Duration,
    bufferDuration: Duration,
    playerDuration: Duration,
    trackDurationMs: Long?,
    playerInteractionSettings: PlayerInteractionSettings,
    onPlayerAction: (NowPlayingAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp)
            .navigationBarsPadding(),
    ) {
        NowPlayingProgressPanel(
            progressState = NowPlayingProgressState(
                currentDuration = currentDuration,
                bufferDuration = bufferDuration,
                playerDuration = playerDuration,
            ),
            trackDurationMs = trackDurationMs,
            playerInteractionSettings = playerInteractionSettings,
            onAction = onPlayerAction,
            lightTheme = true,
        )
        QueueTransportControls(
            nowPlayingState = nowPlayingState,
            onPlayerAction = onPlayerAction,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QueueTransportControls(
    nowPlayingState: NowPlayingState,
    onPlayerAction: (NowPlayingAction) -> Unit,
) {
    val controls = nowPlayingState.controls
    val queue = nowPlayingState.queue

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .widthIn(max = 286.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueTransportButton(
                painter = painterResource(PlaybackRes.drawable.icon_play_previous),
                contentDescription = null,
                enabled = queue.canPlayPrevious,
                onClick = { onPlayerAction(NowPlayingAction.PlayPrevious) },
            )
            QueueTransportButton(
                painter = painterResource(
                    if (controls.isPlaying) PlaybackRes.drawable.icon_pause else PlaybackRes.drawable.icon_play,
                ),
                contentDescription = stringResource(
                    if (controls.isPlaying) PlaybackRes.string.player_pause else PlaybackRes.string.player_play,
                ),
                primary = true,
                enabled = controls.isPlaying || !controls.isLoading,
                onClick = {
                    onPlayerAction(if (controls.isPlaying) NowPlayingAction.Pause else NowPlayingAction.Resume)
                },
            )
            QueueTransportButton(
                painter = painterResource(PlaybackRes.drawable.icon_play_next),
                contentDescription = null,
                enabled = queue.canPlayNext,
                onClick = { onPlayerAction(NowPlayingAction.PlayNext) },
            )
        }
    }
}

@Composable
private fun QueueTransportButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(if (primary) 58.dp else 52.dp)
            .clip(CircleShape)
            .background(if (primary) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (enabled) 0.92f else 0.28f),
            modifier = Modifier.size(if (primary) 30.dp else 28.dp),
        )
    }
}

internal fun QueueItemUi.lazyListKey(index: Int): String = "queue-item-$index-${this.index}"
