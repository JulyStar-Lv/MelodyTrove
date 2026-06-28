package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.BrowseAlbumPreview
import com.github.tidetunes.core.domain.model.BrowseArtistPreview

interface BrowseRepository {
    suspend fun loadAlbums(limit: Int): List<BrowseAlbumPreview>
    suspend fun loadArtists(limit: Int): List<BrowseArtistPreview>
    suspend fun loadGenreNames(limit: Int): List<String>
}
