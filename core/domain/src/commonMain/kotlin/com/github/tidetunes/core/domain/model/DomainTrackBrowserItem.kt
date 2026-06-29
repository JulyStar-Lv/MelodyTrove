package com.github.tidetunes.core.domain.model

data class DomainTrackBrowserItem(
    val id: Long,
    val title: String,
    val artist: String?,
    val albumName: String?,
    val durationMs: Long?,
    val mediaId: MediaId?,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val albumId: Long? = null,
    val canDownload: Boolean,
)
