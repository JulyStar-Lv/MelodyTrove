package com.github.tidetunes.service.playback.presentation.nowplaying

import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.CurrentTrackInfo
import com.github.tidetunes.service.playback.domain.PlaybackQueue
import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.domain.PlayerState

public fun CurrentTrackInfo.toNowPlayingTrackItem(): NowPlayingTrackItem {
    return NowPlayingTrackItem(
        id = id,
        title = title,
        artist = this.artist?.takeIf { it.isNotBlank() },
        durationMs = durationMs,
        artwork = artwork,
        lyrics = lyrics,
        mediaId = mediaId,
        annotation = annotation?.takeIf(String::isNotBlank),
    )
}

public fun PlaybackQueue.toNowPlayingQueueState(
    previousArtwork: Artwork?,
    nextArtwork: Artwork?,
    canPlayPrevious: Boolean = previousArtwork != null,
    canPlayNext: Boolean = nextArtwork != null,
): NowPlayingQueueState {
    return NowPlayingQueueState(
        currentIndex = currentIndex,
        itemCount = items.size,
        canPlayPrevious = canPlayPrevious,
        canPlayNext = canPlayNext,
        previousArtwork = previousArtwork,
        nextArtwork = nextArtwork,
    )
}

public fun PlayerState.toNowPlayingControlsState(): NowPlayingControlsState {
    return NowPlayingControlsState(
        isPlaying = status == PlaybackStatus.Playing,
        isLoading = status == PlaybackStatus.Loading,
        repeatMode = repeatMode,
        shuffleEnabled = shuffleEnabled,
    )
}
