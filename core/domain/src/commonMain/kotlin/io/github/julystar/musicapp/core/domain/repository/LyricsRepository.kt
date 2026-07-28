package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.DomainLyrics

interface LyricsRepository {
    suspend fun loadLyrics(trackId: Long): DomainLyrics
}
