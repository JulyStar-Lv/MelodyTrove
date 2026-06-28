package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.DomainTrackBrowserItem

interface TrackBrowserRepository {
    suspend fun findTracksByGenre(genre: String, limit: Int): List<DomainTrackBrowserItem>
    suspend fun findRecentlyAdded(limit: Int): List<DomainTrackBrowserItem>
    suspend fun findRecentlyPlayed(limit: Int): List<DomainTrackBrowserItem>
}
