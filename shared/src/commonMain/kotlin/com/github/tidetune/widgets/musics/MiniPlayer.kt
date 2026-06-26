package com.github.tidetune.widgets.musics

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
import tidetune.shared.generated.resources.Res
import tidetune.shared.generated.resources.icon_pause
import tidetune.shared.generated.resources.icon_play
import tidetune.shared.generated.resources.icon_play_next
import tidetune.shared.generated.resources.icon_stop
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.github.tidetune.components.TideTuneIconButton
import com.github.tidetune.components.TideTuneIconButtonSize
import com.github.tidetune.components.TideTuneIconButtonType
import com.github.tidetune.components.MusicCover
import com.github.tidetune.viewmodels.PlayerVM
import com.github.tidetune.core.LocalNavController
import com.github.tidetune.core.RouteMusicPlayer
import com.github.tidetune.utils.formatDuration
import com.github.tidetune.utils.toMusicDurationMs
import uniffi.tidetune_core.DataSourceKey

@Composable
private fun MiniPlayerCore(
    isPlaying: Boolean,
    title: String,
    cover: DataSourceKey?,
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
            coverDataSourceKey = cover,
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
                        TideTuneIconButton(
                            sizeType = TideTuneIconButtonSize.Medium,
                            buttonType = TideTuneIconButtonType.Default,
                            disabled = loading,
                            painter = painterResource(Res.drawable.icon_play),
                            onClick = onPlay,
                        )
                    } else {
                        TideTuneIconButton(
                            sizeType = TideTuneIconButtonSize.Medium,
                            buttonType = TideTuneIconButtonType.Default,
                            painter = painterResource(Res.drawable.icon_pause),
                            onClick = onPause,
                        )
                    }
                    TideTuneIconButton(
                        sizeType = TideTuneIconButtonSize.Medium,
                        buttonType = TideTuneIconButtonType.Default,
                        painter = painterResource(Res.drawable.icon_play_next),
                        disabled = !canNext,
                        onClick = onNext,
                    )
                    TideTuneIconButton(
                        sizeType = TideTuneIconButtonSize.Medium,
                        buttonType = TideTuneIconButtonType.Default,
                        painter = painterResource(Res.drawable.icon_stop),
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
    val isPlaying by playerVM.playing.collectAsState()
    val music by playerVM.music.collectAsState()
    val loading by playerVM.loading.collectAsState()
    val nextMusic by playerVM.nextMusic.collectAsState()
    val currentDuration by playerVM.currentDuration.collectAsState()

    MiniPlayerCore(
        isPlaying = isPlaying,
        title = music?.meta?.title ?: "",
        cover = music?.cover,
        currentDurationMS = toMusicDurationMs(currentDuration),
        totalDuration = formatDuration(music),
        totalDurationMS = toMusicDurationMs(music),
        canNext = nextMusic != null,
        loading = loading,
        onClick = { navController.navigate(RouteMusicPlayer()) },
        onPlay = { playerVM.resume() },
        onPause = { playerVM.pause() },
        onStop = { playerVM.stop() },
        onNext = { playerVM.playNext() },
    )
}

