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
        durationMs = durationMs,
        artwork = artwork,
        lyrics = lyrics,
        mediaId = null, // resolved at the data layer; kept for future source-level tracking
    )
}

public fun PlaybackQueue.toNowPlayingQueueState(
    previousArtwork: Artwork?,
    nextArtwork: Artwork?,
): NowPlayingQueueState {
    return NowPlayingQueueState(
        currentIndex = currentIndex,
        itemCount = items.size,
        canPlayPrevious = previousArtwork != null,
        canPlayNext = nextArtwork != null,
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
