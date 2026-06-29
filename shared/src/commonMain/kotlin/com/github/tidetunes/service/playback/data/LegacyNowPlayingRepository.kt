package com.github.tidetunes.service.playback.data

import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.CurrentTrackInfo
import com.github.tidetunes.service.playback.domain.NowPlayingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LegacyNowPlayingRepository(
    private val playerRepository: PlayerRepository,
    scope: CoroutineScope,
) : NowPlayingRepository {
    override val currentTrackInfo: StateFlow<CurrentTrackInfo?> = playerRepository.currentTrackInfo
    override val previousArtwork: StateFlow<Artwork?> = playerRepository.previousArtwork
    override val nextArtwork: StateFlow<Artwork?> = playerRepository.nextArtwork
    override val canPlayPrevious: StateFlow<Boolean> = playerRepository.previousMusic
        .map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)
    override val canPlayNext: StateFlow<Boolean> = playerRepository.nextMusic
        .map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override fun removeCurrentTrack() {
        playerRepository.remove()
    }

    override fun removeCurrentLyrics() {
        playerRepository.removeLyric()
    }
}
