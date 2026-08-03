package io.github.julystar.musicapp.core.domain.model

data class DomainPlaylistTrack(
    val trackId: Long,
    val title: String,
    val artist: String? = null,
    val albumName: String? = null,
    val durationMs: Long?,
    val sortOrder: Long,
    val sourceStorageId: Long?,
    val sourcePath: String?,
    val mediaId: MediaId? = null,
)
