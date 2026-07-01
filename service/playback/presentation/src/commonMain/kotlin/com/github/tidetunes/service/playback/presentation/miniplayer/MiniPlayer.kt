package com.github.tidetunes.service.playback.presentation.miniplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.presentation.components.MusicCover
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import com.github.tidetunes.core.utils.formatDuration
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.presentation.PlayerVM
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import tidetunes.service.playback.presentation.generated.resources.Res
import tidetunes.service.playback.presentation.generated.resources.icon_pause
import tidetunes.service.playback.presentation.generated.resources.icon_play
import tidetunes.service.playback.presentation.generated.resources.icon_play_next
import tidetunes.service.playback.presentation.generated.resources.icon_stop
import kotlin.time.Duration.Companion.milliseconds

@Composable
private fun MiniPlayerCore(
    isPlaying: Boolean,
    title: String,
    cover: Artwork?,
    currentDurationMS: ULong,
    totalDuration: String,
    totalDurationMS: ULong,
    loading: Boolean,
    canNext: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable { onClick() }.fillMaxWidth().padding(30.dp).height(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicCover(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .size(60.dp),
            artwork = cover,
        )
        Box(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = TextStyle(fontSize = 16.sp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Box(modifier = Modifier.width(16.dp))
                Row(
                    modifier = Modifier.wrapContentWidth(),
                ) {
                    if (!isPlaying) {
                        TideTunesIconButton(
                            sizeType = TideTunesIconButtonSize.Medium,
                            buttonType = TideTunesIconButtonType.Default,
                            disabled = loading,
                            painter = painterResource(Res.drawable.icon_play),
                            onClick = onPlay,
                        )
                    } else {
                        TideTunesIconButton(
                            sizeType = TideTunesIconButtonSize.Medium,
                            buttonType = TideTunesIconButtonType.Default,
                            painter = painterResource(Res.drawable.icon_pause),
                            onClick = onPause,
                        )
                    }
                    TideTunesIconButton(
                        sizeType = TideTunesIconButtonSize.Medium,
                        buttonType = TideTunesIconButtonType.Default,
                        painter = painterResource(Res.drawable.icon_play_next),
                        disabled = !canNext,
                        onClick = onNext,
                    )
                    TideTunesIconButton(
                        sizeType = TideTunesIconButtonSize.Medium,
                        buttonType = TideTunesIconButtonType.Default,
                        painter = painterResource(Res.drawable.icon_stop),
                        onClick = onStop,
                    )
                }
            }
            Box(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .fillMaxWidth(),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress =
                        if (totalDurationMS == 0uL) {
                            0f
                        } else {
                            currentDurationMS.toFloat() / totalDurationMS.toFloat()
                        },
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = MiuixTheme.colorScheme.onSurface,
                    ),
                )
            }
            Text(
                text = totalDuration,
                fontSize = 9.sp,
            )
        }
    }
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
        cover = currentTrack?.artwork,
        currentDurationMS = playbackPosition.positionMs.coerceAtLeast(0).toULong(),
        totalDuration = formatDuration(durationMs.takeIf { it > 0 }?.milliseconds),
        totalDurationMS = durationMs.coerceAtLeast(0).toULong(),
        canNext = nowPlayingState.queue.canPlayNext,
        loading = playbackState.status == PlaybackStatus.Loading,
        onClick = onOpenNowPlaying,
        onPlay = { playerVM.resume() },
        onPause = { playerVM.pause() },
        onStop = { playerVM.stop() },
        onNext = { playerVM.playNext() },
    )
}
