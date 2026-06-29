package com.github.tidetunes.core.domain.model

data class DomainArtistAlbum(
    val id: Long,
    val name: String?,
    val year: Int?,
    val firstTrackId: Long?,
)

data class DomainArtistDetail(
    val name: String?,
    val albums: List<DomainArtistAlbum>,
    val tracks: List<DomainTrackBrowserItem>,
)
