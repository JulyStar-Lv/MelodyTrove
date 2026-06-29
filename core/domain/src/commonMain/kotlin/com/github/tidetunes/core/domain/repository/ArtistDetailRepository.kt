package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.DomainArtistDetail

interface ArtistDetailRepository {
    suspend fun loadArtistDetail(artistId: Long): DomainArtistDetail
}
