package com.github.tidetunes.core.domain.model

data class DomainPlaylistTrack(
    val trackId: Long,
    val title: String,
    val durationMs: Long?,
    val sortOrder: Long,
    val sourceStorageId: Long?,
    val sourcePath: String?,
)
