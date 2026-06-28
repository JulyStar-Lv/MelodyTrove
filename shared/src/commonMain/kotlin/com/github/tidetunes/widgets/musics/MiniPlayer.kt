package com.github.tidetunes.widgets.musics

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import tidetunes.shared.generated.resources.Res
import tidetunes.shared.generated.resources.icon_pause
import tidetunes.shared.generated.resources.icon_play
import tidetunes.shared.generated.resources.icon_play_next
import tidetunes.shared.generated.resources.icon_stop
import com.github.tidetunes.core.presentation.components.appPainterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.toArtwork
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import com.github.tidetunes.core.presentation.components.MusicCover
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.viewmodels.PlayerVM
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.RouteMusicPlayer
import com.github.tidetunes.core.utils.formatDuration

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
        verticalAlignment = Alignment.CenterVertically
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = TextStyle(fontSize = 16.sp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Box(modifier = Modifier.width(16.dp))
                Row(
                    modifier = Modifier.wrapContentWidth()
                ) {
                    if (!isPlaying) {
                        TideTunesIconButton(
                            sizeType = TideTunesIconButtonSize.Medium,
                            buttonType = TideTunesIconButtonType.Default,
                            disabled = loading,
                            painter = appPainterResource(Res.drawable.icon_play),
                            onClick = onPlay,
                        )
                    } else {
                        TideTunesIconButton(
                            sizeType = TideTunesIconButtonSize.Medium,
                            buttonType = TideTunesIconButtonType.Default,
                            painter = appPainterResource(Res.drawable.icon_pause),
                            onClick = onPause,
                        )
                    }
                    TideTunesIconButton(
                        sizeType = TideTunesIconButtonSize.Medium,
                        buttonType = TideTunesIconButtonType.Default,
                        painter = appPainterResource(Res.drawable.icon_play_next),
                        disabled = !canNext,
                        onClick = onNext,
                    )
                    TideTunesIconButton(
                        sizeType = TideTunesIconButtonSize.Medium,
                        buttonType = TideTunesIconButtonType.Default,
                        painter = appPainterResource(Res.drawable.icon_stop),
                        onClick = onStop,
                    )
                }
            }
            Box(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = {
                        if (totalDurationMS == 0uL) {
                            0f
                        } else {
                            currentDurationMS.toFloat() / totalDurationMS.toFloat()
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurface,
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
    playerVM: PlayerVM = koinViewModel()
) {
    val navController = LocalNavController.current
    val playbackState by playerVM.playbackState.collectAsState()
    val playbackPosition by playerVM.playbackPosition.collectAsState()
    val music by playerVM.music.collectAsState()
    val nextMusic by playerVM.nextMusic.collectAsState()
    val durationMs = playbackPosition.durationMs.takeIf { it > 0 }
        ?: music?.meta?.duration?.inWholeMilliseconds
        ?: 0

    MiniPlayerCore(
        isPlaying = playbackState.status == PlaybackStatus.Playing,
        title = music?.meta?.title ?: "",
        cover = music?.cover?.toArtwork(),
        currentDurationMS = playbackPosition.positionMs.coerceAtLeast(0).toULong(),
        totalDuration = formatDuration(music),
        totalDurationMS = durationMs.coerceAtLeast(0).toULong(),
        canNext = nextMusic != null,
        loading = playbackState.status == PlaybackStatus.Loading,
        onClick = { navController.navigate(RouteMusicPlayer()) },
        onPlay = { playerVM.resume() },
        onPause = { playerVM.pause() },
        onStop = { playerVM.stop() },
        onNext = { playerVM.playNext() },
    )
}
