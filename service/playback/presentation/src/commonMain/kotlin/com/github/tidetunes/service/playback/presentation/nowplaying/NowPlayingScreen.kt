package com.github.tidetunes.service.playback.presentation.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.LyricDisplaySettings
import com.github.tidetunes.core.domain.model.LyricFontChoice
import com.github.tidetunes.core.domain.model.LyricLine
import com.github.tidetunes.core.domain.model.LyricTextAlignment
import com.github.tidetunes.core.domain.model.LyricsLoadState
import com.github.tidetunes.core.domain.model.PlayerInteractionSettings
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
import com.github.tidetunes.core.utils.toMusicDurationMs
import com.github.tidetunes.service.playback.domain.RepeatMode
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.service.playback.presentation.generated.resources.Res
import tidetunes.service.playback.presentation.generated.resources.downloads_title
import tidetunes.service.playback.presentation.generated.resources.icon_back
import tidetunes.service.playback.presentation.generated.resources.icon_collapse
import tidetunes.service.playback.presentation.generated.resources.icon_heart
import tidetunes.service.playback.presentation.generated.resources.icon_lyrics
import tidetunes.service.playback.presentation.generated.resources.icon_mode_list
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
import tidetunes.service.playback.presentation.generated.resources.music_player_edit_lyric_timing
import tidetunes.service.playback.presentation.generated.resources.music_player_edit_metadata
import tidetunes.service.playback.presentation.generated.resources.music_player_search_metadata
import tidetunes.service.playback.presentation.generated.resources.now_playing_title
import tidetunes.service.playback.presentation.generated.resources.player_add_favorite
import tidetunes.service.playback.presentation.generated.resources.player_close
import tidetunes.service.playback.presentation.generated.resources.player_loading_lyrics
import tidetunes.service.playback.presentation.generated.resources.player_lyrics_unavailable
import tidetunes.service.playback.presentation.generated.resources.player_more_options
import tidetunes.service.playback.presentation.generated.resources.player_pause
import tidetunes.service.playback.presentation.generated.resources.player_play
import tidetunes.service.playback.presentation.generated.resources.player_remove_favorite
import tidetunes.service.playback.presentation.generated.resources.player_unknown_artist
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DesktopPlayerBreakpoint = 860.dp

// ── Player Header ──

@Composable
private fun MusicPlayerHeader(
    hasLyric: Boolean,
    nowPlayingState: NowPlayingState,
    onAction: (NowPlayingAction) -> Unit,
) {
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
            text = stringResource(Res.string.now_playing_title),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Bold,
        )
        NowPlayingMoreButton(
            hasLyric = hasLyric,
            nowPlayingState = nowPlayingState,
            onAction = onAction,
        )
    }
}

@Composable
private fun NowPlayingMoreButton(
    hasLyric: Boolean,
    nowPlayingState: NowPlayingState,
    onAction: (NowPlayingAction) -> Unit,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Box {
        TideIconButton(
            size = TideIconButtonSize.Medium,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_vertialcal_more),
            contentDescription = stringResource(Res.string.player_more_options),
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
                    if (nowPlayingState.externalEditorSupported) {
                        TideContextMenuItem(
                            label = Res.string.music_player_edit_metadata,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.OpenMetadataEditor)
                            },
                        )
                    } else null,
                    if (nowPlayingState.externalEditorSupported) {
                        TideContextMenuItem(
                            label = Res.string.music_player_edit_lyric_timing,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.OpenLyricTimingEditor)
                            },
                        )
                    } else null,
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
                    } else null,
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

// ── Progress Slider ──

@Composable
private fun MusicSlider(
    currentDuration: String,
    currentDurationMs: ULong,
    bufferDurationMs: ULong,
    totalDuration: String,
    totalDurationMs: ULong,
    tapToSeekEnabled: Boolean,
    showTotalDuration: Boolean,
    onChangeMusicPosition: (ms: ULong) -> Unit,
    lightTheme: Boolean = false,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbingDurationMs by remember { mutableStateOf(currentDurationMs) }
    val displayedDurationMs = if (isScrubbing) scrubbingDurationMs else currentDurationMs
    val sliderRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f)
    val labelColor = if (lightTheme) {
        Color.White.copy(alpha = 0.40f)
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

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
            tapToSeekEnabled = tapToSeekEnabled,
            trackColorOverride = Color.White.copy(alpha = 0.28f),
            bufferColorOverride = Color.White.copy(alpha = 0.44f),
            activeTrackColorOverride = Color.White,
            thumbColorOverride = Color.White,
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
                color = labelColor,
                style = MiuixTheme.textStyles.footnote2.copy(fontFamily = TideTunesFontFamilies.Mono),
            )
            Text(
                text = if (showTotalDuration) {
                    totalDuration
                } else {
                    val remainingMs = totalDurationMs.toLong() - displayedDurationMs.toLong()
                    "-${formatPlayerDuration(remainingMs.coerceAtLeast(0).milliseconds)}"
                },
                color = labelColor,
                style = MiuixTheme.textStyles.footnote2.copy(fontFamily = TideTunesFontFamilies.Mono),
            )
        }
    }
}

// ── Cover Image ──

@Composable
private fun CoverImage(
    artwork: Artwork?,
    modifier: Modifier = Modifier,
    maxArtworkSize: androidx.compose.ui.unit.Dp = 400.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = TideTunesTokens.shapes.xl,
    swipeEnabled: Boolean = false,
    onSwipePrevious: () -> Unit = {},
    onSwipeNext: () -> Unit = {},
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        val artworkSize = minOf(maxWidth, maxHeight, maxArtworkSize)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .pointerInput(swipeEnabled) {
                    if (swipeEnabled) {
                        var accumulatedDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onHorizontalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
                            onDragEnd = {
                                if (abs(accumulatedDrag) >= 72f) {
                                    if (accumulatedDrag > 0f) onSwipePrevious() else onSwipeNext()
                                }
                            },
                        )
                    }
                }
                .dropShadow(
                    color = Color.Black.copy(alpha = 0.32f),
                    offsetX = 0.dp,
                    offsetY = 18.dp,
                    blurRadius = 38.dp,
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.24f)),
        ) {
            ArtworkImage(
                modifier = Modifier.fillMaxSize(),
                artwork = artwork,
            )
        }
    }
}

// ── Track Information ──

@Composable
private fun TrackInformation(
    track: NowPlayingTrackItem?,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    modifier: Modifier = Modifier,
    lightTheme: Boolean = false,
) {
    val customFontWeight = FontWeight(lyricDisplaySettings.font.weight.coerceIn(100, 900))
    val titleFontFamily = lyricDisplaySettings.pageFontFamilyFor(track?.title.orEmpty())
    val artistText = track?.artist?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.player_unknown_artist)
    val artistFontFamily = lyricDisplaySettings.pageFontFamilyFor(artistText)
    val annotation = track?.annotation
        ?.takeIf { playerInteractionSettings.showSongAnnotation && it.isNotBlank() }
    val textColor = if (lightTheme) Color.White else MiuixTheme.colorScheme.onSurface
    val mutedColor = if (lightTheme) Color.White.copy(alpha = 0.55f) else MiuixTheme.colorScheme.onSurfaceVariantSummary

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = track?.title.orEmpty(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = textColor,
            style = TextStyle(
                fontFamily = titleFontFamily ?: TideTunesFontFamilies.Sans,
                fontSize = 24.sp,
                fontWeight = if (titleFontFamily == null) FontWeight.Bold else customFontWeight,
                lineHeight = 30.sp,
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = artistText,
            color = mutedColor,
            style = MiuixTheme.textStyles.body1.let { style ->
                if (artistFontFamily == null) style else style.copy(
                    fontFamily = artistFontFamily,
                    fontWeight = customFontWeight,
                )
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
        )
        annotation?.let { text ->
            Text(
                text = text,
                color = mutedColor,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}

// ── Five-Button Transport ──

@Composable
private fun TransportPanel(
    nowPlayingState: NowPlayingState,
    isSleepTimerEnabled: Boolean,
    onAction: (NowPlayingAction) -> Unit,
    lightTheme: Boolean = false,
    compact: Boolean = false,
) {
    val controls = nowPlayingState.controls
    val queue = nowPlayingState.queue
    val iconTint = if (lightTheme) Color.White.copy(alpha = 0.90f) else MiuixTheme.colorScheme.onSurface
    val dimTint = if (lightTheme) Color.White.copy(alpha = 0.45f) else MiuixTheme.colorScheme.onSurfaceVariantActions
    val accentTint = if (lightTheme) Color.White else MiuixTheme.colorScheme.primary
    val primaryButtonColor = if (compact) Color(0xFF302D33) else if (lightTheme) Color.White else MiuixTheme.colorScheme.onSurface
    val primaryIconTint = if (compact) Color.White else if (lightTheme) Color(0xFF06040E) else MiuixTheme.colorScheme.background
    val primaryButtonSize = if (compact) 72.dp else 62.dp
    val primaryIconSize = if (compact) 28.dp else 24.dp
    val repeatDrawable: DrawableResource = when (controls.repeatMode) {
        RepeatMode.Off -> Res.drawable.icon_mode_repeat
        RepeatMode.One -> Res.drawable.icon_mode_repeatone
        RepeatMode.All -> Res.drawable.icon_mode_repeat
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Compact follows the Design transport rail; desktop retains the timer shortcut.
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(
                if (compact) repeatDrawable else Res.drawable.icon_timelapse,
            ),
            colors = TideIconButtonColors(
                iconTint = if (
                    (compact && controls.repeatMode != RepeatMode.Off) ||
                    (!compact && isSleepTimerEnabled)
                ) {
                    accentTint
                } else {
                    dimTint
                },
                buttonBg = Color.Transparent,
            ),
            onClick = {
                onAction(
                    if (compact) NowPlayingAction.CycleRepeatMode else NowPlayingAction.OpenSleepTimer,
                )
            },
        )
        // Previous
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_play_previous),
            enabled = queue.canPlayPrevious,
            colors = TideIconButtonColors(iconTint = iconTint, buttonBg = Color.Transparent),
            onClick = { onAction(NowPlayingAction.PlayPrevious) },
        )
        // Play/Pause (larger)
        Box(
            modifier = Modifier
                .size(primaryButtonSize)
                .clip(CircleShape)
                .background(primaryButtonColor)
                .clickable(enabled = controls.isPlaying || !controls.isLoading) {
                    onAction(if (controls.isPlaying) NowPlayingAction.Pause else NowPlayingAction.Resume)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (controls.isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play,
                ),
                contentDescription = stringResource(
                    if (controls.isPlaying) Res.string.player_pause else Res.string.player_play
                ),
                tint = primaryIconTint,
                modifier = Modifier.size(primaryIconSize),
            )
        }
        // Next
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_play_next),
            enabled = queue.canPlayNext,
            colors = TideIconButtonColors(iconTint = iconTint, buttonBg = Color.Transparent),
            onClick = { onAction(NowPlayingAction.PlayNext) },
        )
        // Queue on compact; repeat remains the desktop trailing action.
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(
                if (compact) Res.drawable.icon_mode_list else repeatDrawable,
            ),
            colors = TideIconButtonColors(
                iconTint = if (!compact && controls.repeatMode != RepeatMode.Off) accentTint else dimTint,
                buttonBg = Color.Transparent,
            ),
            onClick = {
                onAction(
                    if (compact) NowPlayingAction.OpenQueue else NowPlayingAction.CycleRepeatMode,
                )
            },
        )
    }
}

// ── Desktop Layout ──

@Composable
private fun DesktopNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    currentPositionMs: Long,
    isSleepTimerEnabled: Boolean,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 34.dp, end = 28.dp, bottom = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        // Left: artwork + metadata + progress + transport
        Column(
            modifier = Modifier
                .weight(0.46f)
                .fillMaxHeight()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoverImage(
                artwork = track?.artwork,
                swipeEnabled = playerInteractionSettings.coverSwipeEnabled,
                onSwipePrevious = {
                    if (state.queue.canPlayPrevious) onAction(NowPlayingAction.PlayPrevious)
                },
                onSwipeNext = {
                    if (state.queue.canPlayNext) onAction(NowPlayingAction.PlayNext)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            )
            TrackInformation(
                track = track,
                lyricDisplaySettings = lyricDisplaySettings,
                playerInteractionSettings = playerInteractionSettings,
                modifier = Modifier.padding(top = 10.dp),
                lightTheme = true,
            )
            Spacer(modifier = Modifier.height(14.dp))
            progressContent(track?.durationMs)
            Spacer(modifier = Modifier.height(20.dp))
            TransportPanel(
                nowPlayingState = state,
                isSleepTimerEnabled = isSleepTimerEnabled,
                onAction = onAction,
                lightTheme = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Right: lyrics
        LyricsSurface(
            track = track,
            lyricDisplaySettings = lyricDisplaySettings,
            currentPositionMs = currentPositionMs,
            isPlaying = state.controls.isPlaying,
            onAction = onAction,
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight(),
        )
    }
}

// ── Lyrics Surface ──

@Composable
private fun LyricsSurface(
    track: NowPlayingTrackItem?,
    lyricDisplaySettings: LyricDisplaySettings,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadState = track?.lyrics?.loadState ?: LyricsLoadState.Loading
    val lyricLines = track?.lyrics?.lines.orEmpty()
    val syncedLyrics = remember(lyricLines, track?.title, track?.durationMs, lyricDisplaySettings) {
        lyricLines.toSyncedLyrics(
            trackTitle = track?.title.orEmpty(),
            trackDurationMs = track?.durationMs,
            settings = lyricDisplaySettings,
        )
    }
    val primaryScale = lyricDisplaySettings.primaryFontScalePercent / 100f
    val primaryFontSize = lyricDisplaySettings.primaryFontSizeSp * primaryScale
    val secondaryScale = lyricDisplaySettings.secondaryFontScalePercent / 100f
    val secondaryFontSize = lyricDisplaySettings.secondaryFontSizeSp * secondaryScale
    val lyricTextAlign = when (lyricDisplaySettings.textAlignment) {
        LyricTextAlignment.Left -> TextAlign.Start
        LyricTextAlignment.Center -> TextAlign.Center
        LyricTextAlignment.Right -> TextAlign.End
    }
    val lyricFontFamily = if (lyricDisplaySettings.font.applyToLyricsPage) {
        val containsCjk = lyricLines.any { line -> line.text.any(Char::isCjkCharacter) }
        val choice = if (containsCjk) lyricDisplaySettings.font.cjkFont else lyricDisplaySettings.font.westernFont
        choice.toFontFamily()
    } else {
        FontFamily.Default
    }
    val lyricFontWeight = FontWeight(lyricDisplaySettings.font.weight.coerceIn(100, 900))

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loadState == LyricsLoadState.Loading -> {
                Text(
                    text = stringResource(Res.string.player_loading_lyrics),
                    color = Color.White.copy(alpha = 0.55f),
                    style = MiuixTheme.textStyles.body1,
                )
            }
            loadState == LyricsLoadState.Missing || loadState == LyricsLoadState.Failed -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        modifier = Modifier.size(58.dp),
                        painter = painterResource(Res.drawable.icon_lyrics),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.30f),
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
                        color = Color.White.copy(alpha = 0.55f),
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
                    color = Color.White.copy(alpha = 0.55f),
                    style = MiuixTheme.textStyles.body1,
                )
            }
            else -> {
                TideLyricsView(
                    lyrics = syncedLyrics,
                    currentPositionMs = currentPositionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                    isPlaying = isPlaying,
                    onLineClick = { line ->
                        onAction(NowPlayingAction.SeekTo(line.start.coerceAtLeast(0).toULong()))
                    },
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.42f),
                    activeTextStyle = TextStyle(
                        fontFamily = lyricFontFamily,
                        fontSize = primaryFontSize.sp,
                        lineHeight = (primaryFontSize * 1.25f).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    inactiveTextStyle = TextStyle(
                        fontFamily = lyricFontFamily,
                        fontSize = (primaryFontSize * 0.84f).sp,
                        lineHeight = (primaryFontSize * 1.08f).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    secondaryTextStyle = TextStyle(
                        fontFamily = lyricFontFamily,
                        fontSize = secondaryFontSize.sp,
                        lineHeight = (secondaryFontSize * 1.28f).sp,
                        fontWeight = lyricFontWeight,
                    ),
                    textAlign = lyricTextAlign,
                    showTranslation = lyricDisplaySettings.showTranslation,
                    wordLiftEnabled = lyricDisplaySettings.wordLiftEnabled,
                    useBlurEffect = lyricDisplaySettings.blurEffectEnabled,
                    perspectiveEffectEnabled = lyricDisplaySettings.perspectiveEffectEnabled,
                    perspectiveAngleDegrees = lyricDisplaySettings.perspectiveAngleDegrees.toFloat(),
                    tapToSeekEnabled = lyricDisplaySettings.tapToSeekEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun LyricFontChoice.toFontFamily(): FontFamily = when (this) {
    LyricFontChoice.System -> FontFamily.Default
    LyricFontChoice.TideSans -> TideTunesFontFamilies.JakartaSans
    LyricFontChoice.TideCjk -> TideTunesFontFamilies.Sans
    LyricFontChoice.Monospace -> TideTunesFontFamilies.Mono
}

@Composable
private fun LyricDisplaySettings.pageFontFamilyFor(text: String): FontFamily? {
    if (!font.applyToLyricsPage) return null
    val choice = if (text.any(Char::isCjkCharacter)) font.cjkFont else font.westernFont
    return choice.toFontFamily()
}

private fun Char.isCjkCharacter(): Boolean = code in 0x2E80..0x9FFF ||
    code in 0xAC00..0xD7AF ||
    code in 0xF900..0xFAFF

// ── Compact (Mobile) Layout ──

@Composable
private fun CompactArtworkArea(
    artwork: Artwork?,
    coverSwipeEnabled: Boolean,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(372.dp),
    ) {
        CoverImage(
            artwork = artwork,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 8.dp),
            maxArtworkSize = 340.dp,
            cornerRadius = 18.dp,
            swipeEnabled = coverSwipeEnabled,
            onSwipePrevious = onSwipePrevious,
            onSwipeNext = onSwipeNext,
        )

        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_collapse),
            contentDescription = stringResource(Res.string.player_close),
            colors = TideIconButtonColors(
                iconTint = Color.White,
            ),
            onClick = onDismiss,
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 8.dp),
        )
    }
}

@Composable
private fun CompactTrackRow(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackInformation(
            track = track,
            lyricDisplaySettings = lyricDisplaySettings,
            playerInteractionSettings = playerInteractionSettings,
            modifier = Modifier.weight(1f),
            lightTheme = true,
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onLikedChange(!liked) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_heart),
                contentDescription = stringResource(
                    if (liked) Res.string.player_remove_favorite else Res.string.player_add_favorite
                ),
                tint = if (liked) MiuixTheme.colorScheme.primary else Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(25.dp),
            )
        }
        NowPlayingMoreButton(
            hasLyric = track?.hasLyric == true,
            nowPlayingState = state,
            onAction = onAction,
        )
    }
}

private data class CompactLyricText(
    val primary: String,
    val translation: String?,
)

private fun String.toCompactLyricText(): CompactLyricText {
    val parts = lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    return CompactLyricText(
        primary = parts.firstOrNull().orEmpty(),
        translation = parts.drop(1).joinToString("\n").takeIf(String::isNotBlank),
    )
}

@Composable
private fun CompactLyricPreview(
    lyricLines: List<LyricLine>,
    activeLyricIndex: Int,
    showTranslation: Boolean,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewStart = (activeLyricIndex - 1).coerceIn(
        minimumValue = 0,
        maximumValue = (lyricLines.size - 3).coerceAtLeast(0),
    )
    val previewLines = lyricLines.drop(previewStart).take(3)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .then(if (previewLines.isNotEmpty()) Modifier.clickable(onClick = onOpenLyrics) else Modifier),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (previewLines.isEmpty()) {
            Text(
                text = stringResource(Res.string.player_lyrics_unavailable),
                color = Color.White.copy(alpha = 0.52f),
                style = MiuixTheme.textStyles.title3.copy(fontSize = 18.sp, lineHeight = 25.sp),
            )
        } else {
            previewLines.forEachIndexed { index, lyricLine ->
                val lyricIndex = previewStart + index
                val lyricText = lyricLine.text.toCompactLyricText()
                val isActive = lyricIndex == activeLyricIndex
                Text(
                    text = lyricText.primary,
                    color = Color.White.copy(alpha = if (isActive) 0.94f else 0.48f),
                    style = if (isActive) {
                        MiuixTheme.textStyles.title3.copy(fontSize = 18.sp, lineHeight = 25.sp)
                    } else {
                        MiuixTheme.textStyles.body1.copy(fontSize = 16.sp, lineHeight = 22.sp)
                    },
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isActive && showTranslation) {
                    lyricText.translation?.let { translation ->
                        Text(
                            text = translation,
                            color = Color.White.copy(alpha = 0.55f),
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp, lineHeight = 18.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactImmersiveNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    palette: ArtworkPalette,
    lyricLines: List<LyricLine>,
    activeLyricIndex: Int,
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val artworkHeight = maxHeight * 0.59f
        val contentTop = maxHeight * 0.44f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkHeight)
                .pointerInput(playerInteractionSettings.coverSwipeEnabled) {
                    if (playerInteractionSettings.coverSwipeEnabled) {
                        var accumulatedDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onHorizontalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
                            onDragEnd = {
                                if (abs(accumulatedDrag) >= 72f) {
                                    if (accumulatedDrag > 0f && state.queue.canPlayPrevious) {
                                        onAction(NowPlayingAction.PlayPrevious)
                                    } else if (accumulatedDrag < 0f && state.queue.canPlayNext) {
                                        onAction(NowPlayingAction.PlayNext)
                                    }
                                }
                            },
                        )
                    }
                },
        ) {
            ArtworkImage(
                modifier = Modifier.fillMaxSize(),
                artwork = track?.artwork,
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to palette.darkMuted.copy(alpha = 0.08f),
                            0.44f to palette.darkMuted.copy(alpha = 0.06f),
                            0.78f to palette.muted.copy(alpha = 0.82f),
                            1f to palette.muted,
                        ),
                    ),
            )
        }

        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_collapse),
            contentDescription = stringResource(Res.string.player_close),
            colors = TideIconButtonColors(
                iconTint = Color.White.copy(alpha = 0.76f),
            ),
            onClick = { onAction(NowPlayingAction.NavigateBack) },
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 28.dp, top = contentTop, end = 20.dp, bottom = 24.dp),
        ) {
            CompactTrackRow(
                state = state,
                lyricDisplaySettings = lyricDisplaySettings,
                playerInteractionSettings = playerInteractionSettings,
                liked = liked,
                onLikedChange = onLikedChange,
                onAction = onAction,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CompactLyricPreview(
                lyricLines = lyricLines,
                activeLyricIndex = activeLyricIndex,
                showTranslation = lyricDisplaySettings.showTranslation,
                onOpenLyrics = { onAction(NowPlayingAction.OpenLyrics) },
            )
            progressContent(track?.durationMs)
            Spacer(modifier = Modifier.height(16.dp))
            TransportPanel(
                nowPlayingState = state,
                isSleepTimerEnabled = false,
                onAction = onAction,
                lightTheme = true,
                compact = true,
            )
        }
    }
}

@Composable
private fun CompactClassicNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    lyricLines: List<LyricLine>,
    activeLyricIndex: Int,
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CompactArtworkArea(
            artwork = track?.artwork,
            coverSwipeEnabled = playerInteractionSettings.coverSwipeEnabled,
            onSwipePrevious = {
                if (state.queue.canPlayPrevious) onAction(NowPlayingAction.PlayPrevious)
            },
            onSwipeNext = {
                if (state.queue.canPlayNext) onAction(NowPlayingAction.PlayNext)
            },
            onDismiss = { onAction(NowPlayingAction.NavigateBack) },
        )

        CompactTrackRow(
            state = state,
            lyricDisplaySettings = lyricDisplaySettings,
            playerInteractionSettings = playerInteractionSettings,
            liked = liked,
            onLikedChange = onLikedChange,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, top = 4.dp, end = 20.dp),
        )

        Spacer(modifier = Modifier.height(36.dp))

        CompactLyricPreview(
            lyricLines = lyricLines,
            activeLyricIndex = activeLyricIndex,
            showTranslation = lyricDisplaySettings.showTranslation,
            onOpenLyrics = { onAction(NowPlayingAction.OpenLyrics) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom: progress + transport
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            progressContent(track?.durationMs)
            Spacer(modifier = Modifier.height(16.dp))
            TransportPanel(
                nowPlayingState = state,
                isSleepTimerEnabled = false,
                onAction = onAction,
                lightTheme = true,
                compact = true,
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CompactNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    palette: ArtworkPalette,
    currentPositionMs: Long,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack
    var liked by remember(track?.id) { mutableStateOf(true) }
    val lyricLines = track?.lyrics?.lines.orEmpty()
    val activeLyricIndex = lyricLines.indexOfLast {
        it.duration.inWholeMilliseconds <= currentPositionMs
    }.coerceAtLeast(0)

    if (playerInteractionSettings.immersiveAlbumCover) {
        CompactImmersiveNowPlayingLayout(
            state = state,
            lyricDisplaySettings = lyricDisplaySettings,
            playerInteractionSettings = playerInteractionSettings,
            palette = palette,
            lyricLines = lyricLines,
            activeLyricIndex = activeLyricIndex,
            liked = liked,
            onLikedChange = { liked = it },
            progressContent = progressContent,
            onAction = onAction,
        )
    } else {
        CompactClassicNowPlayingLayout(
            state = state,
            lyricDisplaySettings = lyricDisplaySettings,
            playerInteractionSettings = playerInteractionSettings,
            lyricLines = lyricLines,
            activeLyricIndex = activeLyricIndex,
            liked = liked,
            onLikedChange = { liked = it },
            progressContent = progressContent,
            onAction = onAction,
        )
    }
}

// ── Main Screen ──

@Composable
fun NowPlayingScreen(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings = LyricDisplaySettings.Default,
    playerInteractionSettings: PlayerInteractionSettings = PlayerInteractionSettings.Default,
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
            .fillMaxSize(),
    ) {
        NowPlayingBackground(
            artwork = currentTrack?.artwork,
            palette = palette,
            immersive = playerInteractionSettings.immersiveAlbumCover,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= DesktopPlayerBreakpoint && maxHeight >= 520.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    MusicPlayerHeader(
                        hasLyric = currentTrack?.hasLyric == true,
                        nowPlayingState = state,
                        onAction = onAction,
                    )
                    DesktopNowPlayingLayout(
                        state = state,
                        lyricDisplaySettings = lyricDisplaySettings,
                        playerInteractionSettings = playerInteractionSettings,
                        currentPositionMs = currentPositionMs,
                        isSleepTimerEnabled = isSleepTimerEnabled,
                        progressContent = progressContent,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                CompactNowPlayingLayout(
                    state = state,
                    lyricDisplaySettings = lyricDisplaySettings,
                    playerInteractionSettings = playerInteractionSettings,
                    palette = palette,
                    currentPositionMs = currentPositionMs,
                    progressContent = progressContent,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun NowPlayingBackground(
    artwork: Artwork?,
    palette: ArtworkPalette,
    immersive: Boolean,
) {
    if (immersive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.darkMuted,
                            palette.muted,
                            palette.vibrant.copy(alpha = 0.86f),
                            MiuixTheme.colorScheme.surface,
                        ),
                    ),
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF100A1C).copy(alpha = 0.48f),
                            palette.darkMuted.copy(alpha = 0.34f),
                        ),
                    ),
                ),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.muted),
        )
        ArtworkImage(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.18f
                    scaleY = 1.18f
                    alpha = 0.78f
                }
                .blur(48.dp),
            artwork = artwork,
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.vibrant.copy(alpha = 0.24f),
                            palette.darkMuted.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.34f),
                        ),
                    ),
                ),
        )
    }
}

// ── Progress Panel ──

@Composable
fun NowPlayingProgressPanel(
    progressState: NowPlayingProgressState,
    trackDurationMs: Long?,
    playerInteractionSettings: PlayerInteractionSettings = PlayerInteractionSettings.Default,
    onAction: (NowPlayingAction) -> Unit,
    lightTheme: Boolean = false,
) {
    val totalDurationMs = trackDurationMs ?: progressState.playerDuration.inWholeMilliseconds

    MusicSlider(
        currentDuration = formatPlayerDuration(progressState.currentDuration),
        currentDurationMs = toMusicDurationMs(progressState.currentDuration),
        bufferDurationMs = progressState.bufferDuration.inWholeMilliseconds.coerceAtLeast(0).toULong(),
        totalDuration = formatPlayerDuration(totalDurationMs.milliseconds),
        totalDurationMs = totalDurationMs.coerceAtLeast(0).toULong(),
        tapToSeekEnabled = playerInteractionSettings.tapProgressToSeekEnabled,
        showTotalDuration = playerInteractionSettings.showTotalDuration,
        onChangeMusicPosition = { nextMs -> onAction(NowPlayingAction.SeekTo(nextMs)) },
        lightTheme = lightTheme,
    )
}

// ── Formatting ──

private fun formatPlayerDuration(duration: kotlin.time.Duration): String {
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
