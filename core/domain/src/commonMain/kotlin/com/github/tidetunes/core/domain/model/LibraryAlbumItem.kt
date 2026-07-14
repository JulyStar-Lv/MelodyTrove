package com.github.tidetunes.core.domain.model

data class LibraryAlbumItem(
    val id: Long,
    val name: String,
    val year: Int?,
)

data class LibraryArtistItem(
    val id: Long,
    val name: String,
)
