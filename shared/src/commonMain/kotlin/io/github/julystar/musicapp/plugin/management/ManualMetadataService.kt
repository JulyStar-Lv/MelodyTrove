package io.github.julystar.musicapp.plugin.management

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.julystar.musicapp.core.domain.model.MetadataScanMode
import io.github.julystar.musicapp.core.domain.model.toOptions
import io.github.julystar.musicapp.database.AlbumArtistCrossRef
import io.github.julystar.musicapp.database.AlbumEntity
import io.github.julystar.musicapp.database.ArtistEntity
import io.github.julystar.musicapp.database.GenreEntity
import io.github.julystar.musicapp.database.LyricsEntity
import io.github.julystar.musicapp.database.MetadataDao
import io.github.julystar.musicapp.database.AppDatabase
import io.github.julystar.musicapp.database.TrackArtistCrossRef
import io.github.julystar.musicapp.database.TrackDao
import io.github.julystar.musicapp.database.TrackGenreCrossRef
import io.github.julystar.musicapp.database.TrackMetadataSources
import io.github.julystar.musicapp.domain.importing.buildTrackEntity
import io.github.julystar.musicapp.domain.importing.toStorageEntry
import io.github.julystar.musicapp.platform.currentTimeMillis
import io.github.julystar.musicapp.plugin.runtime.PluginLookupMode
import io.github.julystar.musicapp.service.playback.data.PlayerRepository
import io.github.julystar.musicapp.service.playback.presentation.nowplaying.NowPlayingTrackItem
import io.github.julystar.musicapp.source.api.MetaLyrics
import io.github.julystar.musicapp.source.api.MetaLyricLine
import io.github.julystar.musicapp.source.api.MetaLyricWord
import io.github.julystar.musicapp.source.api.MetaSongCandidate
import io.github.julystar.musicapp.source.api.MetaSongQuery
import io.github.julystar.musicapp.source.storage.RemoteMetadataReader
import uniffi.app_backend.RemoteMetadata

class ManualMetadataService(
    private val lookup: MetadataLookupUseCase,
    private val database: AppDatabase,
    private val trackDao: TrackDao,
    private val metadataDao: MetadataDao,
    private val playerRepository: PlayerRepository,
    private val metadataReader: RemoteMetadataReader,
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
            pageSize = MANUAL_METADATA_RESULTS_PER_SOURCE,
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
                    ?.let { name ->
                        metadataDao.resolveManualMetadataAlbum(
                            name = name,
                            date = candidate.date,
                            currentAlbumId = current.albumId,
                        )
                    }
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
                            metadataSource = TrackMetadataSources.Plugin,
                            metadataLocked = true,
                            metadataSourceId = candidate.sourceId,
                            metadataExternalId = candidate.id,
                            metadataAppliedAt = now,
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

    suspend fun resetFromFile(trackId: Long) {
        val candidate = database.trackSourceRefDao().metadataResetCandidateForTrack(trackId)
            ?: error("No available music file can be used to reset metadata")
        val sourceItem = database.sourceItemDao().get(candidate.sourceItemId)
            ?: error("The selected music file is no longer in the library")
        val entry = candidate.toStorageEntry()
        val metadata = metadataReader.read(entry, MetadataScanMode.Fast.toOptions())
            ?: error("Failed to read metadata from the music file")
        val now = currentTimeMillis()

        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val current = trackDao.get(trackId)
                    ?: error("Current track is no longer in the library")
                val albumId = metadata.album
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { name ->
                        metadataDao.resolveManualMetadataAlbum(
                            name = name,
                            date = metadata.date,
                            currentAlbumId = current.albumId,
                        )
                    }
                trackDao.upsertAll(
                    listOf(
                        buildTrackEntity(
                            entry = entry,
                            metadata = metadata,
                            sourceItem = sourceItem,
                            now = now,
                            existingTrack = current,
                            albumId = albumId,
                            respectMetadataLock = false,
                        ),
                    ),
                )
                replaceTrackArtists(trackId, metadata.trackArtistNames())
                replaceTrackGenre(trackId, metadata.genre)
                albumId?.let { replaceAlbumArtist(it, metadata.albumArtist) }
            }
        }
        playerRepository.refreshCurrentMetadata()
    }

    private suspend fun replaceArtists(trackId: Long, rawArtists: String) {
        replaceTrackArtists(
            trackId = trackId,
            names = rawArtists.split('/')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy(::normalizeName),
        )
    }

    private suspend fun replaceTrackArtists(trackId: Long, names: List<String>) {
        metadataDao.deleteTrackArtistsForTracks(listOf(trackId))
        val normalizedNames = names
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy(::normalizeName)
        if (normalizedNames.isEmpty()) return
        metadataDao.insertArtists(
            normalizedNames.map { name ->
                ArtistEntity(
                    name = name,
                    normalizedName = normalizeName(name),
                    sortName = null,
                )
            },
        )
        val artists = metadataDao.findArtistsByNormalizedNames(normalizedNames.map(::normalizeName))
            .associateBy(ArtistEntity::normalizedName)
        metadataDao.upsertTrackArtists(
            normalizedNames.mapIndexedNotNull { index, name ->
                artists[normalizeName(name)]?.let { artist ->
                    TrackArtistCrossRef(trackId = trackId, artistId = artist.id, position = index)
                }
            },
        )
    }

    private suspend fun replaceTrackGenre(trackId: Long, rawGenre: String?) {
        metadataDao.deleteTrackGenresForTracks(listOf(trackId))
        val name = rawGenre?.trim()?.takeIf(String::isNotEmpty) ?: return
        val normalized = normalizeName(name)
        metadataDao.insertGenres(listOf(GenreEntity(name = name, normalizedName = normalized)))
        val genre = metadataDao.findGenresByNormalizedNames(listOf(normalized)).single()
        metadataDao.upsertTrackGenres(listOf(TrackGenreCrossRef(trackId, genre.id)))
    }

    private suspend fun replaceAlbumArtist(albumId: Long, rawArtist: String?) {
        metadataDao.deleteAlbumArtistsForAlbums(listOf(albumId))
        val name = rawArtist?.trim()?.takeIf(String::isNotEmpty) ?: return
        val normalized = normalizeName(name)
        metadataDao.insertArtists(
            listOf(ArtistEntity(name = name, normalizedName = normalized, sortName = null)),
        )
        val artist = metadataDao.findArtistsByNormalizedNames(listOf(normalized)).single()
        metadataDao.upsertAlbumArtists(listOf(AlbumArtistCrossRef(albumId, artist.id, position = 0)))
    }
}

private const val MANUAL_METADATA_RESULTS_PER_SOURCE = 3

internal suspend fun MetadataDao.resolveManualMetadataAlbum(
    name: String,
    date: String?,
    currentAlbumId: Long?,
): Long {
    val normalized = normalizeName(name)
    val preservedArtworkId = currentAlbumId
        ?.let { albumId -> getArtworkForAlbum(albumId)?.id }
    insertAlbums(
        listOf(
            AlbumEntity(
                name = name,
                normalizedName = normalized,
                sortName = null,
                year = date?.take(4)?.toIntOrNull(),
                artworkId = preservedArtworkId,
            ),
        ),
    )
    val album = findAlbumsByNormalizedNames(listOf(normalized)).single()
    if (
        preservedArtworkId != null &&
        album.artworkId == null &&
        getArtworkForAlbum(album.id) == null
    ) {
        upsertAlbums(listOf(album.copy(artworkId = preservedArtworkId)))
    }
    return album.id
}

internal fun MetaLyrics.toEntity(trackId: Long, updatedAt: Long): LyricsEntity? {
    val wordTimed = listOfNotNull(
        rawTtml.toPayload("TTML", wordTimed = true),
        rawMultiPersonEnhancedLrc.toPayload("LRC", wordTimed = true),
        rawEnhancedLrc.toPayload("LRC", wordTimed = true),
        rawVerbatimLrc.toPayload("LRC", wordTimed = true),
        lines.toEnhancedLrcOrNull()?.let { content ->
            PersistedLyricPayload("LRC", content, wordTimed = true)
        },
    ).firstOrNull()
    val plain = rawPlainLrc.toPayload("LRC")
    val generated = lines.takeIf { it.isNotEmpty() }?.joinToString("\n") { line ->
        val text = line.text.trim()
        line.startMs?.let { startMs -> "${startMs.toLrcTimestamp()}$text" } ?: text
    }
    val payload = wordTimed
        ?: plain
        ?: generated?.let { content ->
            PersistedLyricPayload(
                format = if (lines.any { it.startMs != null }) "LRC" else "TEXT",
                content = content,
                wordTimed = false,
            )
        }
        ?: return null
    val synchronized = payload.format != "TEXT"
    return LyricsEntity(
        trackId = trackId,
        format = payload.format,
        language = null,
        synchronized = synchronized,
        content = payload.content,
        sourcePath = "external:plugin",
        updatedAt = updatedAt,
        sourceKind = when {
            payload.format == "TTML" -> "ExternalTtml"
            payload.wordTimed -> "ExternalWordTimed"
            else -> "ExternalPlain"
        },
    )
}

private data class PersistedLyricPayload(
    val format: String,
    val content: String,
    val wordTimed: Boolean,
)

private fun String?.toPayload(
    format: String,
    wordTimed: Boolean = false,
): PersistedLyricPayload? = this
    ?.takeIf(String::isNotBlank)
    ?.let { content -> PersistedLyricPayload(format, content, wordTimed) }

private fun List<MetaLyricLine>.toEnhancedLrcOrNull(): String? {
    if (none { line -> line.words.any { word -> word.startMs != null } }) return null

    return mapIndexedNotNull { index, line ->
        val lineStart = line.startMs ?: return@mapIndexedNotNull null
        val timedWords = line.words.mapNotNull { word -> word.toAbsoluteTiming(lineStart) }
        if (timedWords.size != line.words.size || timedWords.isEmpty()) {
            return@mapIndexedNotNull "${lineStart.toLrcTimestamp()}${line.text.trim()}"
        }
        val lineEnd = line.endMs
            ?: timedWords.last().endMs
            ?: getOrNull(index + 1)?.startMs
            ?: (timedWords.last().startMs + 1)
        buildString {
            append(lineStart.toLrcTimestamp())
            timedWords.forEach { word ->
                append('<')
                append(word.startMs.toEnhancedLrcTimestamp())
                append('>')
                append(word.text)
            }
            append('<')
            append(lineEnd.coerceAtLeast(timedWords.last().startMs + 1).toEnhancedLrcTimestamp())
            append('>')
        }
    }.joinToString("\n").takeIf(String::isNotBlank)
}

private data class TimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long?,
)

private fun MetaLyricWord.toAbsoluteTiming(lineStartMs: Long): TimedWord? {
    val start = startMs ?: return null
    return TimedWord(
        text = text,
        startMs = start.toAbsoluteTime(lineStartMs),
        endMs = endMs?.toAbsoluteTime(lineStartMs),
    )
}

private fun Long.toAbsoluteTime(lineStartMs: Long): Long =
    if (this < lineStartMs) lineStartMs + this else this

private fun Long.toLrcTimestamp(): String {
    val safe = coerceAtLeast(0)
    val minutes = safe / 60_000
    val seconds = (safe % 60_000) / 1_000
    val hundredths = (safe % 1_000) / 10
    return "[${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}." +
        "${hundredths.toString().padStart(2, '0')}]"
}

private fun Long.toEnhancedLrcTimestamp(): String {
    val safe = coerceAtLeast(0)
    val minutes = safe / 60_000
    val seconds = (safe % 60_000) / 1_000
    val milliseconds = safe % 1_000
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}." +
        milliseconds.toString().padStart(3, '0')
}

private fun normalizeName(value: String): String = value.trim().lowercase()

private fun RemoteMetadata.trackArtistNames(): List<String> = artists
    .ifEmpty { listOfNotNull(artist) }
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinctBy(::normalizeName)
