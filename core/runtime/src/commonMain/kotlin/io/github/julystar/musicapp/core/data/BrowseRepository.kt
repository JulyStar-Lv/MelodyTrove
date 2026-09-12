package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.BrowseAlbumPreview
import io.github.julystar.musicapp.core.domain.model.BrowseArtistPreview
import io.github.julystar.musicapp.core.domain.repository.BrowseRepository
import io.github.julystar.musicapp.database.MetadataDao
import io.github.julystar.musicapp.database.TrackDao

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
