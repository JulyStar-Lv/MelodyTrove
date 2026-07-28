package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.DomainArtistDetail

interface ArtistDetailRepository {
    suspend fun loadArtistDetail(artistId: Long): DomainArtistDetail
}
