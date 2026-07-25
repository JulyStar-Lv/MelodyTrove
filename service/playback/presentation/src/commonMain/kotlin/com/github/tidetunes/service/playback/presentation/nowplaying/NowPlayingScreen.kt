package com.github.tidetunes.service.playback.presentation.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import tidetunes.service.playback.presentation.generated.resources.icon_heart_compact
import tidetunes.service.playback.presentation.generated.resources.icon_heart_compact_filled
import tidetunes.service.playback.presentation.generated.resources.icon_lyrics
import tidetunes.service.playback.presentation.generated.resources.icon_pause
import tidetunes.service.playback.presentation.generated.resources.icon_play
import tidetunes.service.playback.presentation.generated.resources.icon_play_next
import tidetunes.service.playback.presentation.generated.resources.icon_play_previous
import tidetunes.service.playback.presentation.generated.resources.icon_timelapse
import tidetunes.service.playback.presentation.generated.resources.icon_more_compact
import tidetunes.service.playback.presentation.generated.resources.icon_transport_next
import tidetunes.service.playback.presentation.generated.resources.icon_transport_pause
import tidetunes.service.playback.presentation.generated.resources.icon_transport_play
import tidetunes.service.playback.presentation.generated.resources.icon_transport_previous
import tidetunes.service.playback.presentation.generated.resources.icon_transport_queue
import tidetunes.service.playback.presentation.generated.resources.icon_transport_repeat
import tidetunes.service.playback.presentation.generated.resources.icon_transport_repeat_one
import tidetunes.service.playback.presentation.generated.resources.icon_transport_shuffle
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
import tidetunes.service.playback.presentation.generated.resources.player_loading_lyrics
import tidetunes.service.playback.presentation.generated.resources.player_lyrics_unavailable
import tidetunes.service.playback.presentation.generated.resources.player_more_options
import tidetunes.service.playback.presentation.generated.resources.player_next_track
import tidetunes.service.playback.presentation.generated.resources.player_pause
import tidetunes.service.playback.presentation.generated.resources.player_play
import tidetunes.service.playback.presentation.generated.resources.player_previous_track
import tidetunes.service.playback.presentation.generated.resources.player_queue
import tidetunes.service.playback.presentation.generated.resources.player_remove_favorite
import tidetunes.service.playback.presentation.generated.resources.player_list_repeat
import tidetunes.service.playback.presentation.generated.resources.player_shuffle
import tidetunes.service.playback.presentation.generated.resources.player_single_repeat
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
    compact: Boolean = false,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Box {
        if (compact) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { moreMenuExpanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_more_compact),
                    contentDescription = stringResource(Res.string.player_more_options),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            TideIconButton(
                size = TideIconButtonSize.Medium,
                variant = TideIconButtonVariant.Default,
                painter = painterResource(Res.drawable.icon_vertialcal_more),
                contentDescription = stringResource(Res.string.player_more_options),
                onClick = { moreMenuExpanded = true },
            )
        }
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.offset(20.dp, 20.dp),
        ) {
            TideContextMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false },
                compact = compact,
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
    compact: Boolean = false,
    immersive: Boolean = false,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbingDurationMs by remember { mutableStateOf(currentDurationMs) }
    val displayedDurationMs = if (isScrubbing) scrubbingDurationMs else currentDurationMs
    val sliderRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f)
    val labelColor = if (immersive) {
        Color.White.copy(alpha = 0.52f)
    } else if (lightTheme) {
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
            height = if (immersive) {
                if (compact) 16.dp else 24.dp
            } else {
                16.dp
            },
            trackHeight = if (immersive) 3.dp else 4.dp,
            thumbSize = if (immersive) 8.dp else 12.dp,
            activeThumbSize = if (immersive) {
                if (compact) 10.dp else 12.dp
            } else {
                16.dp
            },
            trackColorOverride = Color.White.copy(alpha = if (immersive) 0.20f else 0.28f),
            bufferColorOverride = Color.White.copy(alpha = if (immersive) 0.20f else 0.44f),
            activeTrackColorOverride = Color.White.copy(alpha = if (immersive) 0.85f else 1f),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (immersive) {
                        if (compact) 2.dp else 4.dp
                    } else {
                        0.dp
                    },
                ),
        ) {
            val durationStyle = if (immersive) {
                TextStyle(
                    fontFamily = TideTunesFontFamilies.Mono,
                    fontSize = if (compact) 12.sp else 15.sp,
                    lineHeight = if (compact) 17.sp else 21.sp,
                )
            } else {
                MiuixTheme.textStyles.footnote2.copy(fontFamily = TideTunesFontFamilies.Mono)
            }
            Text(
                text = currentDuration,
                color = labelColor,
                style = durationStyle,
            )
            Text(
                text = if (showTotalDuration) {
                    totalDuration
                } else {
                    val remainingMs = totalDurationMs.toLong() - displayedDurationMs.toLong()
                    "-${formatPlayerDuration(remainingMs.coerceAtLeast(0).milliseconds)}"
                },
                color = labelColor,
                style = durationStyle,
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
    shadowOffsetY: androidx.compose.ui.unit.Dp = 18.dp,
    shadowBlurRadius: androidx.compose.ui.unit.Dp = 38.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 0.dp,
    borderColor: Color = Color.Transparent,
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
                    offsetY = shadowOffsetY,
                    blurRadius = shadowBlurRadius,
                )
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(cornerRadius),
                )
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.24f)),
        ) {
            ArtworkImage(
                modifier = Modifier.fillMaxSize(),
                artwork = artwork,
                contentScale = ContentScale.Crop,
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
    compact: Boolean = false,
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
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            color = textColor,
            style = TextStyle(
                fontFamily = titleFontFamily ?: TideTunesFontFamilies.Sans,
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = if (titleFontFamily == null) FontWeight.Bold else customFontWeight,
                lineHeight = if (compact) 28.sp else 30.sp,
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = artistText,
            color = mutedColor,
            style = if (compact) {
                TextStyle(
                    fontFamily = artistFontFamily ?: TideTunesFontFamilies.Sans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (artistFontFamily == null) FontWeight.Medium else customFontWeight,
                )
            } else {
                MiuixTheme.textStyles.body1.let { style ->
                    if (artistFontFamily == null) style else style.copy(
                        fontFamily = artistFontFamily,
                        fontWeight = customFontWeight,
                    )
                }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
        )
        annotation?.takeUnless { compact }?.let { text ->
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
    dense: Boolean = false,
) {
    val controls = nowPlayingState.controls
    val queue = nowPlayingState.queue
    if (compact) {
        CompactTransportPanel(
            nowPlayingState = nowPlayingState,
            onAction = onAction,
            dense = dense,
        )
        return
    }

    val iconTint = if (lightTheme) Color.White.copy(alpha = 0.90f) else MiuixTheme.colorScheme.onSurface
    val dimTint = if (lightTheme) Color.White.copy(alpha = 0.45f) else MiuixTheme.colorScheme.onSurfaceVariantActions
    val accentTint = if (lightTheme) Color.White else MiuixTheme.colorScheme.primary
    val primaryButtonColor = if (lightTheme) Color.White else MiuixTheme.colorScheme.onSurface
    val primaryIconTint = if (lightTheme) Color(0xFF06040E) else MiuixTheme.colorScheme.background
    val primaryButtonSize = 62.dp
    val primaryIconSize = 24.dp
    val repeatDrawable: DrawableResource = when (controls.repeatMode) {
        RepeatMode.Off -> Res.drawable.icon_transport_repeat
        RepeatMode.One -> Res.drawable.icon_transport_repeat_one
        RepeatMode.All -> Res.drawable.icon_transport_repeat
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_timelapse),
            colors = TideIconButtonColors(
                iconTint = if (isSleepTimerEnabled) {
                    accentTint
                } else {
                    dimTint
                },
                buttonBg = Color.Transparent,
            ),
            onClick = { onAction(NowPlayingAction.OpenSleepTimer) },
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
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(repeatDrawable),
            colors = TideIconButtonColors(
                iconTint = if (controls.repeatMode != RepeatMode.Off) accentTint else dimTint,
                buttonBg = Color.Transparent,
            ),
            onClick = { onAction(NowPlayingAction.CycleRepeatMode) },
        )
    }
}

@Composable
private fun CompactTransportPanel(
    nowPlayingState: NowPlayingState,
    onAction: (NowPlayingAction) -> Unit,
    dense: Boolean,
) {
    val controls = nowPlayingState.controls
    val queue = nowPlayingState.queue
    val playbackModeDrawable: DrawableResource = when {
        controls.shuffleEnabled -> Res.drawable.icon_transport_shuffle
        controls.repeatMode == RepeatMode.One -> Res.drawable.icon_transport_repeat_one
        else -> Res.drawable.icon_transport_repeat
    }
    val playbackModeDescription = stringResource(
        when {
            controls.shuffleEnabled -> Res.string.player_shuffle
            controls.repeatMode == RepeatMode.One -> Res.string.player_single_repeat
            else -> Res.string.player_list_repeat
        },
    )
    val playbackModeTint = if (controls.shuffleEnabled || controls.repeatMode == RepeatMode.One) {
        MiuixTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.82f)
    }
    val secondaryButtonSize = if (dense) 44.dp else 56.dp
    val primaryButtonSize = if (dense) 58.dp else 72.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (dense) 62.dp else 84.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactTransportButton(
            painter = playbackModeDrawable,
            contentDescription = playbackModeDescription,
            tint = playbackModeTint,
            buttonSize = secondaryButtonSize,
            iconSize = if (dense) 21.dp else 24.dp,
            onClick = { onAction(NowPlayingAction.CycleRepeatMode) },
            modifier = Modifier.weight(1f),
        )
        CompactTransportButton(
            painter = Res.drawable.icon_transport_previous,
            contentDescription = stringResource(Res.string.player_previous_track),
            tint = Color.White,
            buttonSize = secondaryButtonSize,
            iconSize = if (dense) 28.dp else 30.dp,
            enabled = queue.canPlayPrevious,
            onClick = { onAction(NowPlayingAction.PlayPrevious) },
            modifier = Modifier.weight(1f),
        )
        CompactTransportButton(
            painter = if (controls.isPlaying) {
                Res.drawable.icon_transport_pause
            } else {
                Res.drawable.icon_transport_play
            },
            contentDescription = stringResource(
                if (controls.isPlaying) Res.string.player_pause else Res.string.player_play,
            ),
            tint = Color.White,
            background = Color.White.copy(alpha = 0.16f),
            buttonSize = primaryButtonSize,
            iconSize = when {
                controls.isPlaying && dense -> 28.dp
                controls.isPlaying -> 32.dp
                dense -> 32.dp
                else -> 36.dp
            },
            enabled = controls.isPlaying || !controls.isLoading,
            onClick = {
                onAction(if (controls.isPlaying) NowPlayingAction.Pause else NowPlayingAction.Resume)
            },
            iconOffsetX = if (controls.isPlaying) 0.dp else 3.dp,
            modifier = Modifier.weight(1f),
        )
        CompactTransportButton(
            painter = Res.drawable.icon_transport_next,
            contentDescription = stringResource(Res.string.player_next_track),
            tint = Color.White,
            buttonSize = secondaryButtonSize,
            iconSize = if (dense) 28.dp else 30.dp,
            enabled = queue.canPlayNext,
            onClick = { onAction(NowPlayingAction.PlayNext) },
            modifier = Modifier.weight(1f),
        )
        CompactTransportButton(
            painter = Res.drawable.icon_transport_queue,
            contentDescription = stringResource(Res.string.player_queue),
            tint = Color.White.copy(alpha = 0.72f),
            buttonSize = secondaryButtonSize,
            iconSize = if (dense) 22.dp else 25.dp,
            onClick = { onAction(NowPlayingAction.OpenQueue) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CompactTransportButton(
    painter: DrawableResource,
    contentDescription: String,
    tint: Color,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    enabled: Boolean = true,
    iconOffsetX: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .then(
                    if (background.alpha > 0f) {
                        Modifier.shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.18f),
                            spotColor = Color.Black.copy(alpha = 0.18f),
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(CircleShape)
                .background(background, CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(painter),
                contentDescription = contentDescription,
                tint = tint.copy(alpha = if (enabled) tint.alpha else tint.alpha * 0.32f),
                modifier = Modifier
                    .size(iconSize)
                    .offset(x = iconOffsetX),
            )
        }
    }
}

// ── Desktop Layout ──

@Composable
private fun DesktopNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    currentPositionMs: Long,
    isSeeking: Boolean,
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
            isPlaying = state.controls.isPlaying && !isSeeking,
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
    isPlaying: Boolean,
    coverSwipeEnabled: Boolean,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = spring(stiffness = 180f),
        label = "compactArtworkScale",
    )
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        CoverImage(
            artwork = artwork,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                },
            maxArtworkSize = 356.dp,
            cornerRadius = 28.dp,
            shadowOffsetY = 20.dp,
            shadowBlurRadius = 44.dp,
            borderWidth = 1.dp,
            borderColor = Color.White.copy(alpha = 0.10f),
            swipeEnabled = coverSwipeEnabled,
            onSwipePrevious = onSwipePrevious,
            onSwipeNext = onSwipeNext,
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
    dense: Boolean = false,
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
            compact = true,
        )
        Box(
            modifier = Modifier
                .size(if (dense) 40.dp else 44.dp)
                .clip(CircleShape)
                .clickable { onLikedChange(!liked) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (liked) {
                        Res.drawable.icon_heart_compact_filled
                    } else {
                        Res.drawable.icon_heart_compact
                    },
                ),
                contentDescription = stringResource(
                    if (liked) Res.string.player_remove_favorite else Res.string.player_add_favorite
                ),
                tint = if (liked) MiuixTheme.colorScheme.primary else Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(if (dense) 20.dp else 24.dp),
            )
        }
        NowPlayingMoreButton(
            hasLyric = track?.hasLyric == true,
            nowPlayingState = state,
            onAction = onAction,
            compact = true,
        )
    }
}

@Composable
private fun CompactLyricsSurface(
    track: NowPlayingTrackItem?,
    lyricDisplaySettings: LyricDisplaySettings,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
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
    val lyricTextAlign = when (lyricDisplaySettings.textAlignment) {
        LyricTextAlignment.Left -> TextAlign.Start
        LyricTextAlignment.Center -> TextAlign.Center
        LyricTextAlignment.Right -> TextAlign.End
    }
    val lyricFontFamily = if (lyricDisplaySettings.font.applyToLyricsPage) {
        val containsCjk = lyricLines.any { line -> line.text.any(Char::isCjkCharacter) }
        val choice = if (containsCjk) {
            lyricDisplaySettings.font.cjkFont
        } else {
            lyricDisplaySettings.font.westernFont
        }
        choice.toFontFamily()
    } else {
        FontFamily.Default
    }
    val lyricFontWeight = FontWeight(lyricDisplaySettings.font.weight.coerceIn(100, 900))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.TopStart,
    ) {
        when {
            loadState == LyricsLoadState.Loading -> {
                CompactLyricsStatus(
                    text = stringResource(Res.string.player_loading_lyrics),
                    dense = dense,
                )
            }
            loadState == LyricsLoadState.Missing ||
                loadState == LyricsLoadState.Failed ||
                syncedLyrics.lines.isEmpty() -> {
                CompactLyricsStatus(
                    text = stringResource(Res.string.player_lyrics_unavailable),
                    dense = dense,
                )
            }
            else -> {
                val activeFontSize = if (dense) 16f else 17f
                val inactiveFontSize = if (dense) 14f else 15f
                val secondaryFontSize = if (dense) 11f else 12f
                TideLyricsView(
                    lyrics = syncedLyrics,
                    currentPositionMs = currentPositionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                    isPlaying = isPlaying,
                    onLineClick = { onOpenLyrics() },
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.42f),
                    activeTextStyle = TextStyle(
                        fontFamily = lyricFontFamily,
                        fontSize = activeFontSize.sp,
                        lineHeight = (activeFontSize * 1.4f).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    inactiveTextStyle = TextStyle(
                        fontFamily = lyricFontFamily,
                        fontSize = inactiveFontSize.sp,
                        lineHeight = (inactiveFontSize * 1.4f).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    secondaryTextStyle = TextStyle(
                        fontFamily = lyricFontFamily,
                        fontSize = secondaryFontSize.sp,
                        lineHeight = (secondaryFontSize * 1.34f).sp,
                        fontWeight = lyricFontWeight,
                    ),
                    textAlign = lyricTextAlign,
                    lineSpacing = if (dense) 0.dp else 2.dp,
                    showTranslation = lyricDisplaySettings.showTranslation,
                    wordLiftEnabled = lyricDisplaySettings.wordLiftEnabled,
                    useBlurEffect = lyricDisplaySettings.blurEffectEnabled,
                    tapToSeekEnabled = true,
                    verticalContentPaddingFraction = 0.04f,
                    lineHorizontalPadding = 8.dp,
                    lineVerticalPadding = if (dense) 1.dp else 2.dp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun CompactLyricsStatus(
    text: String,
    dense: Boolean,
) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.52f),
        style = MiuixTheme.textStyles.title3.copy(
            fontSize = if (dense) 16.sp else 17.sp,
            lineHeight = if (dense) 22.sp else 24.sp,
        ),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = if (dense) 4.dp else 12.dp),
    )
}

@Composable
private fun CompactClassicNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    currentPositionMs: Long,
    isSeeking: Boolean,
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 90.dp, bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.88f)
                .widthIn(max = 356.dp),
        ) {
            CompactArtworkArea(
                artwork = track?.artwork,
                isPlaying = state.controls.isPlaying,
                coverSwipeEnabled = playerInteractionSettings.coverSwipeEnabled,
                onSwipePrevious = {
                    if (state.queue.canPlayPrevious) onAction(NowPlayingAction.PlayPrevious)
                },
                onSwipeNext = {
                    if (state.queue.canPlayNext) onAction(NowPlayingAction.PlayNext)
                },
                modifier = Modifier.fillMaxWidth(),
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
                    .padding(start = 8.dp, top = 20.dp, end = 8.dp),
            )

            CompactLyricsSurface(
                track = track,
                lyricDisplaySettings = lyricDisplaySettings,
                currentPositionMs = currentPositionMs,
                isPlaying = state.controls.isPlaying && !isSeeking,
                onOpenLyrics = { onAction(NowPlayingAction.OpenLyrics) },
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 16.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                Box(modifier = Modifier.offset(y = (-8).dp)) {
                    progressContent(track?.durationMs)
                }
                Spacer(modifier = Modifier.height(4.dp))
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
}

@Composable
private fun CompactLandscapeNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    currentPositionMs: Long,
    isSeeking: Boolean,
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    progressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val artworkColumnWidth = maxOf(maxWidth * 0.47f, 413.dp).coerceAtMost(maxWidth * 0.56f)
        val stageHeight = minOf(
            (maxHeight * 0.82f).coerceAtLeast(300.dp),
            maxHeight - 34.dp,
            340.dp,
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(stageHeight)
                    .align(Alignment.CenterVertically)
                    .padding(start = 40.dp, end = 8.dp),
            ) {
                CompactTrackRow(
                    state = state,
                    lyricDisplaySettings = lyricDisplaySettings,
                    playerInteractionSettings = playerInteractionSettings,
                    liked = liked,
                    onLikedChange = onLikedChange,
                    onAction = onAction,
                    dense = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CompactLyricsSurface(
                    track = track,
                    lyricDisplaySettings = lyricDisplaySettings,
                    currentPositionMs = currentPositionMs,
                    isPlaying = state.controls.isPlaying && !isSeeking,
                    onOpenLyrics = { onAction(NowPlayingAction.OpenLyrics) },
                    dense = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp, bottom = 12.dp),
                )
                Box(modifier = Modifier.offset(y = (-8).dp)) {
                    progressContent(track?.durationMs)
                }
                TransportPanel(
                    nowPlayingState = state,
                    isSleepTimerEnabled = false,
                    onAction = onAction,
                    lightTheme = true,
                    compact = true,
                    dense = true,
                )
            }

            Box(
                modifier = Modifier
                    .width(artworkColumnWidth)
                    .fillMaxHeight()
                    .padding(start = 20.dp, end = 87.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                CoverImage(
                    artwork = track?.artwork,
                    modifier = Modifier.size(stageHeight),
                    maxArtworkSize = 340.dp,
                    cornerRadius = 18.dp,
                    shadowOffsetY = 16.dp,
                    shadowBlurRadius = 42.dp,
                    borderWidth = 1.dp,
                    borderColor = Color.White.copy(alpha = 0.10f),
                    swipeEnabled = playerInteractionSettings.coverSwipeEnabled,
                    onSwipePrevious = {
                        if (state.queue.canPlayPrevious) onAction(NowPlayingAction.PlayPrevious)
                    },
                    onSwipeNext = {
                        if (state.queue.canPlayNext) onAction(NowPlayingAction.PlayNext)
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactNowPlayingLayout(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    currentPositionMs: Long,
    isSeeking: Boolean,
    progressContent: @Composable (Long?) -> Unit,
    compactProgressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
    val track = state.currentTrack
    var liked by remember(track?.id) { mutableStateOf(true) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isShortLandscape = maxWidth >= 640.dp && maxWidth > maxHeight && maxHeight < 520.dp
        if (isShortLandscape) {
            CompactLandscapeNowPlayingLayout(
                state = state,
                lyricDisplaySettings = lyricDisplaySettings,
                playerInteractionSettings = playerInteractionSettings,
                currentPositionMs = currentPositionMs,
                isSeeking = isSeeking,
                liked = liked,
                onLikedChange = { liked = it },
                progressContent = compactProgressContent,
                onAction = onAction,
            )
        } else {
            CompactClassicNowPlayingLayout(
                state = state,
                lyricDisplaySettings = lyricDisplaySettings,
                playerInteractionSettings = playerInteractionSettings,
                currentPositionMs = currentPositionMs,
                isSeeking = isSeeking,
                liked = liked,
                onLikedChange = { liked = it },
                progressContent = progressContent,
                onAction = onAction,
            )
        }
    }
}

// ── Main Screen ──

@Composable
fun NowPlayingScreen(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings = LyricDisplaySettings.Default,
    playerInteractionSettings: PlayerInteractionSettings = PlayerInteractionSettings.Default,
    currentPositionMs: Long,
    isSeeking: Boolean = false,
    isSleepTimerEnabled: Boolean,
    progressContent: @Composable (Long?) -> Unit,
    compactProgressContent: @Composable (Long?) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTrack = state.currentTrack

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxSize(),
    ) {
        NowPlayingBackground(artwork = currentTrack?.artwork)
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
                        isSeeking = isSeeking,
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
                    currentPositionMs = currentPositionMs,
                    isSeeking = isSeeking,
                    progressContent = progressContent,
                    compactProgressContent = compactProgressContent,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun NowPlayingBackground(artwork: Artwork?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08060E)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFF5B8A), Color(0xFF7A6CFF)),
                    ),
                ),
        )
        ArtworkImage(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.36f
                    scaleY = 1.36f
                    alpha = 0.75f
                }
                .blur(54.dp),
            artwork = artwork,
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.10f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF08060E).copy(alpha = 0.28f),
                            0.52f to Color(0xFF08060E).copy(alpha = 0.46f),
                            1f to Color(0xFF08060E).copy(alpha = 0.72f),
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
    compact: Boolean = false,
    immersive: Boolean = false,
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
        compact = compact,
        immersive = immersive,
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
