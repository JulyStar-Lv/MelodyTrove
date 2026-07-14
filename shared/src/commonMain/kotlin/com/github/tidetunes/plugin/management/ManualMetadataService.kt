package com.github.tidetunes.plugin.management

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.github.tidetunes.database.AlbumEntity
import com.github.tidetunes.database.ArtistEntity
import com.github.tidetunes.database.LyricsEntity
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TrackArtistCrossRef
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.plugin.runtime.PluginLookupMode
import com.github.tidetunes.service.playback.data.PlayerRepository
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingTrackItem
import com.github.tidetunes.source.api.MetaLyrics
import com.github.tidetunes.source.api.MetaSongCandidate
import com.github.tidetunes.source.api.MetaSongQuery

class ManualMetadataService(
    private val lookup: MetadataLookupUseCase,
    private val database: TideTunesDatabase,
    private val trackDao: TrackDao,
    private val metadataDao: MetadataDao,
    private val playerRepository: PlayerRepository,
) {
    suspend fun search(
        track: NowPlayingTrackItem,
        keyword: String,
    ): MetadataLookupCollection<MetaSongCandidate> = lookup.searchSongs(
        query = MetaSongQuery(
            title = track.title,
            artist = track.artist,
            durationMs = track.durationMs,
            keyword = keyword.trim().takeIf(String::isNotEmpty),
        ),
        mode = PluginLookupMode.MANUAL,
    )

    suspend fun apply(
        trackId: Long,
        candidate: MetaSongCandidate,
    ): List<MetadataLookupFailure> {
        val lyricsResult = lookup.getLyrics(candidate, PluginLookupMode.MANUAL)
        val now = currentTimeMillis()
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val current = trackDao.get(trackId) ?: error("Current track is no longer in the library")
                val albumId = candidate.album
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { resolveAlbum(it, candidate.date) }
                    ?: current.albumId
                trackDao.upsertAll(
                    listOf(
                        current.copy(
                            title = candidate.title.trim().ifEmpty { current.title },
                            albumId = albumId,
                            artist = candidate.artist?.trim()?.takeIf(String::isNotEmpty) ?: current.artist,
                            trackNumber = candidate.trackNumber?.substringBefore('/')?.trim()?.toIntOrNull()
                                ?: current.trackNumber,
                            date = candidate.date?.trim()?.takeIf(String::isNotEmpty) ?: current.date,
                            year = candidate.date?.take(4)?.toIntOrNull() ?: current.year,
                            updatedAt = now,
                        ),
                    ),
                )
                candidate.artist?.let { replaceArtists(trackId, it) }
                lyricsResult.value?.toEntity(trackId, now)?.let { lyrics ->
                    metadataDao.upsertLyrics(listOf(lyrics))
                }
            }
        }
        playerRepository.refreshCurrentMetadata()
        return lyricsResult.failures
    }

    private suspend fun resolveAlbum(name: String, date: String?): Long {
        val normalized = normalizeName(name)
        metadataDao.insertAlbums(
            listOf(
                AlbumEntity(
                    name = name,
                    normalizedName = normalized,
                    sortName = null,
                    year = date?.take(4)?.toIntOrNull(),
                    artworkId = null,
                ),
            ),
        )
        return metadataDao.findAlbumsByNormalizedNames(listOf(normalized)).single().id
    }

    private suspend fun replaceArtists(trackId: Long, rawArtists: String) {
        val names = rawArtists.split('/')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy(::normalizeName)
        if (names.isEmpty()) return
        metadataDao.insertArtists(
            names.map { name ->
                ArtistEntity(
                    name = name,
                    normalizedName = normalizeName(name),
                    sortName = null,
                )
            },
        )
        val artists = metadataDao.findArtistsByNormalizedNames(names.map(::normalizeName))
            .associateBy(ArtistEntity::normalizedName)
        metadataDao.deleteTrackArtistsForTracks(listOf(trackId))
        metadataDao.upsertTrackArtists(
            names.mapIndexedNotNull { index, name ->
                artists[normalizeName(name)]?.let { artist ->
                    TrackArtistCrossRef(trackId = trackId, artistId = artist.id, position = index)
                }
            },
        )
    }
}

internal fun MetaLyrics.toEntity(trackId: Long, updatedAt: Long): LyricsEntity? {
    val raw = listOfNotNull(
        rawPlainLrc,
        rawVerbatimLrc,
        rawEnhancedLrc,
        rawMultiPersonEnhancedLrc,
    ).firstOrNull(String::isNotBlank)
    val generated = lines.takeIf { it.isNotEmpty() }?.joinToString("\n") { line ->
        val text = line.text.trim()
        line.startMs?.let { startMs -> "${startMs.toLrcTimestamp()}$text" } ?: text
    }
    val content = raw ?: generated ?: return null
    val synchronized = raw != null || lines.any { it.startMs != null }
    return LyricsEntity(
        trackId = trackId,
        format = if (synchronized) "LRC" else "TEXT",
        language = null,
        synchronized = synchronized,
        content = content,
        sourcePath = null,
        updatedAt = updatedAt,
    )
}

private fun Long.toLrcTimestamp(): String {
    val safe = coerceAtLeast(0)
    val minutes = safe / 60_000
    val seconds = (safe % 60_000) / 1_000
    val hundredths = (safe % 1_000) / 10
    return "[${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}." +
        "${hundredths.toString().padStart(2, '0')}]"
}

private fun normalizeName(value: String): String = value.trim().lowercase()
