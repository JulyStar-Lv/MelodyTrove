package com.github.tidetunes.service.playback.domain

import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.CurrentTrackInfo
import kotlinx.coroutines.flow.StateFlow

interface NowPlayingRepository {
    val currentTrackInfo: StateFlow<CurrentTrackInfo?>
    val previousArtwork: StateFlow<Artwork?>
    val nextArtwork: StateFlow<Artwork?>
    val canPlayPrevious: StateFlow<Boolean>
    val canPlayNext: StateFlow<Boolean>

    fun removeCurrentTrack()
    fun removeCurrentLyrics()
}
