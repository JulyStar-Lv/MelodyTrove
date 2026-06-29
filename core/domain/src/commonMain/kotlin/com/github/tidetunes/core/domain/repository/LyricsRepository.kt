package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.DomainLyrics

interface LyricsRepository {
    suspend fun loadLyrics(trackId: Long): DomainLyrics
}
