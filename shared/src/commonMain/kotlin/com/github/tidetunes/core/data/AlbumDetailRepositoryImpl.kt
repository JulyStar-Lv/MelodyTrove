package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.DomainAlbumDetail
import com.github.tidetunes.core.domain.model.DomainTrackBrowserItem
import com.github.tidetunes.core.domain.repository.AlbumDetailRepository
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao

class AlbumDetailRepositoryImpl(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
) : AlbumDetailRepository {

    override suspend fun loadAlbumDetail(albumId: Long): DomainAlbumDetail {
        val album = metadataDao.getAlbum(albumId)
        val tracks = trackDao.findByAlbumId(albumId)
        val artist = metadataDao.artistNamesForAlbum(albumId).joinToString(", ")

        return DomainAlbumDetail(
            albumTitle = album?.name ?: "Unknown Album",
            albumArtist = artist.ifBlank { album?.name },
            tracks = tracks.map { track ->
                DomainTrackBrowserItem(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    albumName = album?.name,
                    durationMs = track.durationMs,
                    trackNumber = track.trackNumber,
                    discNumber = track.discNumber,
                    mediaId = null,
                    albumId = track.albumId,
                    canDownload = false,
                )
            },
        )
    }
}
