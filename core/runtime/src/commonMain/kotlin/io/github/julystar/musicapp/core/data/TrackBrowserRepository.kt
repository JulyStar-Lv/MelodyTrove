package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.repository.TrackBrowserRepository
import io.github.julystar.musicapp.database.MetadataDao
import io.github.julystar.musicapp.database.TrackDao

class TrackBrowserRepositoryImpl(
    private val trackDao: TrackDao,
    private val metadataDao: MetadataDao,
) : TrackBrowserRepository {

    override suspend fun findTracksByGenre(
        genre: String,
        limit: Int,
    ): List<DomainTrackBrowserItem> {
        return trackDao.findTracksByGenre(genre, limit).map { it.toBrowserItem() }
    }

    override suspend fun findRecentlyAdded(limit: Int): List<DomainTrackBrowserItem> {
        return trackDao.findRecentlyAdded(limit).map { it.toBrowserItem() }
    }

    override suspend fun findRecentlyPlayed(limit: Int): List<DomainTrackBrowserItem> {
        return trackDao.findRecentlyPlayed(limit).map { it.toBrowserItem() }
    }

    private suspend fun io.github.julystar.musicapp.database.TrackEntity.toBrowserItem(): DomainTrackBrowserItem {
        val albumName = albumId?.let { metadataDao.getAlbum(it)?.name }
        return DomainTrackBrowserItem(
            id = id,
            title = title,
            artist = artist,
            albumName = albumName,
            durationMs = durationMs,
            trackNumber = trackNumber,
            discNumber = discNumber,
            mediaId = null,
            albumId = albumId,
            canDownload = false,
        )
    }
}
