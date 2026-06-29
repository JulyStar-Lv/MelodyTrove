package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.DomainLyrics
import com.github.tidetunes.core.domain.repository.LyricsRepository
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao

class LyricsRepositoryImpl(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
) : LyricsRepository {

    override suspend fun loadLyrics(trackId: Long): DomainLyrics {
        val track = trackDao.findByIds(listOf(trackId)).firstOrNull()
        val lyrics = metadataDao.getLyrics(trackId)
        val artistNames = metadataDao.artistNamesForTrack(trackId)

        val lines = lyrics?.content
            ?.lines()
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        return DomainLyrics(
            trackTitle = track?.title ?: "Unknown Track",
            trackArtist = artistNames.joinToString(", ").ifBlank { track?.artist },
            lines = lines,
            format = lyrics?.format,
            synchronized = lyrics?.synchronized ?: false,
        )
    }
}
