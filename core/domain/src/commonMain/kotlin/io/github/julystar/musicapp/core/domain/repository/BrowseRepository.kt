package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.BrowseAlbumPreview
import io.github.julystar.musicapp.core.domain.model.BrowseArtistPreview

interface BrowseRepository {
    suspend fun loadAlbums(limit: Int): List<BrowseAlbumPreview>
    suspend fun loadArtists(limit: Int): List<BrowseArtistPreview>
    suspend fun loadGenreNames(limit: Int): List<String>
}
