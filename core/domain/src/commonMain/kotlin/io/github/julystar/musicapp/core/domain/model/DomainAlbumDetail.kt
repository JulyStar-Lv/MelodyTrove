package io.github.julystar.musicapp.core.domain.model

data class DomainAlbumDetail(
    val albumTitle: String,
    val albumArtist: String?,
    val tracks: List<DomainTrackBrowserItem>,
    val year: Int? = null,
    val genre: String? = null,
)
