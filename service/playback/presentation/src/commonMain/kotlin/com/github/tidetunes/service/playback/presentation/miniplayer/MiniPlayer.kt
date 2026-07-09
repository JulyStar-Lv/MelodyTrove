package com.github.tidetunes.service.playback.presentation.miniplayer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.presentation.components.MusicCover
import com.github.tidetunes.core.presentation.components.TideCompactMiniPlayerBar
import com.github.tidetunes.core.presentation.components.TideGradientPlayButton
import com.github.tidetunes.core.presentation.components.TideMiniPlayerBar
import com.github.tidetunes.core.presentation.components.TidePlayerControlButton
import com.github.tidetunes.core.presentation.components.TidePlayerControlSize
import com.github.tidetunes.core.presentation.components.TidePlayerControlVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.core.utils.formatDuration
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.presentation.PlayerVM
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import tidetunes.service.playback.presentation.generated.resources.Res
import tidetunes.service.playback.presentation.generated.resources.icon_pause
import tidetunes.service.playback.presentation.generated.resources.icon_play
import tidetunes.service.playback.presentation.generated.resources.icon_play_next
import tidetunes.service.playback.presentation.generated.resources.icon_play_previous
import kotlin.time.Duration.Companion.milliseconds

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
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val progress = playbackProgress(
        currentDurationMS = currentDurationMS,
        totalDurationMS = totalDurationMS,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 140.dp) {
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
                onClick = onClick,
                onPlay = onPlay,
                onPause = onPause,
                onPrevious = onPrevious,
                onNext = onNext,
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
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    TideMiniPlayerBar(
        title = title,
        subtitle = subtitle,
        progress = progress,
        onClick = onClick,
        artwork = {
            MusicCover(
                modifier = Modifier
                    .clip(RoundedCornerShape(shapes.md))
                    .size(40.dp),
                artwork = cover,
            )
        },
        controls = {
            TidePlayerControlButton(
                painter = painterResource(Res.drawable.icon_play_previous),
                enabled = canPrevious,
                size = TidePlayerControlSize.Mini,
                variant = TidePlayerControlVariant.Secondary,
                contentDescription = "Previous track",
                onClick = onPrevious,
            )
            TideGradientPlayButton(
                painter = painterResource(if (isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play),
                enabled = !loading,
                size = TidePlayerControlSize.Mini,
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = if (isPlaying) onPause else onPlay,
            )
            TidePlayerControlButton(
                painter = painterResource(Res.drawable.icon_play_next),
                enabled = canNext,
                size = TidePlayerControlSize.Mini,
                variant = TidePlayerControlVariant.Secondary,
                contentDescription = "Next track",
                onClick = onNext,
            )
        },
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
        onClick = onClick,
        artwork = {
            MusicCover(
                modifier = Modifier
                    .clip(RoundedCornerShape(shapes.md))
                    .size(40.dp),
                artwork = cover,
            )
        },
        overlayControls = {
            TideGradientPlayButton(
                painter = painterResource(if (isPlaying) Res.drawable.icon_pause else Res.drawable.icon_play),
                enabled = !loading,
                size = TidePlayerControlSize.Mini,
                contentDescription = if (isPlaying) "Pause" else "Play",
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
    playerVM: PlayerVM = koinViewModel(),
) {
    val playbackState by playerVM.playbackState.collectAsState()
    val playbackPosition by playerVM.playbackPosition.collectAsState()
    val nowPlayingState by playerVM.nowPlayingState.collectAsState()
    val currentTrack = nowPlayingState.currentTrack
    val durationMs = playbackPosition.durationMs.takeIf { it > 0 }
        ?: currentTrack?.durationMs
        ?: 0

    MiniPlayerCore(
        isPlaying = playbackState.status == PlaybackStatus.Playing,
        title = currentTrack?.title ?: playbackState.currentItem?.title ?: "",
        subtitle = miniPlayerSubtitle(
            queueCount = nowPlayingState.queue.itemCount,
            durationText = formatDuration(durationMs.takeIf { it > 0 }?.milliseconds),
        ),
        cover = currentTrack?.artwork,
        currentDurationMS = playbackPosition.positionMs.coerceAtLeast(0).toULong(),
        totalDurationMS = durationMs.coerceAtLeast(0).toULong(),
        canPrevious = nowPlayingState.queue.canPlayPrevious,
        canNext = nowPlayingState.queue.canPlayNext,
        loading = playbackState.status == PlaybackStatus.Loading,
        onClick = onOpenNowPlaying,
        onPlay = { playerVM.resume() },
        onPause = { playerVM.pause() },
        onPrevious = { playerVM.playPrevious() },
        onNext = { playerVM.playNext() },
    )
}

private fun miniPlayerSubtitle(
    queueCount: Int,
    durationText: String,
): String {
    val duration = durationText.takeIf { it.isNotBlank() } ?: "--:--"
    return if (queueCount > 1) {
        "$queueCount tracks - $duration"
    } else {
        duration
    }
}
