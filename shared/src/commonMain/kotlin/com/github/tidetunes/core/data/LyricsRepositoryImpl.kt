package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.DomainLyrics
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.filterLyricTextBlock
import com.github.tidetunes.core.domain.repository.LyricsRepository
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.domain.importing.TrackMetadataPrefetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class LyricsRepositoryImpl(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
    private val metadataPrefetcher: TrackMetadataPrefetcher,
    private val settingsRepository: SettingsRepository,
) : LyricsRepository {

    override suspend fun loadLyrics(trackId: Long): DomainLyrics {
        val track = trackDao.findByIds(listOf(trackId)).firstOrNull()
        var candidates = metadataDao.getLyricsCandidates(trackId)
        if (candidates.isEmpty()) {
            try {
                metadataPrefetcher.prefetch(trackId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Metadata prefetch is best effort and should not block the lyrics screen.
            }
            candidates = metadataDao.getLyricsCandidates(trackId)
        }
        val settings = settingsRepository.settings.first()
        val lyrics = candidates.selectLyrics(settings.lyrics)
        val artistNames = metadataDao.artistNamesForTrack(trackId)

        val lines = lyrics?.content
            ?.let(settings.lyrics::filterLyricTextBlock)
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
