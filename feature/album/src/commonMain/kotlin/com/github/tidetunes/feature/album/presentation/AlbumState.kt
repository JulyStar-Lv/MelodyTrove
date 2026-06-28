package com.github.tidetunes.feature.album.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.MediaId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AlbumState(
    val isLoading: Boolean = true,
    val albumId: Long = 0,
    val title: String = "",
    val artist: String = "",
    val artwork: Artwork? = null,
    val tracks: ImmutableList<AlbumTrackItem> = persistentListOf(),
    val error: String? = null,
)

@Immutable
data class AlbumTrackItem(
    val id: Long,
    val title: String,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long?,
    val mediaId: MediaId?,
    val canDownload: Boolean,
)
