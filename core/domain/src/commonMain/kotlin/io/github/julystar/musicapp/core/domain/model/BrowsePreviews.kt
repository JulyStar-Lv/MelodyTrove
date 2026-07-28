package io.github.julystar.musicapp.core.domain.model

data class BrowseAlbumPreview(
    val id: Long,
    val name: String,
    val year: Int?,
    val artworkTrackId: Long?,
    val trackCount: Int,
)

data class BrowseArtistPreview(
    val id: Long,
    val name: String,
    val trackCount: Int,
)
