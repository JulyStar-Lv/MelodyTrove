package com.github.tidetunes.service.playback.presentation.nowplaying

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import tidetunes.service.playback.presentation.generated.resources.Res
import tidetunes.service.playback.presentation.generated.resources.downloads_title
import tidetunes.service.playback.presentation.generated.resources.icon_back
import tidetunes.service.playback.presentation.generated.resources.icon_lyrics
import tidetunes.service.playback.presentation.generated.resources.icon_mode_list
import tidetunes.service.playback.presentation.generated.resources.icon_mode_one
import tidetunes.service.playback.presentation.generated.resources.icon_mode_repeat
import tidetunes.service.playback.presentation.generated.resources.icon_mode_repeatone
import tidetunes.service.playback.presentation.generated.resources.icon_pause
import tidetunes.service.playback.presentation.generated.resources.icon_play
import tidetunes.service.playback.presentation.generated.resources.icon_play_next
import tidetunes.service.playback.presentation.generated.resources.icon_play_previous
import tidetunes.service.playback.presentation.generated.resources.icon_timelapse
import tidetunes.service.playback.presentation.generated.resources.icon_vertialcal_more
import tidetunes.service.playback.presentation.generated.resources.music_lyric_add
import tidetunes.service.playback.presentation.generated.resources.music_lyric_fail
import tidetunes.service.playback.presentation.generated.resources.music_lyric_no_desc
import tidetunes.service.playback.presentation.generated.resources.music_lyric_remove
import tidetunes.service.playback.presentation.generated.resources.music_lyric_try_add_desc
import tidetunes.service.playback.presentation.generated.resources.music_player_context_menu_remove
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.TideTunesContextMenu
import com.github.tidetunes.core.presentation.components.TideTunesContextMenuItem
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonColors
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import com.github.tidetunes.core.presentation.components.customAnchoredDraggable
import com.github.tidetunes.core.presentation.components.dropShadow
import com.github.tidetunes.core.presentation.components.rememberCustomAnchoredDraggableState
import com.github.tidetunes.core.utils.nextTickOnMain
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.LyricLine
import com.github.tidetunes.core.presentation.media.AnimatedLyricLine
import com.github.tidetunes.core.domain.model.LyricsLoadState
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.media.ArtworkPalette
import com.github.tidetunes.service.playback.domain.RepeatMode
import com.github.tidetunes.core.utils.formatDuration
import com.github.tidetunes.core.utils.toMusicDurationMs
import kotlin.collections.emptyList
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration

@Composable
private fun MusicPlayerHeader(
    hasLyric: Boolean,
    nowPlayingState: NowPlayingState,
    onAction: (NowPlayingAction) -> Unit,
) {
    var moreMenuExpanded by remember {
        mutableStateOf(false)
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(13.dp, 13.dp)
            .fillMaxWidth()
    ) {
        TideTunesIconButton(
            sizeType = TideTunesIconButtonSize.Medium,
            buttonType = TideTunesIconButtonType.Default,
            painter = painterResource(Res.drawable.icon_back),
            onClick = {
                onAction(NowPlayingAction.NavigateBack)
            }
        )
        Box {
            TideTunesIconButton(
                sizeType = TideTunesIconButtonSize.Medium,
                buttonType = TideTunesIconButtonType.Default,
                painter = painterResource(Res.drawable.icon_vertialcal_more),
                onClick = { moreMenuExpanded = true; }
            )
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier
                    .offset(20.dp, (20).dp)
            ) {
                TideTunesContextMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false; },
                    items = listOf(
                        if (hasLyric) {
                            TideTunesContextMenuItem(
                                label = Res.string.music_lyric_remove,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(NowPlayingAction.RemoveLyric)
                                }
                            )
                        } else {
                            TideTunesContextMenuItem(
                                label = Res.string.music_lyric_add,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(NowPlayingAction.AddLyric)
                                }
                            )
                        },
                        if (nowPlayingState.currentTrack?.canDownload == true) {
                            TideTunesContextMenuItem(
                                label = Res.string.downloads_title,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(NowPlayingAction.DownloadCurrentTrack)
                                }
                            )
                        } else null,
                        TideTunesContextMenuItem(
                            label = Res.string.music_player_context_menu_remove,
                            isError = true,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.RemoveCurrentTrack)
                            }
                        ),
                    ).filterNotNull()
                )
            }
        }
    }
}

@Composable
private fun MusicSlider(
    currentDuration: String,
    _currentDurationMS: ULong,
    bufferDurationMS: ULong,
    totalDuration: String,
    totalDurationMS: ULong,
    onChangeMusicPosition: (ms: ULong) -> Unit,
) {
    val handleSize = 12.dp
    val sliderHeight = 4.dp
    val sliderContainerHeight = 16.dp

    var isDragging by remember { mutableStateOf(false) }
    var draggingCurrentDurationMS by remember { mutableStateOf(_currentDurationMS) }
    val currentDurationMS = if (isDragging) {
        draggingCurrentDurationMS
    } else {
        _currentDurationMS
    }

    val durationRate = if (totalDurationMS == 0UL) {
        0f
    } else {
        (currentDurationMS.toDouble() / totalDurationMS.toDouble()).toFloat()
    };
    val bufferRate = if (totalDurationMS == 0UL) {
        0f
    } else {
        (bufferDurationMS.toDouble() / totalDurationMS.toDouble()).toFloat()
    };
    var sliderWidth by remember { mutableIntStateOf(0) }
    val sliderWidthDp = with(LocalDensity.current) {
        sliderWidth.toDp()
    }

    val draggableState = rememberDraggableState { deltaPx ->
        val delta = (deltaPx.toDouble() / sliderWidth.toDouble() * totalDurationMS.toDouble()).toLong()
        var nextMS = draggingCurrentDurationMS.toLong() + delta
        nextMS = nextMS.coerceIn(0L, totalDurationMS.toLong())

        draggingCurrentDurationMS = nextMS.toULong()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderContainerHeight)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = currentDurationMS.toFloat(),
                        range = 0f..totalDurationMS.toFloat(),
                    )
                    setProgress { value ->
                        onChangeMusicPosition(value.toLong().coerceAtLeast(0L).toULong())
                        true
                    }
                }
                .onSizeChanged { size ->
                    if (sliderWidth != size.width) {
                        sliderWidth = size.width;
                    }
                }
                .pointerInput(totalDurationMS, sliderWidth) {
                    detectTapGestures { offset ->
                        var nextMS =
                            (offset.x.toDouble() / sliderWidth.toDouble() * totalDurationMS.toDouble()).toLong()
                        nextMS = nextMS.coerceIn(0L, totalDurationMS.toLong())
                        onChangeMusicPosition(nextMS.toULong())
                    }
                }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        isDragging = true
                        draggingCurrentDurationMS = _currentDurationMS
                    },
                    onDragStopped = {
                        isDragging = false
                        onChangeMusicPosition(draggingCurrentDurationMS)
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sliderHeight)
                    .offset(0.dp, (sliderContainerHeight - sliderHeight) / 2)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferRate)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(durationRate)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Box(
                modifier = Modifier
                    .offset(
                        -handleSize / 2 + (sliderWidthDp * durationRate),
                        (sliderContainerHeight - handleSize) / 2
                    )
                    .size(handleSize)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = currentDuration,
                fontSize = 10.sp
            )
            Text(
                text = totalDuration,
                fontSize = 10.sp
            )
        }
    }
}


@Composable
private fun CoverImage(artwork: Artwork?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        ArtworkImage(
            modifier = Modifier
                .dropShadow(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    offsetX = 0.dp,
                    offsetY = 0.dp,
                    blurRadius = 16.dp
                )
                .clip(RoundedCornerShape(20.dp))
                .size(300.dp),
            artwork = artwork,
        )
    }
}

@Composable
private fun LyricsPanel(
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    linePositionMs: Duration,
    lyricLoadedState: LyricsLoadState,
    onClickAdd: () -> Unit,
    widgetHeight: Int,
) {
    val density = LocalDensity.current
    val widgetHeightDp = with(density) {
        widgetHeight.toDp()
    }
    val listState = rememberLazyListState()

    LaunchedEffect(lyricIndex, widgetHeight, lyricLoadedState) {
        if (lyricLoadedState == LyricsLoadState.Loaded) {
            listState.animateScrollToItem(lyricIndex + 1, -(widgetHeight / 2))
        }
    }

    if (widgetHeight == 0) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lyricLoadedState == LyricsLoadState.Missing || lyricLoadedState == LyricsLoadState.Failed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(64.dp),
                    painter = painterResource(Res.drawable.icon_lyrics),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceVariant
                )
                Box(modifier = Modifier.height(4.dp))
                if (lyricLoadedState == LyricsLoadState.Missing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.music_lyric_no_desc),
                            fontSize = 14.sp,
                        )
                        TideTunesTextButton(
                            text = stringResource(Res.string.music_lyric_try_add_desc),
                            type = TideTunesTextButtonType.Primary,
                            size = TideTunesTextButtonSize.Medium,
                            onClick = {
                                onClickAdd()
                            }
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.music_lyric_fail),
                        fontSize = 14.sp,
                    )
                }
            }
            return
        }

        if (lyricLoadedState == LyricsLoadState.Loading) {
            return
        }

        Column {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth(),
                userScrollEnabled = false,
            ) {
                item {
                    Box(modifier = Modifier.height(widgetHeightDp / 2))
                }
                itemsIndexed(lyrics, key = { _, _ -> Unit.hashCode() }) { index, lyric ->
                    val isCurrent = index == lyricIndex
                    if (isCurrent) {
                        AnimatedLyricLine(
                            line = lyric,
                            isCurrentLine = true,
                            linePositionMs = linePositionMs,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = lyric.text,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicPlayerBody(
    onPrev: () -> Unit,
    onNext: () -> Unit,
    artwork: Artwork?,
    previousArtwork: Artwork?,
    nextArtwork: Artwork?,
    canPrev: Boolean,
    canNext: Boolean,
    lyricIndex: Int,
    linePositionMs: Duration,
    lyrics: List<LyricLine>,
    lyricLoadedState: LyricsLoadState,
    onClickAddLyric: () -> Unit,
) {
    val density = LocalDensity.current
    val anchoredDraggableState = rememberCustomAnchoredDraggableState(
        initialValue = 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearOutSlowInEasing
        ),
        anchors = mapOf(0f to "DEFAULT"),
    )
    val deltaDp = with(density) {
        anchoredDraggableState.value.toDp()
    }
    var widgetWidth by remember { mutableIntStateOf(0) }
    val widgetWidthDp = with(LocalDensity.current) {
        widgetWidth.toDp()
    }
    var widgetHeight by remember { mutableIntStateOf(0) }

    var dragStartX by remember { mutableFloatStateOf(0f) }
    var showLyric by remember { mutableStateOf(false) }

    fun updateAnchored() {
        val anchors = listOfNotNull(
            0f to "DEFAULT",
            if (canPrev) {
                widgetWidth.toFloat() to "PREV"
            } else null,
            if (canNext) {
                -widgetWidth.toFloat() to "NEXT"
            } else null,
        ).toMap()

        anchoredDraggableState.updateAnchors(
            anchors,
            { value ->
                if (value == widgetWidth.toFloat()) {
                    nextTickOnMain {
                        onPrev()
                        anchoredDraggableState.update(0f)
                        showLyric = false
                    }
                } else if (value == -widgetWidth.toFloat()) {
                    nextTickOnMain {
                        onNext()
                        anchoredDraggableState.update(0f)
                        showLyric = false
                    }
                }
            }
        )
    }

    LaunchedEffect(canPrev, canNext) {
        updateAnchored()
    }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    showLyric = !showLyric
                }
            }
            .onSizeChanged { size ->
                if (widgetWidth != size.width) {
                    widgetWidth = size.width;
                    updateAnchored()
                }
                if (widgetHeight != size.height) {
                    widgetHeight = size.height
                }
            }
            .customAnchoredDraggable(
                state = anchoredDraggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    dragStartX = anchoredDraggableState.value
                },
                onLimitDragEnded = { nextValue ->
                    val dis = (nextValue - dragStartX).absoluteValue.coerceIn(0f, widgetWidth.toFloat());
                    val sign = (nextValue - dragStartX).sign;
                    val next = dragStartX + dis * sign
                    next
                }
            )
            .fillMaxSize()
    ) {
        if (widgetWidth > 0) {
            if (canPrev) {
                Box(
                    modifier = Modifier
                        .offset(x = -widgetWidthDp + deltaDp),
                    contentAlignment = Alignment.Center,
                ) {
                    CoverImage(artwork = previousArtwork)
                }
            }
            if (canNext) {
                Box(
                    modifier = Modifier
                        .offset(x = widgetWidthDp + deltaDp),
                    contentAlignment = Alignment.Center,
                ) {
                    CoverImage(artwork = nextArtwork)
                }
            }
        }
        Box(
            modifier = Modifier
                .offset(x = deltaDp),
            contentAlignment = Alignment.Center,
        ) {
            if (!showLyric) {
                CoverImage(artwork = artwork)
            } else {
                LyricsPanel(
                    lyricIndex = lyricIndex,
                    linePositionMs = linePositionMs,
                    lyrics = lyrics,
                    lyricLoadedState = lyricLoadedState,
                    onClickAdd = onClickAddLyric,
                    widgetHeight = widgetHeight,
                )
            }
        }
    }
}

@Composable
private fun MusicPanel(
    nowPlayingState: NowPlayingState,
    isSleepTimerEnabled: Boolean,
    onAction: (NowPlayingAction) -> Unit,
) {
    val controls = nowPlayingState.controls
    val queue = nowPlayingState.queue

    val modeDrawable: DrawableResource = when (controls.repeatMode) {
        RepeatMode.Off -> if (queue.itemCount > 1) {
            Res.drawable.icon_mode_list
        } else {
            Res.drawable.icon_mode_one
        }
        RepeatMode.One -> Res.drawable.icon_mode_repeatone
        RepeatMode.All -> Res.drawable.icon_mode_repeat
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TideTunesIconButton(
            sizeType = TideTunesIconButtonSize.Medium,
            buttonType = if (isSleepTimerEnabled) {
                TideTunesIconButtonType.Primary
            } else {
                TideTunesIconButtonType.Default
            },
            overrideColors = TideTunesIconButtonColors(
                iconTint = if (isSleepTimerEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                buttonBg = Color.Transparent,
            ),
            painter = painterResource(Res.drawable.icon_timelapse),
            onClick = {
                onAction(NowPlayingAction.OpenSleepTimer)
            }
        )
        TideTunesIconButton(
            sizeType = TideTunesIconButtonSize.Medium,
            buttonType = TideTunesIconButtonType.Default,
            painter = painterResource(Res.drawable.icon_play_previous),
            disabled = !queue.canPlayPrevious,
            onClick = {
                onAction(NowPlayingAction.PlayPrevious)
            }
        )
        if (!controls.isPlaying) {
            TideTunesIconButton(
                sizeType = TideTunesIconButtonSize.Large,
                buttonType = TideTunesIconButtonType.Primary,
                painter = painterResource(Res.drawable.icon_play),
                disabled = controls.isLoading,
                overrideColors = if (controls.isLoading) {
                    TideTunesIconButtonColors(
                        buttonDisabledBg = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    null
                },
                onClick = {
                    onAction(NowPlayingAction.Resume)
                }
            )
        }
        if (controls.isPlaying) {
            TideTunesIconButton(
                sizeType = TideTunesIconButtonSize.Large,
                buttonType = TideTunesIconButtonType.Primary,
                painter = painterResource(Res.drawable.icon_pause),
                onClick = {
                    onAction(NowPlayingAction.Pause)
                }
            )
        }
        TideTunesIconButton(
            sizeType = TideTunesIconButtonSize.Medium,
            buttonType = TideTunesIconButtonType.Default,
            painter = painterResource(Res.drawable.icon_play_next),
            disabled = !queue.canPlayNext,
            onClick = {
                onAction(NowPlayingAction.PlayNext)
            }
        )
        TideTunesIconButton(
            sizeType = TideTunesIconButtonSize.Medium,
            buttonType = TideTunesIconButtonType.Default,
            painter = painterResource(modeDrawable),
            onClick = {
                onAction(NowPlayingAction.CycleRepeatMode)
            }
        )
    }
}

@Composable
fun NowPlayingScreen(
    state: NowPlayingState,
    palette: ArtworkPalette = ArtworkPalette.Default,
    lyricIndex: Int,
    linePositionMs: Duration = Duration.ZERO,
    isSleepTimerEnabled: Boolean,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTrack = state.currentTrack
    val lyricLoadedState = currentTrack?.lyrics?.loadState ?: LyricsLoadState.Loading
    val lyrics = currentTrack?.lyrics?.lines ?: emptyList()

    val hasLyric = currentTrack?.hasLyric == true

    Box(
        modifier = modifier
            .clipToBounds()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.darkMuted,
                        palette.muted,
                        palette.vibrant,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .fillMaxSize()
    ) {
        Column {
            MusicPlayerHeader(
                hasLyric = hasLyric,
                nowPlayingState = state,
                onAction = onAction,
            )
            Column(
                modifier = Modifier
                    .weight(1.0F)
            ) {
                MusicPlayerBody(
                    onPrev = {
                        onAction(NowPlayingAction.PlayPrevious)
                    },
                    onNext = {
                        onAction(NowPlayingAction.PlayNext)
                    },
                    artwork = currentTrack?.artwork,
                    linePositionMs = linePositionMs,
                    previousArtwork = state.queue.previousArtwork,
                    nextArtwork = state.queue.nextArtwork,
                    canPrev = state.queue.canPlayPrevious,
                    canNext = state.queue.canPlayNext,
                    lyricIndex = lyricIndex,
                    lyricLoadedState = lyricLoadedState,
                    lyrics = lyrics,
                    onClickAddLyric = {
                        onAction(NowPlayingAction.AddLyric)
                    }
                )
            }
            Column(
                modifier = Modifier.padding(36.dp, 10.dp)
            ) {
                Text(
                    text = currentTrack?.title ?: "",
                    maxLines = 3,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(0.dp, 10.dp)
                )
                progressContent(currentTrack?.durationMs)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(0.dp, 48.dp)
            ) {
                MusicPanel(
                    nowPlayingState = state,
                    isSleepTimerEnabled = isSleepTimerEnabled,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
internal fun NowPlayingProgressPanel(
    progressState: NowPlayingProgressState,
    trackDurationMs: Long?,
    onAction: (NowPlayingAction) -> Unit,
) {
    val totalDurationMs = trackDurationMs ?: progressState.playerDuration.inWholeMilliseconds

    MusicSlider(
        currentDuration = formatDuration(progressState.currentDuration),
        _currentDurationMS = toMusicDurationMs(progressState.currentDuration),
        bufferDurationMS = progressState.bufferDuration.inWholeMilliseconds.toULong(),
        totalDuration = formatDuration(totalDurationMs.milliseconds),
        totalDurationMS = totalDurationMs.coerceAtLeast(0).toULong(),
        onChangeMusicPosition = { nextMS ->
            onAction(NowPlayingAction.SeekTo(nextMS))
        },
    )
}
