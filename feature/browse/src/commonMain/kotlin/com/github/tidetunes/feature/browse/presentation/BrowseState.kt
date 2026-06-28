package com.github.tidetunes.feature.browse.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.Artwork
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class BrowseState(
    val isLoading: Boolean = true,
    val albums: ImmutableList<BrowseAlbumItem> = persistentListOf(),
    val artists: ImmutableList<BrowseArtistItem> = persistentListOf(),
    val genres: ImmutableList<String> = persistentListOf(),
    val error: String? = null,
)

@Immutable
data class BrowseAlbumItem(
    val id: Long,
    val name: String,
    val year: Int?,
    val artwork: Artwork?,
    val trackCount: Int,
)

@Immutable
data class BrowseArtistItem(
    val id: Long,
    val name: String,
    val trackCount: Int,
)
