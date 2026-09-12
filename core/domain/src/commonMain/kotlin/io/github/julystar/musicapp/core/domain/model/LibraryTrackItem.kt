package io.github.julystar.musicapp.core.domain.model

/**
 * Immutable presentation model for a library track row.
 * Mapped from [io.github.julystar.musicapp.database.TrackEntity] at the data boundary.
 */
data class LibraryTrackItem(
    val id: Long,
    val title: String,
    val artist: String?,
    val durationMs: Long?,
    val mediaId: MediaId? = null,
    val albumName: String? = null,
    val albumId: Long? = null,
    val codec: String? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val createdAt: Long? = null,
    val lastPlayedAt: Long? = null,
)
