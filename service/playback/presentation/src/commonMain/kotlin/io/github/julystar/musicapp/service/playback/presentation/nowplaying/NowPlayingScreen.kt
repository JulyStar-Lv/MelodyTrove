package io.github.julystar.musicapp.service.playback.presentation.nowplaying

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.LyricDisplaySettings
import io.github.julystar.musicapp.core.domain.model.LyricFontChoice
import io.github.julystar.musicapp.core.domain.model.LyricTextAlignment
import io.github.julystar.musicapp.core.domain.model.LyricsLoadState
import io.github.julystar.musicapp.core.domain.model.PlayerInteractionSettings
import io.github.julystar.musicapp.core.lyrics.ui.LyricsView
import io.github.julystar.musicapp.core.presentation.components.DesignContextMenu
import io.github.julystar.musicapp.core.presentation.components.DesignContextMenuItem
import io.github.julystar.musicapp.core.presentation.components.DesignIconButton
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonVariant
import io.github.julystar.musicapp.core.presentation.components.DesignPlayerControlButton
import io.github.julystar.musicapp.core.presentation.components.DesignPlayerControlSize
import io.github.julystar.musicapp.core.presentation.components.DesignPlayerControlVariant
import io.github.julystar.musicapp.core.presentation.components.DesignSlider
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.components.dropShadow
import io.github.julystar.musicapp.core.presentation.media.ArtworkImage
import io.github.julystar.musicapp.core.presentation.platform.LocalDesktopTitleBarInset
import io.github.julystar.musicapp.core.presentation.theme.DesignFontFamilies
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import io.github.julystar.musicapp.core.utils.toMusicDurationMs
import io.github.julystar.musicapp.service.playback.domain.RepeatMode
import io.github.julystar.musicapp.service.playback.presentation.transition.playerArtworkSharedElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import musicapp.core.presentation.generated.resources.Res as CoreRes
import musicapp.core.presentation.generated.resources.icon_deleteseep
import musicapp.core.presentation.generated.resources.icon_download
import musicapp.core.presentation.generated.resources.icon_search
import musicapp.core.presentation.generated.resources.icon_settings_sliders
import musicapp.service.playback.presentation.generated.resources.Res
import musicapp.service.playback.presentation.generated.resources.downloads_title
import musicapp.service.playback.presentation.generated.resources.icon_back
import musicapp.service.playback.presentation.generated.resources.icon_heart_compact
import musicapp.service.playback.presentation.generated.resources.icon_heart_compact_filled
import musicapp.service.playback.presentation.generated.resources.icon_lyrics
import musicapp.service.playback.presentation.generated.resources.icon_more_compact
import musicapp.service.playback.presentation.generated.resources.icon_transport_next
import musicapp.service.playback.presentation.generated.resources.icon_transport_pause
import musicapp.service.playback.presentation.generated.resources.icon_transport_play
import musicapp.service.playback.presentation.generated.resources.icon_transport_previous
import musicapp.service.playback.presentation.generated.resources.icon_transport_queue
import musicapp.service.playback.presentation.generated.resources.icon_transport_repeat
import musicapp.service.playback.presentation.generated.resources.icon_transport_repeat_one
import musicapp.service.playback.presentation.generated.resources.icon_transport_shuffle
import musicapp.core.presentation.generated.resources.icon_timelapse
import musicapp.service.playback.presentation.generated.resources.icon_vertialcal_more
import musicapp.service.playback.presentation.generated.resources.music_lyric_add
import musicapp.service.playback.presentation.generated.resources.music_lyric_fail
import musicapp.service.playback.presentation.generated.resources.music_lyric_no_desc
import musicapp.service.playback.presentation.generated.resources.music_lyric_remove
import musicapp.service.playback.presentation.generated.resources.music_lyric_try_add_desc
import musicapp.service.playback.presentation.generated.resources.music_player_context_menu_remove
import musicapp.service.playback.presentation.generated.resources.music_player_edit_lyric_timing
import musicapp.service.playback.presentation.generated.resources.music_player_edit_metadata
import musicapp.service.playback.presentation.generated.resources.music_player_search_metadata
import musicapp.service.playback.presentation.generated.resources.now_playing_title
import musicapp.service.playback.presentation.generated.resources.player_add_favorite
import musicapp.service.playback.presentation.generated.resources.player_loading_lyrics
import musicapp.service.playback.presentation.generated.resources.player_lyrics_unavailable
import musicapp.service.playback.presentation.generated.resources.player_more_options
import musicapp.service.playback.presentation.generated.resources.player_next_track
import musicapp.service.playback.presentation.generated.resources.player_pause
import musicapp.service.playback.presentation.generated.resources.player_play
import musicapp.service.playback.presentation.generated.resources.player_previous_track
import musicapp.service.playback.presentation.generated.resources.player_queue
import musicapp.service.playback.presentation.generated.resources.player_remove_favorite
import musicapp.service.playback.presentation.generated.resources.player_list_repeat
import musicapp.service.playback.presentation.generated.resources.player_shuffle
import musicapp.service.playback.presentation.generated.resources.player_single_repeat
import musicapp.service.playback.presentation.generated.resources.player_unknown_artist
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DesktopPlayerBreakpoint = 860.dp
private const val NowPlayingDismissDistanceFraction = 0.5f
private val NowPlayingDismissVelocityThreshold = 900.dp

@Composable
private fun NowPlayingDismissGestureArea(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
    )
}

// ── Player Header ──

@Composable
private fun MusicPlayerHeader(
    onAction: (NowPlayingAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth(),
    ) {
        DesignIconButton(
            size = DesignIconButtonSize.Medium,
            variant = DesignIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_back),
            onClick = { onAction(NowPlayingAction.NavigateBack) },
        )
        Text(
            text = stringResource(Res.string.now_playing_title),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.size(36.dp))
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
            DesignIconButton(
                size = DesignIconButtonSize.Medium,
                variant = DesignIconButtonVariant.Default,
                painter = painterResource(Res.drawable.icon_vertialcal_more),
                contentDescription = stringResource(Res.string.player_more_options),
                onClick = { moreMenuExpanded = true },
            )
        }
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.offset(20.dp, 20.dp),
        ) {
            DesignContextMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false },
                compact = compact,
                items = listOfNotNull(
                    DesignContextMenuItem(
                        label = Res.string.music_player_search_metadata,
                        icon = CoreRes.drawable.icon_search,
                        onClick = {
                            moreMenuExpanded = false
                            onAction(NowPlayingAction.SearchMetadata)
                        },
                    ),
                    if (nowPlayingState.externalEditorSupported) {
                        DesignContextMenuItem(
                            label = Res.string.music_player_edit_metadata,
                            icon = CoreRes.drawable.icon_settings_sliders,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.OpenMetadataEditor)
                            },
                        )
                    } else null,
                    if (nowPlayingState.externalEditorSupported) {
                        DesignContextMenuItem(
                            label = Res.string.music_player_edit_lyric_timing,
                            icon = CoreRes.drawable.icon_timelapse,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.OpenLyricTimingEditor)
                            },
                        )
                    } else null,
                    if (hasLyric) {
                        DesignContextMenuItem(
                            label = Res.string.music_lyric_remove,
                            icon = CoreRes.drawable.icon_deleteseep,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.RemoveLyric)
                            },
                        )
                    } else {
                        DesignContextMenuItem(
                            label = Res.string.music_lyric_add,
                            icon = Res.drawable.icon_lyrics,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.AddLyric)
                            },
                        )
                    },
                    if (nowPlayingState.currentTrack?.canDownload == true) {
                        DesignContextMenuItem(
                            label = Res.string.downloads_title,
                            icon = CoreRes.drawable.icon_download,
                            onClick = {
                                moreMenuExpanded = false
                                onAction(NowPlayingAction.DownloadCurrentTrack)
                            },
                        )
                    } else null,
                    DesignContextMenuItem(
                        label = Res.string.music_player_context_menu_remove,
                        icon = CoreRes.drawable.icon_deleteseep,
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
        DesignSlider(
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
                    fontFamily = DesignFontFamilies.Mono,
                    fontSize = if (compact) 12.sp else 15.sp,
                    lineHeight = if (compact) 17.sp else 21.sp,
                )
            } else {
                MiuixTheme.textStyles.footnote2.copy(fontFamily = DesignFontFamilies.Mono)
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
    maxArtworkSize: Dp = 400.dp,
    cornerRadius: Dp = DesignTokens.shapes.xl,
    shadowOffsetY: Dp = 18.dp,
    shadowBlurRadius: Dp = 38.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    swipeEnabled: Boolean = false,
    onSwipePrevious: () -> Unit = {},
    onSwipeNext: () -> Unit = {},
) {
    val compactCornerRadius = DesignTokens.shapes.sm
    val artworkShape = remember(maxArtworkSize, cornerRadius, compactCornerRadius) {
        PlayerArtworkShape(
            expandedSize = maxArtworkSize,
            compactCornerRadius = compactCornerRadius,
            expandedCornerRadius = cornerRadius,
        )
    }
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        val artworkSize = minOf(maxWidth, maxHeight, maxArtworkSize)
        Box(
            modifier = Modifier
                .playerArtworkSharedElement()
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
                .clip(artworkShape)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = artworkShape,
                )
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.24f)),
        ) {
            ArtworkImage(
                modifier = Modifier.fillMaxSize(),
                artwork = artwork,
                contentScale = ContentScale.Crop,
                smoothTransition = true,
            )
        }
    }
}

private data class PlayerArtworkShape(
    val expandedSize: Dp,
    val compactCornerRadius: Dp,
    val expandedCornerRadius: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val compactSizePx = with(density) { CompactArtworkTransitionSize.toPx() }
        val expandedSizePx = with(density) { expandedSize.toPx() }
        val compactRadiusPx = with(density) { compactCornerRadius.toPx() }
        val expandedRadiusPx = with(density) { expandedCornerRadius.toPx() }
        val currentSizePx = minOf(size.width, size.height)
        val fraction = if (expandedSizePx <= compactSizePx) {
            1f
        } else {
            ((currentSizePx - compactSizePx) / (expandedSizePx - compactSizePx)).coerceIn(0f, 1f)
        }
        val radiusPx = compactRadiusPx + (expandedRadiusPx - compactRadiusPx) * fraction
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                cornerRadius = CornerRadius(radiusPx),
            ),
        )
    }
}

private val CompactArtworkTransitionSize = 44.dp

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
                fontFamily = titleFontFamily ?: DesignFontFamilies.Sans,
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
                    fontFamily = artistFontFamily ?: DesignFontFamilies.Sans,
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
                .sizeIn(maxWidth = buttonSize, maxHeight = buttonSize)
                .aspectRatio(1f)
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
    progressContent: @Composable (Long?) -> Unit,
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
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
            TrackRow(
                state = state,
                lyricDisplaySettings = lyricDisplaySettings,
                playerInteractionSettings = playerInteractionSettings,
                liked = liked,
                onLikedChange = onLikedChange,
                onAction = onAction,
                modifier = Modifier.padding(top = 10.dp),
                compact = false,
            )
            Spacer(modifier = Modifier.height(14.dp))
            progressContent(track?.durationMs)
            Spacer(modifier = Modifier.height(20.dp))
            CompactTransportPanel(
                nowPlayingState = state,
                onAction = onAction,
                dense = false,
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
                        DesignTextButton(
                            text = stringResource(Res.string.music_lyric_try_add_desc),
                            variant = DesignTextButtonVariant.Primary,
                            size = DesignTextButtonSize.Medium,
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
                LyricsView(
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
    LyricFontChoice.AppSans -> DesignFontFamilies.JakartaSans
    LyricFontChoice.AppCjk -> DesignFontFamilies.Sans
    LyricFontChoice.Monospace -> DesignFontFamilies.Mono
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
private fun TrackRow(
    state: NowPlayingState,
    lyricDisplaySettings: LyricDisplaySettings,
    playerInteractionSettings: PlayerInteractionSettings,
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
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
            compact = compact,
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
                LyricsView(
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
                    lineHorizontalPadding = if (dense) 0.dp else 8.dp,
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
        modifier = Modifier.padding(
            horizontal = if (dense) 0.dp else 8.dp,
            vertical = if (dense) 4.dp else 12.dp,
        ),
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

            TrackRow(
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
                CompactTransportPanel(
                    nowPlayingState = state,
                    onAction = onAction,
                    dense = false,
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
            Box(
                modifier = Modifier
                    .width(artworkColumnWidth)
                    .height(stageHeight)
                    .align(Alignment.CenterVertically)
                    .padding(start = 40.dp, end = 20.dp),
                contentAlignment = Alignment.CenterStart,
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(stageHeight)
                    .align(Alignment.CenterVertically)
                    .padding(start = 8.dp, end = 40.dp),
            ) {
                TrackRow(
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
                CompactTransportPanel(
                    nowPlayingState = state,
                    onAction = onAction,
                    dense = true,
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
    liked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    onAction: (NowPlayingAction) -> Unit,
) {
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
                onLikedChange = onLikedChange,
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
                onLikedChange = onLikedChange,
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
    progressContent: @Composable (Long?) -> Unit,
    compactProgressContent: @Composable (Long?) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTrack = state.currentTrack
    val titleBarInset = LocalDesktopTitleBarInset.current
    val density = LocalDensity.current
    val dragAnimationScope = rememberCoroutineScope()
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var dragAnimationJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxSize(),
    ) {
        ImmersivePlayerBackground(artwork = currentTrack?.artwork)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = titleBarInset)
                .graphicsLayer {
                    translationY = dragOffsetPx
                },
        ) {
            val viewportHeightPx = with(density) { maxHeight.toPx() }
            val dismissVelocityPxPerSecond =
                with(density) { NowPlayingDismissVelocityThreshold.toPx() }
            val indicatorDraggableState = rememberDraggableState { deltaPx ->
                dragOffsetPx = (dragOffsetPx + deltaPx).coerceIn(0f, viewportHeightPx)
            }

            if (maxWidth >= DesktopPlayerBreakpoint && maxHeight >= 520.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    MusicPlayerHeader(onAction = onAction)
                    DesktopNowPlayingLayout(
                        state = state,
                        lyricDisplaySettings = lyricDisplaySettings,
                        playerInteractionSettings = playerInteractionSettings,
                        currentPositionMs = currentPositionMs,
                        isSeeking = isSeeking,
                        progressContent = progressContent,
                        liked = isFavorite,
                        onLikedChange = { onToggleFavorite() },
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
                    liked = isFavorite,
                    onLikedChange = { onToggleFavorite() },
                    onAction = onAction,
                )
                NowPlayingDismissGestureArea(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .draggable(
                            state = indicatorDraggableState,
                            orientation = Orientation.Vertical,
                            onDragStarted = {
                                dragAnimationJob?.cancel()
                            },
                            onDragStopped = { velocityPxPerSecond ->
                                if (
                                    shouldDismissNowPlayingScreen(
                                        dragOffsetPx = dragOffsetPx,
                                        viewportHeightPx = viewportHeightPx,
                                        velocityPxPerSecond = velocityPxPerSecond,
                                        velocityThresholdPxPerSecond = dismissVelocityPxPerSecond,
                                    )
                                ) {
                                    onAction(NowPlayingAction.NavigateBack)
                                } else {
                                    dragAnimationJob?.cancel()
                                    dragAnimationJob = dragAnimationScope.launch {
                                        animate(
                                            initialValue = dragOffsetPx,
                                            targetValue = 0f,
                                            animationSpec = spring(),
                                        ) { value, _ ->
                                            dragOffsetPx = value
                                        }
                                    }
                                }
                            },
                        ),
                )
            }
        }
    }
}

internal fun shouldDismissNowPlayingScreen(
    dragOffsetPx: Float,
    viewportHeightPx: Float,
    velocityPxPerSecond: Float,
    velocityThresholdPxPerSecond: Float,
): Boolean =
    viewportHeightPx > 0f &&
        (
            dragOffsetPx >= viewportHeightPx * NowPlayingDismissDistanceFraction ||
                velocityPxPerSecond >= velocityThresholdPxPerSecond
        )

@Composable
fun ImmersivePlayerBackground(artwork: Artwork?) {
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
            smoothTransition = true,
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
