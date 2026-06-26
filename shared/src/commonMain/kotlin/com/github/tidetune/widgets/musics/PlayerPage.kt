package com.github.tidetune.widgets.musics

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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import tidetune.shared.generated.resources.Res
import tidetune.shared.generated.resources.icon_back
import tidetune.shared.generated.resources.icon_lyrics
import tidetune.shared.generated.resources.icon_mode_list
import tidetune.shared.generated.resources.icon_mode_one
import tidetune.shared.generated.resources.icon_mode_repeat
import tidetune.shared.generated.resources.icon_mode_repeatone
import tidetune.shared.generated.resources.icon_pause
import tidetune.shared.generated.resources.icon_play
import tidetune.shared.generated.resources.icon_play_next
import tidetune.shared.generated.resources.icon_play_previous
import tidetune.shared.generated.resources.icon_timelapse
import tidetune.shared.generated.resources.icon_vertialcal_more
import tidetune.shared.generated.resources.music_lyric_add
import tidetune.shared.generated.resources.music_lyric_fail
import tidetune.shared.generated.resources.music_lyric_no_desc
import tidetune.shared.generated.resources.music_lyric_remove
import tidetune.shared.generated.resources.music_lyric_try_add_desc
import tidetune.shared.generated.resources.music_player_context_menu_remove
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.github.tidetune.components.TideTuneContextMenu
import com.github.tidetune.components.TideTuneContextMenuItem
import com.github.tidetune.components.TideTuneIconButton
import com.github.tidetune.components.TideTuneIconButtonColors
import com.github.tidetune.components.TideTuneIconButtonSize
import com.github.tidetune.components.TideTuneIconButtonType
import com.github.tidetune.components.TideTuneTextButton
import com.github.tidetune.components.TideTuneTextButtonSize
import com.github.tidetune.components.TideTuneTextButtonType
import com.github.tidetune.components.MusicCover
import com.github.tidetune.components.customAnchoredDraggable
import com.github.tidetune.components.dropShadow
import com.github.tidetune.components.rememberCustomAnchoredDraggableState
import com.github.tidetune.utils.nextTickOnMain
import com.github.tidetune.viewmodels.PlayerVM
import com.github.tidetune.viewmodels.SleepModeVM
import com.github.tidetune.core.LocalNavController
import com.github.tidetune.core.RouteImport
import com.github.tidetune.singleton.RouteImportType
import com.github.tidetune.utils.formatDuration
import com.github.tidetune.utils.toMusicDurationMs
import uniffi.tidetune_core.DataSourceKey
import uniffi.tidetune_core.LyricLine
import uniffi.tidetune_core.LyricLoadState
import uniffi.tidetune_core.PlayMode
import kotlin.collections.emptyList
import kotlin.math.absoluteValue
import kotlin.math.sign

@Composable
private fun MusicPlayerHeader(
    hasLyric: Boolean,
    playerVM: PlayerVM = koinViewModel(),
) {
    val navController = LocalNavController.current
    val currentPlaying by playerVM.music.collectAsState()


    var moreMenuExpanded by remember {
        mutableStateOf(false)
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(13.dp, 13.dp)
            .fillMaxWidth()
    ) {
        TideTuneIconButton(
            sizeType = TideTuneIconButtonSize.Medium,
            buttonType = TideTuneIconButtonType.Default,
            painter = painterResource(Res.drawable.icon_back),
            onClick = {
                navController.popBackStack()
            }
        )
        Box {
            TideTuneIconButton(
                sizeType = TideTuneIconButtonSize.Medium,
                buttonType = TideTuneIconButtonType.Default,
                painter = painterResource(Res.drawable.icon_vertialcal_more),
                onClick = { moreMenuExpanded = true; }
            )
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier
                    .offset(20.dp, (20).dp)
            ) {
                TideTuneContextMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false; },
                    items = listOf(
                        if (hasLyric) {
                            TideTuneContextMenuItem(
                                label = Res.string.music_lyric_remove,
                                onClick = {
                                    playerVM.removeLyric()
                                }
                            )
                        } else {
                            TideTuneContextMenuItem(
                                label = Res.string.music_lyric_add,
                                onClick = {
                                    if (currentPlaying?.meta?.id != null) {
                                        navController.navigate(
                                            RouteImport(RouteImportType.Lyric)
                                        )
                                    }
                                }
                            )
                        },
                        TideTuneContextMenuItem(
                            label = Res.string.music_player_context_menu_remove,
                            isError = true,
                            onClick = {
                                playerVM.remove()
                            }
                        ),
                    )
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
private fun CoverImage(dataSourceKey: DataSourceKey?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        MusicCover(
            modifier = Modifier
                .dropShadow(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    offsetX = 0.dp,
                    offsetY = 0.dp,
                    blurRadius = 16.dp
                )
                .clip(RoundedCornerShape(20.dp))
                .size(300.dp),
            coverDataSourceKey = dataSourceKey,
        )
    }
}

@Composable
private fun MusicLyric(
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    lyricLoadedState: LyricLoadState,
    onClickAdd: () -> Unit,
    widgetHeight: Int,
) {
    val density = LocalDensity.current
    val widgetHeightDp = with(density) {
        widgetHeight.toDp()
    }
    val listState = rememberLazyListState()

    LaunchedEffect(lyricIndex, widgetHeight, lyricLoadedState) {
        if (lyricLoadedState == LyricLoadState.LOADED) {
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
        if (lyricLoadedState == LyricLoadState.MISSING || lyricLoadedState == LyricLoadState.FAILED) {
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
                if (lyricLoadedState == LyricLoadState.MISSING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.music_lyric_no_desc),
                            fontSize = 14.sp,
                        )
                        TideTuneTextButton(
                            text = stringResource(Res.string.music_lyric_try_add_desc),
                            type = TideTuneTextButtonType.Primary,
                            size = TideTuneTextButtonSize.Medium,
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

        if (lyricLoadedState == LyricLoadState.LOADING) {
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
                itemsIndexed(lyrics) { index, lyric ->
                    val isCurrent = index == lyricIndex
                    val textColor =
                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = lyric.text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
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
    cover: DataSourceKey?,
    prevCover: DataSourceKey?,
    nextCover: DataSourceKey?,
    canPrev: Boolean,
    canNext: Boolean,
    lyricIndex: Int,
    lyrics: List<LyricLine>,
    lyricLoadedState: LyricLoadState,
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
                    CoverImage(dataSourceKey = prevCover)
                }
            }
            if (canNext) {
                Box(
                    modifier = Modifier
                        .offset(x = widgetWidthDp + deltaDp),
                    contentAlignment = Alignment.Center,
                ) {
                    CoverImage(dataSourceKey = nextCover)
                }
            }
        }
        Box(
            modifier = Modifier
                .offset(x = deltaDp),
            contentAlignment = Alignment.Center,
        ) {
            if (!showLyric) {
                CoverImage(dataSourceKey = cover)
            } else {
                MusicLyric(
                    lyricIndex = lyricIndex,
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
    playerVM: PlayerVM = koinViewModel(),
    sleepModeVM: SleepModeVM = koinViewModel()
) {
    val playMode by playerVM.playMode.collectAsState()
    val timeToPauseState by sleepModeVM.state.collectAsState()
    val previousMusic by playerVM.previousMusic.collectAsState()
    val nextMusic by playerVM.nextMusic.collectAsState()
    val playing by playerVM.playing.collectAsState()
    val loading by playerVM.loading.collectAsState()

    val isTimeToPauseOpen = timeToPauseState.enabled
    val modeDrawable: DrawableResource = when (playMode) {
        PlayMode.SINGLE -> Res.drawable.icon_mode_one
        PlayMode.SINGLE_LOOP -> Res.drawable.icon_mode_repeatone
        PlayMode.LIST -> Res.drawable.icon_mode_list
        PlayMode.LIST_LOOP -> Res.drawable.icon_mode_repeat
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TideTuneIconButton(
            sizeType = TideTuneIconButtonSize.Medium,
            buttonType = if (isTimeToPauseOpen) {
                TideTuneIconButtonType.Primary
            } else {
                TideTuneIconButtonType.Default
            },
            overrideColors = TideTuneIconButtonColors(
                iconTint = if (isTimeToPauseOpen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                buttonBg = Color.Transparent,
            ),
            painter = painterResource(Res.drawable.icon_timelapse),
            onClick = {
                sleepModeVM.openModal()
            }
        )
        TideTuneIconButton(
            sizeType = TideTuneIconButtonSize.Medium,
            buttonType = TideTuneIconButtonType.Default,
            painter = painterResource(Res.drawable.icon_play_previous),
            disabled = previousMusic == null,
            onClick = {
                playerVM.playPrevious()
            }
        )
        if (!playing) {
            TideTuneIconButton(
                sizeType = TideTuneIconButtonSize.Large,
                buttonType = TideTuneIconButtonType.Primary,
                painter = painterResource(Res.drawable.icon_play),
                disabled = loading,
                overrideColors = if (loading) {
                    TideTuneIconButtonColors(
                        buttonDisabledBg = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    null
                },
                onClick = {
                    playerVM.resume()
                }
            )
        }
        if (playing) {
            TideTuneIconButton(
                sizeType = TideTuneIconButtonSize.Large,
                buttonType = TideTuneIconButtonType.Primary,
                painter = painterResource(Res.drawable.icon_pause),
                onClick = {
                    playerVM.pause()
                }
            )
        }
        TideTuneIconButton(
            sizeType = TideTuneIconButtonSize.Medium,
            buttonType = TideTuneIconButtonType.Default,
            painter = painterResource(Res.drawable.icon_play_next),
            disabled = nextMusic == null,
            onClick = {
                playerVM.playNext()
            }
        )
        TideTuneIconButton(
            sizeType = TideTuneIconButtonSize.Medium,
            buttonType = TideTuneIconButtonType.Default,
            painter = painterResource(modeDrawable),
            onClick = {
                playerVM.changePlayModeToNext()
            }
        )
    }
}

@Composable
fun MusicPlayerPage(
    playerVM: PlayerVM = koinViewModel(),
) {
    val navController = LocalNavController.current
    val currentMusic by playerVM.music.collectAsState()
    val currentDuration by playerVM.currentDuration.collectAsState()
    val previousMusic by playerVM.previousMusic.collectAsState()
    val nextMusic by playerVM.nextMusic.collectAsState()
    val bufferDuration by playerVM.bufferDuration.collectAsState()
    val playerDuration by playerVM.playerDuration.collectAsState()
    val currentLyricIndex by playerVM.lyricIndex.collectAsState()
    val lyricLoadedState = currentMusic?.lyric?.loadedState ?: LyricLoadState.LOADING
    val lyrics = currentMusic?.lyric?.data?.lines ?: emptyList()

    val hasLyric = lyricLoadedState != LyricLoadState.MISSING

    Box(
        modifier = Modifier
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
    ) {
        Column {
            MusicPlayerHeader(
                hasLyric = hasLyric,
            )
            Column(
                modifier = Modifier
                    .weight(1.0F)
            ) {
                MusicPlayerBody(
                    onPrev = {
                        playerVM.playPrevious()
                    },
                    onNext = {
                        playerVM.playNext()
                    },
                    cover = currentMusic?.cover,
                    prevCover = previousMusic?.cover,
                    nextCover = nextMusic?.cover,
                    canPrev = previousMusic != null,
                    canNext = nextMusic != null,
                    lyricIndex = currentLyricIndex,
                    lyricLoadedState = lyricLoadedState,
                    lyrics = lyrics,
                    onClickAddLyric = {
                        if (currentMusic != null) {
                            navController.navigate(RouteImport(RouteImportType.Lyric))
                        }
                    }
                )
            }
            Column(
                modifier = Modifier.padding(36.dp, 10.dp)
            ) {
                Text(
                    text = currentMusic?.meta?.title ?: "",
                    maxLines = 3,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(0.dp, 10.dp)
                )
                MusicSlider(
                    currentDuration = formatDuration(currentDuration),
                    _currentDurationMS = toMusicDurationMs(currentDuration),
                    bufferDurationMS = bufferDuration.inWholeMilliseconds.toULong(),
                    totalDuration = formatDuration(
                        currentMusic?.meta?.duration ?: playerDuration
                    ),
                    totalDurationMS = toMusicDurationMs(
                        currentMusic?.meta?.duration ?: playerDuration
                    ),
                    onChangeMusicPosition = { nextMS ->
                        playerVM.seek(nextMS)
                    }
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(0.dp, 48.dp)
            ) {
                MusicPanel()
            }
        }
    }
}
