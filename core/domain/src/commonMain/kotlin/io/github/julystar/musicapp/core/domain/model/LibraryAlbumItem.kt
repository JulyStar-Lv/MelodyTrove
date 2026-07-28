package io.github.julystar.musicapp.core.domain.model

data class LibraryAlbumItem(
    val id: Long,
    val name: String,
    val year: Int?,
)

data class LibraryArtistItem(
    val id: Long,
    val name: String,
)
