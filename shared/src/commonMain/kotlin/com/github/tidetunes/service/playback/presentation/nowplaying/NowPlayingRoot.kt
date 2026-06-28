package com.github.tidetunes.service.playback.presentation.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import com.github.tidetunes.viewmodels.PlayerVM
import com.github.tidetunes.feature.dashboard.presentation.SleepModeVM
import com.github.tidetunes.core.presentation.media.ArtworkPalette
import com.github.tidetunes.core.presentation.media.rememberArtworkPalette
import com.github.tidetunes.core.domain.model.LyricLine
import org.koin.compose.viewmodel.koinViewModel


/**
 * Computes the playback position within the current lyric line.
 * Subtracts accumulated duration of all lines before [lyricIndex]
 * from [currentDuration] and clamps to the line"s duration.
 */
private fun computeLinePosition(
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    currentDuration: Duration,
): Duration {
    if (lyrics.isEmpty() || lyricIndex < 0 || lyricIndex >= lyrics.size) {
        return Duration.ZERO
    }
    val lineStartMs = lyrics.take(lyricIndex).sumOf { it.duration.inWholeMilliseconds }
    val positionMs = currentDuration.inWholeMilliseconds - lineStartMs
    val lineDurationMs = lyrics[lyricIndex].duration.inWholeMilliseconds
    val clampedMs = positionMs.coerceIn(0, lineDurationMs)
    return clampedMs.milliseconds
}



@Composable
fun NowPlayingRoot(
    onNavigateBack: () -> Unit,
    onNavigateToLyricImport: () -> Unit,
    playerViewModel: PlayerVM = koinViewModel(),
    sleepModeViewModel: SleepModeVM = koinViewModel(),
) {
    val state by playerViewModel.nowPlayingState.collectAsState()
    val palette = rememberArtworkPalette(artwork = state.currentTrack?.artwork)
    val lyricIndex by playerViewModel.lyricIndex.collectAsState()
    val sleepModeState by sleepModeViewModel.state.collectAsState()

    // Compute within-line position for word-by-word lyric animation
    val currentDuration by playerViewModel.currentDuration.collectAsState()
    val linePositionMs = remember(lyricIndex, currentDuration) {
        val lyrics = state.currentTrack?.lyrics?.lines ?: emptyList()
        computeLinePosition(lyrics, lyricIndex, currentDuration)
    }

    LaunchedEffect(playerViewModel) {
        playerViewModel.nowPlayingEvents.collect { event ->
            when (event) {
                is NowPlayingEvent.ShowMessage -> Unit
            }
        }
    }

    fun onAction(action: NowPlayingAction) {
        when (action) {
            NowPlayingAction.NavigateBack -> onNavigateBack()
            NowPlayingAction.AddLyric -> {
                if (state.currentTrack != null) {
                    onNavigateToLyricImport()
                }
            }
            NowPlayingAction.OpenSleepTimer -> sleepModeViewModel.openModal()
            else -> playerViewModel.onNowPlayingAction(action)
        }
    }

    NowPlayingScreen(
        state = state,
        palette = palette,
        lyricIndex = lyricIndex,
        linePositionMs = linePositionMs,
        isSleepTimerEnabled = sleepModeState.enabled,
        progressContent = { trackDurationMs ->
            NowPlayingProgressRoot(
                trackDurationMs = trackDurationMs,
                playerViewModel = playerViewModel,
                onAction = ::onAction,
            )
        },
        onAction = ::onAction,
    )
}

@Composable
private fun NowPlayingProgressRoot(
    trackDurationMs: Long?,
    playerViewModel: PlayerVM,
    onAction: (NowPlayingAction) -> Unit,
) {
    val currentDuration by playerViewModel.currentDuration.collectAsState()
    val bufferDuration by playerViewModel.bufferDuration.collectAsState()
    val playerDuration by playerViewModel.playerDuration.collectAsState()

    NowPlayingProgressPanel(
        progressState = NowPlayingProgressState(
            currentDuration = currentDuration,
            bufferDuration = bufferDuration,
            playerDuration = playerDuration,
        ),
        trackDurationMs = trackDurationMs,
        onAction = onAction,
    )
}
