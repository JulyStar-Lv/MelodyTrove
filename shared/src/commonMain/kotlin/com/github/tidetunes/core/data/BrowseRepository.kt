package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.BrowseAlbumPreview
import com.github.tidetunes.core.domain.model.BrowseArtistPreview
import com.github.tidetunes.core.domain.repository.BrowseRepository
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao

class BrowseRepositoryImpl(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
) : BrowseRepository {
    override suspend fun loadAlbums(limit: Int): List<BrowseAlbumPreview> {
        return metadataDao.listAlbumsWithTracks(limit).map { album ->
            val tracks = trackDao.findByAlbumId(album.id)
            BrowseAlbumPreview(
                id = album.id,
                name = album.name,
                year = album.year,
                artworkTrackId = tracks.firstOrNull()?.id,
                trackCount = trackDao.countTracksByAlbumId(album.id),
            )
        }
    }

    override suspend fun loadArtists(limit: Int): List<BrowseArtistPreview> {
        return metadataDao.listArtistsWithTracks(limit).map { artist ->
            BrowseArtistPreview(
                id = artist.id,
                name = artist.name,
                trackCount = trackDao.countTracksByArtistId(artist.id),
            )
        }
    }

    override suspend fun loadGenreNames(limit: Int): List<String> {
        return metadataDao.listGenreNames(limit)
    }
}
