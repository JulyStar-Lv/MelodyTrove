package com.github.tidetunes.service.playback.presentation.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.service.playback.domain.SleepModeLeftTime
import com.github.tidetunes.service.playback.presentation.PlayerVM
import com.github.tidetunes.service.playback.presentation.miniplayer.IdleMiniPlayer
import com.github.tidetunes.service.playback.presentation.miniplayer.MiniPlayer
import com.github.tidetunes.service.playback.presentation.sleep.SleepModeVM
import com.github.tidetunes.service.playback.presentation.sleep.TimeToPauseModal
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun rememberIsPlaybackPlaying(
    playerVM: PlayerVM = koinViewModel(),
): Boolean {
    val isPlaying by playerVM.playing.collectAsState()
    return isPlaying
}

@Composable
fun rememberHasPlaybackItem(
    playerVM: PlayerVM = koinViewModel(),
): Boolean {
    val playbackState by playerVM.playbackState.collectAsState()
    return playbackState.currentItem != null
}

@Composable
fun PlaybackMiniPlayerHost(
    onOpenNowPlaying: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onOpenQueue: () -> Unit,
    playerVM: PlayerVM = koinViewModel(),
) {
    val playbackState by playerVM.playbackState.collectAsState()
    if (playbackState.currentItem != null) {
        MiniPlayer(
            onOpenNowPlaying = onOpenNowPlaying,
            onOpenQueue = onOpenQueue,
            playerVM = playerVM,
        )
    } else {
        IdleMiniPlayer(onBrowseLibrary = onBrowseLibrary)
    }
}

@Composable
fun rememberOpenSleepTimer(
    sleepModeVM: SleepModeVM = koinViewModel(),
): (SleepModeLeftTime) -> Unit = sleepModeVM::openModal

@Composable
fun PlaybackSleepTimerHost(
    sleepModeVM: SleepModeVM = koinViewModel(),
) {
    TimeToPauseModal(sleepModeVM = sleepModeVM)
}
