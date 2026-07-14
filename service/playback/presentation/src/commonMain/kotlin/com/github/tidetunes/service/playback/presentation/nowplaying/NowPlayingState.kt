package com.github.tidetunes.service.playback.presentation.nowplaying

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.Lyrics
import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.service.playback.domain.RepeatMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class NowPlayingState(
    val currentTrack: NowPlayingTrackItem? = null,
    val queue: NowPlayingQueueState = NowPlayingQueueState(),
    val controls: NowPlayingControlsState = NowPlayingControlsState(),
)

@Immutable
data class NowPlayingTrackItem(
    val id: Long,
    val title: String,
    val artist: String? = null,
    val durationMs: Long?,
    val artwork: Artwork?,
    val lyrics: Lyrics = Lyrics(),
    val mediaId: MediaId?,
) {
    val canDownload: Boolean
        get() = mediaId != null

    val hasLyric: Boolean
        get() = lyrics.hasLyric
}

@Immutable
data class NowPlayingQueueState(
    val currentIndex: Int = -1,
    val itemCount: Int = 0,
    val canPlayPrevious: Boolean = false,
    val canPlayNext: Boolean = false,
    val previousArtwork: Artwork? = null,
    val nextArtwork: Artwork? = null,
)

@Immutable
data class NowPlayingControlsState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val shuffleEnabled: Boolean = false,
)

@Immutable
data class NowPlayingProgressState(
    val currentDuration: Duration = 0.milliseconds,
    val bufferDuration: Duration = 0.milliseconds,
    val playerDuration: Duration = 0.milliseconds,
    val lyricIndex: Int = -1,
)

sealed interface NowPlayingAction {
    data object NavigateBack : NowPlayingAction
    data object AddLyric : NowPlayingAction
    data object SearchMetadata : NowPlayingAction
    data object RemoveLyric : NowPlayingAction
    data object RemoveCurrentTrack : NowPlayingAction
    data object DownloadCurrentTrack : NowPlayingAction
    data object OpenSleepTimer : NowPlayingAction
    data object PlayPrevious : NowPlayingAction
    data object PlayNext : NowPlayingAction
    data object Resume : NowPlayingAction
    data object Pause : NowPlayingAction
    data object CycleRepeatMode : NowPlayingAction
    data class SeekTo(val positionMs: ULong) : NowPlayingAction
}

sealed interface NowPlayingEvent {
    data class ShowMessage(val message: String) : NowPlayingEvent
}
