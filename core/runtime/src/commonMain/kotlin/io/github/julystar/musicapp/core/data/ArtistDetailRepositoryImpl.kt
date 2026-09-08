package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.DomainArtistAlbum
import io.github.julystar.musicapp.core.domain.model.DomainArtistDetail
import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.repository.ArtistDetailRepository
import io.github.julystar.musicapp.database.MetadataDao
import io.github.julystar.musicapp.database.TrackDao

class ArtistDetailRepositoryImpl(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
) : ArtistDetailRepository {

    override suspend fun loadArtistDetail(artistId: Long): DomainArtistDetail {
        val artist = metadataDao.getArtist(artistId)
        val albums = metadataDao.albumsByArtistId(artistId)
        val tracks = trackDao.findTracksByArtistId(artistId)

        val albumItems = albums.map { album ->
            val firstTrackId = tracks.firstOrNull { track -> track.albumId == album.id }?.id
            DomainArtistAlbum(
                id = album.id,
                name = album.name,
                year = album.year,
                firstTrackId = firstTrackId,
            )
        }

        val trackItems = tracks.map { track ->
            val albumName = albums.find { it.id == track.albumId }?.name
            DomainTrackBrowserItem(
                id = track.id,
                title = track.title,
                artist = track.artist,
                albumName = albumName,
                durationMs = track.durationMs,
                trackNumber = track.trackNumber,
                discNumber = track.discNumber,
                mediaId = null,
                albumId = track.albumId,
                canDownload = false,
            )
        }

        return DomainArtistDetail(
            name = artist?.name,
            albums = albumItems,
            tracks = trackItems,
        )
    }
}
