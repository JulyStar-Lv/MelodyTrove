package com.github.tidetunes.service.playback.presentation.miniplayer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.repository.FavoritesRepository
import com.github.tidetunes.core.presentation.components.MusicCover
import com.github.tidetunes.core.presentation.components.TideCompactMiniPlayerBar
import com.github.tidetunes.core.presentation.components.TideExpandedMiniPlayerBar
import com.github.tidetunes.core.presentation.components.TideGradientPlayButton
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonColors
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.components.TideMiniPlayerBar
import com.github.tidetunes.core.presentation.components.TidePlayerControlSize
import com.github.tidetunes.core.presentation.platform.isDesktopPlatform
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.domain.RepeatMode
import com.github.tidetunes.service.playback.presentation.PlayerVM
import com.github.tidetunes.service.playback.presentation.transition.playerArtworkSharedElement
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import tidetunes.service.playback.presentation.generated.resources.Res
import tidetunes.service.playback.presentation.generated.resources.icon_heart_compact
import tidetunes.service.playback.presentation.generated.resources.icon_heart_compact_filled
import tidetunes.service.playback.presentation.generated.resources.icon_pause
import tidetunes.service.playback.presentation.generated.resources.icon_play
import tidetunes.service.playback.presentation.generated.resources.icon_play_next
import tidetunes.service.playback.presentation.generated.resources.icon_play_previous
import tidetunes.service.playback.presentation.generated.resources.icon_transport_queue
import tidetunes.service.playback.presentation.generated.resources.icon_transport_repeat
import tidetunes.service.playback.presentation.generated.resources.icon_transport_repeat_one
import tidetunes.service.playback.presentation.generated.resources.icon_transport_shuffle
import tidetunes.service.playback.presentation.generated.resources.player_add_favorite
import tidetunes.service.playback.presentation.generated.resources.player_choose_from_library
import tidetunes.service.playback.presentation.generated.resources.player_list_repeat
import tidetunes.service.playback.presentation.generated.resources.player_next_track
import tidetunes.service.playback.presentation.generated.resources.player_nothing_playing
import tidetunes.service.playback.presentation.generated.resources.player_pause
import tidetunes.service.playback.presentation.generated.resources.player_play
import tidetunes.service.playback.presentation.generated.resources.player_previous_track
import tidetunes.service.playback.presentation.generated.resources.player_queue
import tidetunes.service.playback.presentation.generated.resources.player_remove_favorite
import tidetunes.service.playback.presentation.generated.resources.player_repeat
import tidetunes.service.playback.presentation.generated.resources.player_shuffle
import tidetunes.service.playback.presentation.generated.resources.player_single_repeat
import tidetunes.service.playback.presentation.generated.resources.player_unknown_artist
import tidetunes.service.playback.presentation.generated.resources.now_playing_title
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun MiniPlayerCore(
    isPlaying: Boolean,
    title: String,
    subtitle: String,
    cover: Artwork?,
    currentDurationMS: ULong,
    totalDurationMS: ULong,
    loading: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    playbackAvailable: Boolean = true,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val progress = playbackProgress(
        currentDurationMS = currentDurationMS,
        totalDurationMS = totalDurationMS,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (isDesktopPlatform() || maxWidth >= 840.dp) {
            ExpandedMiniPlayerBar(
                isPlaying = isPlaying,
                title = title,
                subtitle = subtitle,
                cover = cover,
                progress = progress,
                loading = loading,
                canPrevious = canPrevious,
                canNext = canNext,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                isFavorite = isFavorite,
                playbackAvailable = playbackAvailable,
                onClick = onClick,
                onPlay = onPlay,
                onPause = onPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleFavorite = onToggleFavorite,
                onCyclePlaybackMode = onCyclePlaybackMode,
                onOpenQueue = onOpenQueue,
            )
        } else if (maxWidth < 140.dp) {
            CompactMiniPlayer(
                isPlaying = isPlaying,
                cover = cover,
                progress = progress,
                loading = loading,
                onClick = onClick,
                onPlay = onPlay,
                onPause = onPause,
            )
        } else {
            MiniPlayerBar(
                isPlaying = isPlaying,
                title = title,
                subtitle = subtitle,
                cover = cover,
                progress = progress,
                loading = loading,
                canPrevious = canPrevious,
                canNext = canNext,
                showMobilePortraitActions = maxWidth < 600.dp,
                isFavorite = isFavorite,
                playbackAvailable = playbackAvailable,
                onClick = onClick,
                onPlay = onPlay,
                onPause = onPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleFavorite = onToggleFavorite,
                onOpenQueue = onOpenQueue,
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    isPlaying: Boolean,
    title: String,
    subtitle: String,
    cover: Artwork?,
    progress: Float,
    loading: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    showMobilePortraitActions: Boolean,
    isFavorite: Boolean,
    playbackAvailable: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes
    val actionTint = MiuixTheme.colorScheme.onSurface

    TideMiniPlayerBar(
        title = title,
        subtitle = subtitle,
        progress = progress,
        onClick = onClick,
        artwork = {
            MusicCover(
                modifier = Modifier
                    .playerArtworkSharedElement()
                    .size(44.dp)
                    .clip(RoundedCornerShape(shapes.sm)),
                artwork = cover,
            )
        },
        controls = {
            if (showMobilePortraitActions) {
                MiniPlayerIconButton(
                    painter = painterResource(
                        if (isFavorite) {
                            Res.drawable.icon_heart_compact_filled
                        } else {
                            Res.drawable.icon_heart_compact
                        },
                    ),
                    contentDescription = stringResource(
                        if (isFavorite) Res.string.player_remove_favorite else Res.string.player_add_favorite,
                    ),
                    tint = actionTint,
                    enabled = playbackAvailable,
                    onClick = onToggleFavorite,
                )
                MiniPlayerIconButton(
                    painter = painterResource(if (isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play),
                    contentDescription = stringResource(
                        if (isPlaying) Res.string.player_pause else Res.string.player_play,
                    ),
                    tint = actionTint,
                    enabled = !loading,
                    emphasized = true,
                    onClick = if (isPlaying) onPause else onPlay,
                )
                MiniPlayerIconButton(
                    painter = painterResource(Res.drawable.icon_transport_queue),
                    contentDescription = stringResource(Res.string.player_queue),
                    tint = actionTint,
                    enabled = playbackAvailable,
                    onClick = onOpenQueue,
                )
            } else {
                MiniPlayerIconButton(
                    painter = painterResource(if (isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play),
                    contentDescription = stringResource(
                        if (isPlaying) Res.string.player_pause else Res.string.player_play
                    ),
                    tint = actionTint,
                    enabled = !loading,
                    emphasized = true,
                    onClick = if (isPlaying) onPause else onPlay,
                )
                MiniPlayerIconButton(
                    painter = painterResource(Res.drawable.icon_play_next),
                    contentDescription = stringResource(Res.string.player_next_track),
                    tint = actionTint,
                    enabled = canNext,
                    onClick = onNext,
                )
            }
        },
    )
}

@Composable
private fun ExpandedMiniPlayerBar(
    isPlaying: Boolean,
    title: String,
    subtitle: String,
    cover: Artwork?,
    progress: Float,
    loading: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    playbackAvailable: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes
    val actionTint = MiuixTheme.colorScheme.onSurface
    val playbackModePainter = painterResource(
        when {
            shuffleEnabled -> Res.drawable.icon_transport_shuffle
            repeatMode == RepeatMode.One -> Res.drawable.icon_transport_repeat_one
            else -> Res.drawable.icon_transport_repeat
        },
    )
    val playbackModeDescription = stringResource(
        when {
            shuffleEnabled -> Res.string.player_shuffle
            repeatMode == RepeatMode.One -> Res.string.player_single_repeat
            repeatMode == RepeatMode.All -> Res.string.player_list_repeat
            else -> Res.string.player_repeat
        },
    )

    TideExpandedMiniPlayerBar(
        title = title,
        subtitle = subtitle,
        progress = progress,
        onClick = onClick,
        artwork = {
            MusicCover(
                modifier = Modifier
                    .playerArtworkSharedElement()
                    .size(46.dp)
                    .clip(RoundedCornerShape(shapes.sm)),
                artwork = cover,
            )
        },
        actions = {
            MiniPlayerIconButton(
                painter = painterResource(
                    if (isFavorite) {
                        Res.drawable.icon_heart_compact_filled
                    } else {
                        Res.drawable.icon_heart_compact
                    },
                ),
                contentDescription = stringResource(
                    if (isFavorite) Res.string.player_remove_favorite else Res.string.player_add_favorite,
                ),
                tint = actionTint,
                enabled = playbackAvailable,
                onClick = onToggleFavorite,
            )
            MiniPlayerIconButton(
                painter = playbackModePainter,
                contentDescription = playbackModeDescription,
                tint = actionTint,
                enabled = playbackAvailable,
                onClick = onCyclePlaybackMode,
            )
            MiniPlayerIconButton(
                painter = painterResource(Res.drawable.icon_play_previous),
                contentDescription = stringResource(Res.string.player_previous_track),
                tint = actionTint,
                enabled = canPrevious,
                onClick = onPrevious,
            )
            MiniPlayerIconButton(
                painter = painterResource(if (isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play),
                contentDescription = stringResource(
                    if (isPlaying) Res.string.player_pause else Res.string.player_play,
                ),
                tint = actionTint,
                enabled = !loading,
                emphasized = true,
                onClick = if (isPlaying) onPause else onPlay,
            )
            MiniPlayerIconButton(
                painter = painterResource(Res.drawable.icon_play_next),
                contentDescription = stringResource(Res.string.player_next_track),
                tint = actionTint,
                enabled = canNext,
                onClick = onNext,
            )
            MiniPlayerIconButton(
                painter = painterResource(Res.drawable.icon_transport_queue),
                contentDescription = stringResource(Res.string.player_queue),
                tint = actionTint,
                enabled = playbackAvailable,
                onClick = onOpenQueue,
            )
        },
    )
}

@Composable
private fun MiniPlayerIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    TideIconButton(
        modifier = Modifier.width(if (emphasized) 48.dp else 44.dp),
        size = if (emphasized) TideIconButtonSize.Touch else TideIconButtonSize.Medium,
        variant = TideIconButtonVariant.Default,
        painter = painter,
        onClick = onClick,
        contentDescription = contentDescription,
        colors = TideIconButtonColors(iconTint = tint),
        enabled = enabled,
        showClickIndication = false,
    )
}

@Composable
private fun CompactMiniPlayer(
    isPlaying: Boolean,
    cover: Artwork?,
    progress: Float,
    loading: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    TideCompactMiniPlayerBar(
        progress = progress,
        accessibilityLabel = stringResource(Res.string.now_playing_title),
        onClick = onClick,
        artwork = {
            MusicCover(
                modifier = Modifier
                    .playerArtworkSharedElement()
                    .size(40.dp)
                    .clip(RoundedCornerShape(shapes.md)),
                artwork = cover,
            )
        },
        overlayControls = {
            TideGradientPlayButton(
                painter = painterResource(if (isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play),
                enabled = !loading,
                size = TidePlayerControlSize.Mini,
                contentDescription = stringResource(
                    if (isPlaying) Res.string.player_pause else Res.string.player_play
                ),
                showClickIndication = false,
                onClick = if (isPlaying) onPause else onPlay,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp),
            )
        },
    )
}

private fun playbackProgress(
    currentDurationMS: ULong,
    totalDurationMS: ULong,
): Float {
    if (totalDurationMS == 0uL) return 0f
    return (currentDurationMS.toFloat() / totalDurationMS.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun MiniPlayer(
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
    playerVM: PlayerVM = koinViewModel(),
    favoritesRepository: FavoritesRepository = koinInject(),
) {
    val playbackState by playerVM.playbackState.collectAsState()
    val playbackPosition by playerVM.playbackPosition.collectAsState()
    val nowPlayingState by playerVM.nowPlayingState.collectAsState()
    val currentTrack = nowPlayingState.currentTrack
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    val coroutineScope = rememberCoroutineScope()
    val isFavorite = currentTrack?.id?.let(favoriteTrackIds::contains) == true
    val durationMs = playbackPosition.durationMs.takeIf { it > 0 }
        ?: currentTrack?.durationMs
        ?: 0

    MiniPlayerCore(
        isPlaying = playbackState.status == PlaybackStatus.Playing,
        title = currentTrack?.title ?: playbackState.currentItem?.title ?: "",
        subtitle = currentTrack?.artist
            ?.takeIf { it.isNotBlank() }
            ?: playbackState.currentItem?.artist?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.player_unknown_artist),
        cover = currentTrack?.artwork,
        currentDurationMS = playbackPosition.positionMs.coerceAtLeast(0).toULong(),
        totalDurationMS = durationMs.coerceAtLeast(0).toULong(),
        canPrevious = nowPlayingState.queue.canPlayPrevious,
        canNext = nowPlayingState.queue.canPlayNext,
        repeatMode = nowPlayingState.controls.repeatMode,
        shuffleEnabled = nowPlayingState.controls.shuffleEnabled,
        isFavorite = isFavorite,
        loading = playbackState.status == PlaybackStatus.Loading,
        onClick = onOpenNowPlaying,
        onPlay = { playerVM.resume() },
        onPause = { playerVM.pause() },
        onPrevious = { playerVM.playPrevious() },
        onNext = { playerVM.playNext() },
        onToggleFavorite = {
            currentTrack?.id?.let { trackId ->
                coroutineScope.launch { favoritesRepository.toggleFavorite(trackId) }
            }
        },
        onCyclePlaybackMode = { playerVM.changePlayModeToNext() },
        onOpenQueue = onOpenQueue,
    )
}

@Composable
fun IdleMiniPlayer(
    onBrowseLibrary: () -> Unit,
) {
    MiniPlayerCore(
        isPlaying = false,
        title = stringResource(Res.string.player_nothing_playing),
        subtitle = stringResource(Res.string.player_choose_from_library),
        cover = null,
        currentDurationMS = 0uL,
        totalDurationMS = 0uL,
        loading = true,
        canPrevious = false,
        canNext = false,
        repeatMode = RepeatMode.Off,
        shuffleEnabled = false,
        isFavorite = false,
        playbackAvailable = false,
        onClick = onBrowseLibrary,
        onPlay = {},
        onPause = {},
        onPrevious = {},
        onNext = {},
        onToggleFavorite = {},
        onCyclePlaybackMode = {},
        onOpenQueue = {},
    )
}
