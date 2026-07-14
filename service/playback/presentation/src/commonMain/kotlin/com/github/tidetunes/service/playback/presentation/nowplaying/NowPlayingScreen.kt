package com.github.tidetunes.service.playback.presentation.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.LyricsLoadState
import com.github.tidetunes.core.lyrics.ui.TideLyricsView
import com.github.tidetunes.core.presentation.components.TideContextMenu
import com.github.tidetunes.core.presentation.components.TideContextMenuItem
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonColors
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.components.TidePlayerControlButton
import com.github.tidetunes.core.presentation.components.TidePlayerControlSize
import com.github.tidetunes.core.presentation.components.TidePlayerControlVariant
import com.github.tidetunes.core.presentation.components.TideSlider
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.components.dropShadow
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.media.ArtworkPalette
import com.github.tidetunes.core.presentation.theme.TideTunesFontFamilies
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.core.utils.formatDuration
import com.github.tidetunes.core.utils.toMusicDurationMs
import com.github.tidetunes.service.playback.domain.RepeatMode
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
import tidetunes.service.playback.presentation.generated.resources.music_player_search_metadata
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DesktopPlayerBreakpoint = 860.dp

@Composable
private fun MusicPlayerHeader(
    hasLyric: Boolean,
    nowPlayingState: NowPlayingState,
    onAction: (NowPlayingAction) -> Unit,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth(),
    ) {
        TideIconButton(
            size = TideIconButtonSize.Medium,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_back),
            onClick = { onAction(NowPlayingAction.NavigateBack) },
        )
        Text(
            text = "NOW PLAYING",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Bold,
        )
        Box {
            TideIconButton(
                size = TideIconButtonSize.Medium,
                variant = TideIconButtonVariant.Default,
                painter = painterResource(Res.drawable.icon_vertialcal_more),
                onClick = { moreMenuExpanded = true },
            )
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier.offset(20.dp, 20.dp),
            ) {
                TideContextMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                    items = listOfNotNull(
                        TideContextMenuItem(
                            label = Res.string.music_player_search_metadata,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.SearchMetadata)
                            },
                        ),
                        if (hasLyric) {
                            TideContextMenuItem(
                                label = Res.string.music_lyric_remove,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(NowPlayingAction.RemoveLyric)
                                },
                            )
                        } else {
                            TideContextMenuItem(
                                label = Res.string.music_lyric_add,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(NowPlayingAction.AddLyric)
                                },
                            )
                        },
                        if (nowPlayingState.currentTrack?.canDownload == true) {
                            TideContextMenuItem(
                                label = Res.string.downloads_title,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(NowPlayingAction.DownloadCurrentTrack)
                                },
                            )
                        } else {
                            null
                        },
                        TideContextMenuItem(
                            label = Res.string.music_player_context_menu_remove,
                            isError = true,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.RemoveCurrentTrack)
                            },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MusicSlider(
    currentDuration: String,
    currentDurationMs: ULong,
    bufferDurationMs: ULong,
    totalDuration: String,
    totalDurationMs: ULong,
    onChangeMusicPosition: (ms: ULong) -> Unit,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbingDurationMs by remember { mutableStateOf(currentDurationMs) }
    val displayedDurationMs = if (isScrubbing) scrubbingDurationMs else currentDurationMs
    val sliderRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        TideSlider(
            value = displayedDurationMs.toFloat(),
            onValueChange = { value ->
                scrubbingDurationMs = value.toLong()
                    .coerceIn(0L, totalDurationMs.toLong())
                    .toULong()
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = sliderRange,
            bufferedValue = bufferDurationMs.toFloat(),
            onValueChangeStarted = {
                isScrubbing = true
                scrubbingDurationMs = currentDurationMs.coerceAtMost(totalDurationMs)
            },
            onValueChangeFinished = {
                val nextDurationMs = scrubbingDurationMs.coerceAtMost(totalDurationMs)
                isScrubbing = false
                onChangeMusicPosition(nextDurationMs)
            },
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = currentDuration,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote2,
            )
            Text(
                text = totalDuration,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote2,
            )
        }
    }
}

@Composable
private fun CoverImage(
    artwork: Artwork?,
    modifier: Modifier = Modifier,
    maxArtworkSize: androidx.compose.ui.unit.Dp = 400.dp,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        val artworkSize = minOf(maxWidth, maxHeight, maxArtworkSize)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .dropShadow(
                    color = Color.Black.copy(alpha = 0.32f),
                    offsetX = 0.dp,
                    offsetY = 18.dp,
                    blurRadius = 38.dp,
                )
                .clip(RoundedCornerShape(TideTunesTokens.shapes.xl))
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.24f)),
        ) {
            ArtworkImage(
                modifier = Modifier.fillMaxSize(),
                artwork = artwork,
            )
        }
    }
}

@Composable
private fun TrackInformation(
    track: NowPlayingTrackItem?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = track?.title.orEmpty(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MiuixTheme.colorScheme.onSurface,
            style = nowPlayingTitleStyle(),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = track?.artist?.takeIf { it.isNotBlank() } ?: "Unknown artist",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
        )
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TideIconButton(
            size = TideIconButtonSize.Medium,
            variant = if (isSleepTimerEnabled) TideIconButtonVariant.Primary else TideIconButtonVariant.Default,
            colors = TideIconButtonColors(
                iconTint = if (isSleepTimerEnabled) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
                buttonBg = Color.Transparent,
            ),
            painter = painterResource(Res.drawable.icon_timelapse),
            onClick = { onAction(NowPlayingAction.OpenSleepTimer) },
        )
        TideIconButton(
            size = TideIconButtonSize.Medium,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_play_previous),
            enabled = queue.canPlayPrevious,
            onClick = { onAction(NowPlayingAction.PlayPrevious) },
        )
        TidePlayerControlButton(
            painter = painterResource(
                if (controls.isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play,
            ),
            enabled = controls.isPlaying || !controls.isLoading,
            size = TidePlayerControlSize.Large,
            variant = TidePlayerControlVariant.Primary,
            onClick = {
                onAction(if (controls.isPlaying) NowPlayingAction.Pause else NowPlayingAction.Resume)
            },
        )
        TideIconButton(
            size = TideIconButtonSize.Medium,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_play_next),
            enabled = queue.canPlayNext,
            onClick = { onAction(NowPlayingAction.PlayNext) },
        )
        TideIconButton(
            size = TideIconButtonSize.Medium,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(modeDrawable),
            onClick = { onAction(NowPlayingAction.CycleRepeatMode) },
        )
    }
}

@Composable
private fun DesktopNowPlayingLayout(
    state: NowPlayingState,
    currentPositionMs: Long,
    isSleepTimerEnabled: Boolean,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 34.dp, end = 28.dp, bottom = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(0.46f)
                .fillMaxHeight()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoverImage(
                artwork = track?.artwork,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            )
            TrackInformation(
                track = track,
                modifier = Modifier.padding(top = 10.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            progressContent(track?.durationMs)
            Spacer(modifier = Modifier.height(16.dp))
            MusicPanel(
                nowPlayingState = state,
                isSleepTimerEnabled = isSleepTimerEnabled,
                onAction = onAction,
            )
        }

        LyricsSurface(
            track = track,
            currentPositionMs = currentPositionMs,
            onAction = onAction,
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun LyricsSurface(
    track: NowPlayingTrackItem?,
    currentPositionMs: Long,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadState = track?.lyrics?.loadState ?: LyricsLoadState.Loading
    val lyricLines = track?.lyrics?.lines.orEmpty()
    val syncedLyrics = remember(lyricLines, track?.title, track?.durationMs) {
        lyricLines.toSyncedLyrics(
            trackTitle = track?.title.orEmpty(),
            trackDurationMs = track?.durationMs,
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(TideTunesTokens.shapes.xl)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loadState == LyricsLoadState.Loading -> {
                Text(
                    text = "Loading lyrics",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body1,
                )
            }
            loadState == LyricsLoadState.Missing || loadState == LyricsLoadState.Failed -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        modifier = Modifier.size(58.dp),
                        painter = painterResource(Res.drawable.icon_lyrics),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.52f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            if (loadState == LyricsLoadState.Missing) {
                                Res.string.music_lyric_no_desc
                            } else {
                                Res.string.music_lyric_fail
                            },
                        ),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body1,
                    )
                    if (loadState == LyricsLoadState.Missing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TideTextButton(
                            text = stringResource(Res.string.music_lyric_try_add_desc),
                            variant = TideTextButtonVariant.Primary,
                            size = TideTextButtonSize.Medium,
                            onClick = { onAction(NowPlayingAction.AddLyric) },
                        )
                    }
                }
            }
            syncedLyrics.lines.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.music_lyric_no_desc),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body1,
                )
            }
            else -> {
                TideLyricsView(
                    lyrics = syncedLyrics,
                    currentPositionMs = currentPositionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                    onLineClick = { line ->
                        onAction(NowPlayingAction.SeekTo(line.start.coerceAtLeast(0).toULong()))
                    },
                    activeColor = MiuixTheme.colorScheme.onSurface,
                    inactiveColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.52f),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun CompactNowPlayingLayout(
    state: NowPlayingState,
    isSleepTimerEnabled: Boolean,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverImage(
            artwork = track?.artwork,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            maxArtworkSize = 300.dp,
        )
        TrackInformation(
            track = track,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(top = 16.dp),
        )
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            progressContent(track?.durationMs)
        }
        MusicPanel(
            nowPlayingState = state,
            isSleepTimerEnabled = isSleepTimerEnabled,
            onAction = onAction,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NowPlayingScreen(
    state: NowPlayingState,
    palette: ArtworkPalette = ArtworkPalette.Default,
    currentPositionMs: Long,
    isSleepTimerEnabled: Boolean,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTrack = state.currentTrack

    Box(
        modifier = modifier
            .clipToBounds()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.darkMuted,
                        palette.muted,
                        palette.vibrant.copy(alpha = 0.86f),
                        MiuixTheme.colorScheme.surface,
                    ),
                ),
            )
            .background(MiuixTheme.colorScheme.background.copy(alpha = 0.74f))
            .fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MusicPlayerHeader(
                hasLyric = currentTrack?.hasLyric == true,
                nowPlayingState = state,
                onAction = onAction,
            )
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                if (maxWidth >= DesktopPlayerBreakpoint && maxHeight >= 520.dp) {
                    DesktopNowPlayingLayout(
                        state = state,
                        currentPositionMs = currentPositionMs,
                        isSleepTimerEnabled = isSleepTimerEnabled,
                        progressContent = progressContent,
                        onAction = onAction,
                    )
                } else {
                    CompactNowPlayingLayout(
                        state = state,
                        isSleepTimerEnabled = isSleepTimerEnabled,
                        progressContent = progressContent,
                        onAction = onAction,
                    )
                }
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
        currentDurationMs = toMusicDurationMs(progressState.currentDuration),
        bufferDurationMs = progressState.bufferDuration.inWholeMilliseconds.coerceAtLeast(0).toULong(),
        totalDuration = formatDuration(totalDurationMs.milliseconds),
        totalDurationMs = totalDurationMs.coerceAtLeast(0).toULong(),
        onChangeMusicPosition = { nextMs -> onAction(NowPlayingAction.SeekTo(nextMs)) },
    )
}

@Composable
private fun nowPlayingTitleStyle(): TextStyle = TextStyle(
    fontFamily = TideTunesFontFamilies.Sans,
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 30.sp,
    letterSpacing = 0.sp,
)
