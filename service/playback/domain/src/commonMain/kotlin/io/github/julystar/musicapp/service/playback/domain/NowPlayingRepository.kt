package io.github.julystar.musicapp.service.playback.domain

import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.CurrentTrackInfo
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
