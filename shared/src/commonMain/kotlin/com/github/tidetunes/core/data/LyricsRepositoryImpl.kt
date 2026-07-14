package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.DomainLyrics
import com.github.tidetunes.core.domain.repository.LyricsRepository
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.domain.importing.TrackMetadataPrefetcher
import kotlinx.coroutines.CancellationException

class LyricsRepositoryImpl(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
    private val metadataPrefetcher: TrackMetadataPrefetcher,
) : LyricsRepository {

    override suspend fun loadLyrics(trackId: Long): DomainLyrics {
        val track = trackDao.findByIds(listOf(trackId)).firstOrNull()
        var lyrics = metadataDao.getLyrics(trackId)
        if (lyrics == null) {
            try {
                metadataPrefetcher.prefetch(trackId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Metadata prefetch is best effort and should not block the lyrics screen.
            }
            lyrics = metadataDao.getLyrics(trackId)
        }
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
