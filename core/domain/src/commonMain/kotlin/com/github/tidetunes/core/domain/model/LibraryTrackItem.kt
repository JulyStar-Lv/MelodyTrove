package com.github.tidetunes.core.domain.model

/**
 * Immutable presentation model for a library track row.
 * Mapped from [com.github.tidetunes.database.TrackEntity] at the data boundary.
 */
data class LibraryTrackItem(
    val id: Long,
    val title: String,
    val artist: String?,
    val durationMs: Long?,
    val mediaId: MediaId? = null,
)
