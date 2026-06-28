package com.github.tidetunes.singleton

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.github.tidetunes.database.LyricsEntity
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.PlaylistDao
import com.github.tidetunes.database.PlaylistEntity
import com.github.tidetunes.database.PlaylistSummaryRow
import com.github.tidetunes.database.PlaylistTrackCrossRef
import com.github.tidetunes.database.PlaylistTrackRow
import com.github.tidetunes.database.RemoteFileDao
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.core.data.CreatePlaylistRequest
import com.github.tidetunes.core.data.UpdatePlaylistRequest
import com.github.tidetunes.core.data.toLegacyStorageEntry
import com.github.tidetunes.core.data.toLegacyStorageEntryLoc
import com.github.tidetunes.domain.importing.normalizeRemotePath
import com.github.tidetunes.domain.importing.stableTrackId
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.source.api.SourceNodeSelection
import com.github.tidetunes.source.api.SourceNodeType
import kotlinx.coroutines.flow.first
import uniffi.tidetunes_core.ArgCreatePlaylist
import uniffi.tidetunes_core.ArgUpdatePlaylist
import uniffi.tidetunes_core.DataSourceKey
import uniffi.tidetunes_core.LrcMetadata
import uniffi.tidetunes_core.LyricLine
import uniffi.tidetunes_core.LyricLoadState
import uniffi.tidetunes_core.Lyrics
import uniffi.tidetunes_core.Music
import uniffi.tidetunes_core.MusicAbstract
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.MusicLyric
import uniffi.tidetunes_core.MusicMeta
import uniffi.tidetunes_core.Playlist
import uniffi.tidetunes_core.PlaylistAbstract
import uniffi.tidetunes_core.PlaylistId
import uniffi.tidetunes_core.PlaylistMeta
import uniffi.tidetunes_core.StorageEntry
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.ToAddMusicEntry
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RoomLibraryStore(
    private val database: TideTunesDatabase,
    private val trackDao: TrackDao,
    private val remoteFileDao: RemoteFileDao,
    private val playlistDao: PlaylistDao,
    private val metadataDao: MetadataDao,
) {
    suspend fun getMusic(id: MusicId): Music? {
        val track = trackDao.get(id.value) ?: return null
        val loc = resolveTrackLoc(track) ?: return null
        val lyric = buildLyric(track.id, loc)
        return Music(
            meta = track.toMusicMeta(),
            loc = loc,
            cover = null,
            lyric = lyric,
        )
    }

    suspend fun getPlaylist(id: PlaylistId): Playlist? {
        val entity = playlistDao.get(id.value) ?: return null
        val rows = playlistDaoRows(id.value)
        val musics = rows.map { row ->
            MusicAbstract(
                meta = MusicMeta(
                    id = MusicId(row.trackId),
                    title = row.title,
                    duration = row.durationMs?.milliseconds,
                    order = listOf(row.sortOrder.coerceAtLeast(0).toUInt()),
                ),
                cover = null,
            )
        }
        return Playlist(
            abstr = entity.toPlaylistAbstract(musics.size.toLong(), musics.totalDuration()),
            musics = musics,
        )
    }

    // Long overload for callers that do not import UniFFI types
    suspend fun getPlaylistById(id: Long): Playlist? = getPlaylist(PlaylistId(id))

    suspend fun createPlaylist(arg: ArgCreatePlaylist): Playlist? {
        val now = currentTimeMillis()
        val playlistId = (playlistDao.maxId() ?: 0L) + 1L
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                playlistDao.upsert(
                    PlaylistEntity(
                        id = playlistId,
                        title = arg.title,
                        artworkId = null,
                        coverStorageId = arg.cover?.storageId?.value,
                        coverPath = arg.cover?.path,
                        createdAt = now,
                        updatedAt = now,
                        sortOrder = (playlistDao.maxSortOrder() ?: -1L) + 1L,
                    )
                )
                playlistDao.upsertTracks(
                    ensureTracksForEntries(arg.entries.map { it.entry to it.name }, now)
                        .mapIndexed { index, track ->
                            PlaylistTrackCrossRef(
                                playlistId = playlistId,
                                trackId = track.id,
                                sortOrder = index.toLong(),
                                addedAt = now,
                            )
                        }
                )
            }
        }
        return getPlaylist(PlaylistId(playlistId))
    }

    suspend fun createPlaylist(request: CreatePlaylistRequest): Playlist? {
        return createPlaylist(
            ArgCreatePlaylist(
                title = request.title,
                cover = request.cover
                    ?.takeIf { selection -> selection.node.type == SourceNodeType.Image }
                    ?.toLegacyStorageEntryLoc(),
                entries = request.entries
                    .filter { selection -> selection.node.type == SourceNodeType.Track }
                    .mapNotNull { selection ->
                        selection.toLegacyStorageEntry()?.let { entry ->
                            ToAddMusicEntry(entry, entry.name)
                        }
                    },
            )
        )
    }

    suspend fun updatePlaylist(arg: ArgUpdatePlaylist) {
        val current = playlistDao.get(arg.id.value) ?: return
        playlistDao.upsert(
            current.copy(
                title = arg.title,
                coverStorageId = arg.cover?.storageId?.value,
                coverPath = arg.cover?.path,
                updatedAt = currentTimeMillis(),
            )
        )
    }

    suspend fun updatePlaylist(request: UpdatePlaylistRequest) {
        updatePlaylist(
            ArgUpdatePlaylist(
                id = PlaylistId(request.id),
                title = request.title,
                cover = request.cover
                    ?.takeIf { selection -> selection.node.type == SourceNodeType.Image }
                    ?.toLegacyStorageEntryLoc(),
            )
        )
    }

    suspend fun addMusicEntries(playlistId: PlaylistId, entries: List<StorageEntry>): List<MusicId> {
        if (entries.isEmpty()) return emptyList()
        val now = currentTimeMillis()
        val currentRows = playlistDaoRows(playlistId.value)
        val existingTrackIds = currentRows.map { it.trackId }.toSet()
        val startOrder = (currentRows.maxOfOrNull { it.sortOrder } ?: -1L) + 1L
        val addedIds = mutableListOf<MusicId>()
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val tracks = ensureTracksForEntries(entries.map { it to null }, now)
                    .filter { it.id !in existingTrackIds }
                playlistDao.upsertTracks(
                    tracks.mapIndexed { index, track ->
                        addedIds += MusicId(track.id)
                        PlaylistTrackCrossRef(
                            playlistId = playlistId.value,
                            trackId = track.id,
                            sortOrder = startOrder + index,
                            addedAt = now,
                        )
                    }
                )
            }
        }
        return addedIds
    }


    // Long overloads for callers that do not import UniFFI types
    suspend fun addMusicSelectionsById(
        playlistId: Long,
        selections: List<SourceNodeSelection>,
    ): List<Long> {
        val ids = addMusicSelections(PlaylistId(playlistId), selections)
        return ids.map { it.value }
    }

    suspend fun addMusicSelections(
        playlistId: PlaylistId,
        selections: List<SourceNodeSelection>,
    ): List<MusicId> {
        return addMusicEntries(
            playlistId = playlistId,
            entries = selections.mapNotNull { selection -> selection.toLegacyStorageEntry() },
        )
    }

    suspend fun removeMusic(playlistId: PlaylistId, musicId: MusicId) {
        playlistDao.deleteTrack(playlistId.value, musicId.value)
    }


    suspend fun replaceMusicOrderById(
        playlistId: Long,
        orderedTrackIds: List<Long>,
    ) {
        replaceMusicOrder(PlaylistId(playlistId), orderedTrackIds.map { MusicId(it) })
    }

    suspend fun replaceMusicOrder(playlistId: PlaylistId, orderedIds: List<MusicId>) {
        val now = currentTimeMillis()
        playlistDao.replaceTracks(
            playlistId = playlistId.value,
            tracks = orderedIds.mapIndexed { index, id ->
                PlaylistTrackCrossRef(
                    playlistId = playlistId.value,
                    trackId = id.value,
                    sortOrder = index.toLong(),
                    addedAt = now,
                )
            },
        )
    }

    suspend fun replacePlaylistOrder(orderedIds: List<PlaylistId>) {
        orderedIds.forEachIndexed { index, id ->
            playlistDao.get(id.value)?.let { playlist ->
                playlistDao.upsert(
                    playlist.copy(
                        sortOrder = index.toLong(),
                        updatedAt = currentTimeMillis(),
                    )
                )
            }
        }
    }

    suspend fun removeLyric(trackId: MusicId) {
        metadataDao.deleteLyricsForTracks(listOf(trackId.value))
    }

    suspend fun updateDuration(trackId: MusicId, durationMs: Long) {
        trackDao.updateDuration(trackId.value, max(durationMs, 0L), currentTimeMillis())
    }

    suspend fun resolveTrackLoc(id: MusicId): StorageEntryLoc? {
        return trackDao.get(id.value)?.let { resolveTrackLoc(it) }
    }

    private suspend fun playlistDaoRows(playlistId: Long): List<PlaylistTrackRow> {
        return playlistDao.observeTracks(playlistId).first()
    }

    private suspend fun ensureTracksForEntries(
        entries: List<Pair<StorageEntry, String?>>,
        now: Long,
    ): List<TrackEntity> {
        val tracks = entries.map { (entry, entryTitle) ->
            val path = normalizeRemotePath(entry.path)
            val title = entryTitle
                ?.takeIf { it.isNotBlank() }
                ?: entry.name.ifBlank { path.substringAfterLast('/').substringBeforeLast('.') }
            TrackEntity(
                id = stableTrackId(entry.storageId.value, path),
                remoteFileId = null,
                sourceStorageId = entry.storageId.value,
                sourcePath = path,
                title = title.substringBeforeLast('.', title),
                sortTitle = null,
                albumId = null,
                albumArtist = null,
                composer = null,
                comment = null,
                grouping = null,
                durationMs = null,
                discNumber = null,
                discTotal = null,
                trackNumber = null,
                trackTotal = null,
                year = null,
                date = null,
                sampleRate = null,
                bitRate = null,
                bitsPerSample = null,
                channels = null,
                channelLayout = null,
                codec = null,
                container = null,
                lossless = null,
                createdAt = now,
                updatedAt = now,
            )
        }
        if (tracks.isNotEmpty()) {
            trackDao.upsertAll(tracks)
        }
        return tracks
    }

    private suspend fun resolveTrackLoc(track: TrackEntity): StorageEntryLoc? {
        val sourceStorageId = track.sourceStorageId
        val sourcePath = track.sourcePath
        if (sourceStorageId != null && sourcePath != null) {
            return StorageEntryLoc(storageId = uniffi.tidetunes_core.StorageId(sourceStorageId), path = sourcePath)
        }
        val remoteFileId = track.remoteFileId ?: return null
        val remoteFile = remoteFileDao.get(remoteFileId) ?: return null
        return StorageEntryLoc(
            storageId = uniffi.tidetunes_core.StorageId(remoteFile.storageId),
            path = remoteFile.canonicalPath,
        )
    }

    private suspend fun buildLyric(trackId: Long, loc: StorageEntryLoc): MusicLyric {
        val entity = metadataDao.getLyrics(trackId)
        val parsed = entity?.toLyrics()
        return MusicLyric(
            loc = loc,
            data = parsed ?: emptyLyrics(),
            loadedState = if (parsed == null) LyricLoadState.MISSING else LyricLoadState.LOADED,
        )
    }

    private fun TrackEntity.toMusicMeta(): MusicMeta {
        return MusicMeta(
            id = MusicId(id),
            title = title,
            duration = durationMs?.milliseconds,
            order = listOf(0u),
        )
    }

    private fun PlaylistEntity.toPlaylistAbstract(
        trackCount: Long,
        duration: Duration?,
    ): PlaylistAbstract {
        val coverLoc = coverPath?.let { path ->
            val storageId = coverStorageId ?: return@let null
            StorageEntryLoc(uniffi.tidetunes_core.StorageId(storageId), path)
        }
        return PlaylistAbstract(
            meta = PlaylistMeta(
                id = PlaylistId(id),
                title = title,
                cover = coverLoc,
                showCover = coverLoc?.let { DataSourceKey.AnyEntry(it) },
                createdTime = createdAt.milliseconds,
                order = listOf(sortOrder.coerceAtLeast(0).toUInt()),
            ),
            musicCount = trackCount.coerceAtLeast(0).toULong(),
            duration = duration,
        )
    }

    private fun PlaylistSummaryRow.toPlaylistAbstract(): PlaylistAbstract {
        val coverLoc = coverPath?.let { path ->
            val storageId = coverStorageId ?: return@let null
            StorageEntryLoc(uniffi.tidetunes_core.StorageId(storageId), path)
        }
        return PlaylistAbstract(
            meta = PlaylistMeta(
                id = PlaylistId(id),
                title = title,
                cover = coverLoc,
                showCover = coverLoc?.let { DataSourceKey.AnyEntry(it) },
                createdTime = createdAt.milliseconds,
                order = listOf(sortOrder.coerceAtLeast(0).toUInt()),
            ),
            musicCount = musicCount.coerceAtLeast(0).toULong(),
            duration = durationMs?.milliseconds,
        )
    }

    fun mapPlaylistSummary(row: PlaylistSummaryRow): PlaylistAbstract = row.toPlaylistAbstract()
}

private fun List<MusicAbstract>.totalDuration(): Duration? {
    var total = Duration.ZERO
    for (music in this) {
        val duration = music.meta.duration ?: return null
        total += duration
    }
    return total
}

private fun LyricsEntity.toLyrics(): Lyrics? {
    if (!synchronized) {
        return Lyrics(
            metdata = LrcMetadata("", "", "", "", "", "", ""),
            lines = listOf(LyricLine(Duration.ZERO, content)),
        )
    }
    val lines = content
        .lineSequence()
        .mapNotNull { line -> parseLrcLine(line.trim()) }
        .toList()
    return Lyrics(
        metdata = LrcMetadata("", "", "", "", "", "", ""),
        lines = lines,
    )
}

private fun parseLrcLine(line: String): LyricLine? {
    if (!line.startsWith("[")) return null
    val close = line.indexOf(']')
    if (close <= 1) return null
    val tag = line.substring(1, close)
    val text = line.substring(close + 1).trim()
    if (text.isBlank()) return null
    val parts = tag.split(':')
    if (parts.size != 2) return null
    val minutes = parts[0].toLongOrNull() ?: return null
    val seconds = parts[1].toDoubleOrNull() ?: return null
    return LyricLine(
        duration = (minutes * 60_000L + (seconds * 1_000.0).toLong()).milliseconds,
        text = text,
    )
}

private fun emptyLyrics(): Lyrics {
    return Lyrics(
        metdata = LrcMetadata("", "", "", "", "", "", ""),
        lines = emptyList(),
    )
}
