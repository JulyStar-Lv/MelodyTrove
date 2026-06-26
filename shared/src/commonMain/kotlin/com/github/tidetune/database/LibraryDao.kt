package com.github.tidetune.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageDao {
    @Query("SELECT * FROM storage ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<StorageEntity>>

    @Query("SELECT * FROM storage WHERE id = :id")
    suspend fun get(id: Long): StorageEntity?

    @Query("SELECT MAX(id) FROM storage")
    suspend fun maxId(): Long?

    @Upsert
    suspend fun upsert(storage: StorageEntity)

    @Query("DELETE FROM storage WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SelectedFolderDao {
    @Query("SELECT * FROM selected_folder WHERE storageId = :storageId ORDER BY displayPath")
    fun observeForStorage(storageId: Long): Flow<List<SelectedFolderEntity>>

    @Query(
        "SELECT * FROM selected_folder " +
            "WHERE storageId = :storageId AND canonicalPath = :canonicalPath LIMIT 1"
    )
    suspend fun findByPath(
        storageId: Long,
        canonicalPath: String,
    ): SelectedFolderEntity?

    @Upsert
    suspend fun upsert(folder: SelectedFolderEntity): Long

    @Query("DELETE FROM selected_folder WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RemoteFileDao {
    @Query("SELECT * FROM remote_file WHERE id = :id")
    suspend fun get(id: Long): RemoteFileEntity?

    @Query(
        "SELECT * FROM remote_file " +
            "WHERE storageId = :storageId AND canonicalPath = :canonicalPath LIMIT 1"
    )
    suspend fun findByPath(storageId: Long, canonicalPath: String): RemoteFileEntity?

    @Query(
        "SELECT * FROM remote_file " +
            "WHERE storageId = :storageId AND canonicalPath IN (:canonicalPaths)"
    )
    suspend fun findByPaths(
        storageId: Long,
        canonicalPaths: List<String>,
    ): List<RemoteFileEntity>

    @Query(
        "SELECT * FROM remote_file " +
            "WHERE storageId = :storageId AND remoteId IN (:remoteIds)"
    )
    suspend fun findByRemoteIds(
        storageId: Long,
        remoteIds: List<String>,
    ): List<RemoteFileEntity>

    @Query("SELECT COUNT(*) FROM remote_file WHERE selectedFolderId = :selectedFolderId")
    suspend fun countForFolder(selectedFolderId: Long): Long

    @Upsert
    suspend fun upsertAll(files: List<RemoteFileEntity>): List<Long>

    @Query(
        "UPDATE remote_file SET lastSeenScanId = :scanId, isDeleted = 0 " +
            "WHERE id IN (:ids)"
    )
    suspend fun markSeen(
        ids: List<Long>,
        scanId: String,
    )

    @Query(
        "UPDATE remote_file SET isDeleted = 1 " +
            "WHERE selectedFolderId = :selectedFolderId AND lastSeenScanId != :scanId"
    )
    suspend fun markMissingDeleted(selectedFolderId: Long, scanId: String)

    @Query(
        "UPDATE remote_file SET isDeleted = 1 " +
            "WHERE storageId = :storageId AND remoteId IN (:remoteIds)"
    )
    suspend fun markDeletedByRemoteIds(
        storageId: Long,
        remoteIds: List<String>,
    ): Int

    @Transaction
    suspend fun applyScanBatch(
        changedFiles: List<RemoteFileEntity>,
        unchangedIds: List<Long>,
        scanId: String,
    ) {
        if (changedFiles.isNotEmpty()) {
            upsertAll(changedFiles)
        }
        if (unchangedIds.isNotEmpty()) {
            markSeen(unchangedIds, scanId)
        }
    }
}

@Dao
interface TrackDao {
    @Query(
        """
        SELECT t.*
        FROM track t
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.remoteFileId IS NULL OR rf.isDeleted = 0
        ORDER BY t.title COLLATE NOCASE
        """
    )
    fun observeAll(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT t.*
        FROM track t
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.remoteFileId IS NULL OR rf.isDeleted = 0
        ORDER BY t.title COLLATE NOCASE
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun page(limit: Int, offset: Int): List<TrackEntity>

    @Query("SELECT * FROM track WHERE id = :id")
    suspend fun get(id: Long): TrackEntity?

    @Query("SELECT * FROM track WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<TrackEntity>

    @Query("SELECT * FROM track WHERE remoteFileId IN (:remoteFileIds)")
    suspend fun findByRemoteFileIds(remoteFileIds: List<Long>): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM track")
    suspend fun count(): Long

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("UPDATE track SET durationMs = :durationMs, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDuration(id: Long, durationMs: Long, updatedAt: Long)
}

data class PlaylistSummaryRow(
    val id: Long,
    val title: String,
    val artworkId: Long?,
    val coverStorageId: Long?,
    val coverPath: String?,
    val createdAt: Long,
    val sortOrder: Long,
    val musicCount: Long,
    val durationMs: Long?,
)

data class PlaylistTrackRow(
    val playlistId: Long,
    val trackId: Long,
    val sortOrder: Long,
    val title: String,
    val durationMs: Long?,
    val remoteFileId: Long?,
    val sourceStorageId: Long?,
    val sourcePath: String?,
)

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT p.id, p.title, p.artworkId, p.createdAt, p.sortOrder,
               p.coverStorageId, p.coverPath,
               COUNT(pt.trackId) AS musicCount,
               SUM(t.durationMs) AS durationMs
        FROM playlist p
        LEFT JOIN playlist_track pt ON pt.playlistId = p.id
        LEFT JOIN track t ON t.id = pt.trackId
        GROUP BY p.id
        ORDER BY p.sortOrder, p.id
        """
    )
    fun observeSummaries(): Flow<List<PlaylistSummaryRow>>

    @Query(
        """
        SELECT pt.playlistId, pt.trackId, pt.sortOrder, t.title, t.durationMs,
               t.remoteFileId, t.sourceStorageId, t.sourcePath
        FROM playlist_track pt
        JOIN track t ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.sortOrder, pt.trackId
        """
    )
    fun observeTracks(playlistId: Long): Flow<List<PlaylistTrackRow>>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun get(id: Long): PlaylistEntity?

    @Query("SELECT MAX(id) FROM playlist")
    suspend fun maxId(): Long?

    @Query("SELECT * FROM playlist ORDER BY sortOrder, id")
    suspend fun listAll(): List<PlaylistEntity>

    @Query("SELECT MAX(sortOrder) FROM playlist")
    suspend fun maxSortOrder(): Long?

    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertTracks(tracks: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_track WHERE playlistId = :playlistId")
    suspend fun deleteTracks(playlistId: Long)

    @Query("DELETE FROM playlist_track WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deleteTrack(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun replaceTracks(playlistId: Long, tracks: List<PlaylistTrackCrossRef>) {
        deleteTracks(playlistId)
        upsertTracks(tracks)
    }
}

@Dao
interface MetadataDao {
    @Upsert
    suspend fun upsertAlbums(albums: List<AlbumEntity>): List<Long>

    @Upsert
    suspend fun upsertArtists(artists: List<ArtistEntity>): List<Long>

    @Upsert
    suspend fun upsertGenres(genres: List<GenreEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbums(albums: List<AlbumEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenres(genres: List<GenreEntity>): List<Long>

    @Query("SELECT * FROM album WHERE normalizedName IN (:normalizedNames)")
    suspend fun findAlbumsByNormalizedNames(normalizedNames: List<String>): List<AlbumEntity>

    @Query("SELECT * FROM artist WHERE normalizedName IN (:normalizedNames)")
    suspend fun findArtistsByNormalizedNames(normalizedNames: List<String>): List<ArtistEntity>

    @Query("SELECT * FROM genre WHERE normalizedName IN (:normalizedNames)")
    suspend fun findGenresByNormalizedNames(normalizedNames: List<String>): List<GenreEntity>

    @Upsert
    suspend fun upsertTrackArtists(values: List<TrackArtistCrossRef>)

    @Query("DELETE FROM track_artist WHERE trackId IN (:trackIds)")
    suspend fun deleteTrackArtistsForTracks(trackIds: List<Long>)

    @Query(
        """
        SELECT a.name
        FROM artist a
        JOIN track_artist ta ON ta.artistId = a.id
        WHERE ta.trackId = :trackId
        ORDER BY ta.position
        """
    )
    suspend fun artistNamesForTrack(trackId: Long): List<String>

    @Upsert
    suspend fun upsertAlbumArtists(values: List<AlbumArtistCrossRef>)

    @Query("DELETE FROM album_artist WHERE albumId IN (:albumIds)")
    suspend fun deleteAlbumArtistsForAlbums(albumIds: List<Long>)

    @Query(
        """
        SELECT a.name
        FROM artist a
        JOIN album_artist aa ON aa.artistId = a.id
        WHERE aa.albumId = :albumId
        ORDER BY aa.position
        """
    )
    suspend fun artistNamesForAlbum(albumId: Long): List<String>

    @Upsert
    suspend fun upsertTrackGenres(values: List<TrackGenreCrossRef>)

    @Query("DELETE FROM track_genre WHERE trackId IN (:trackIds)")
    suspend fun deleteTrackGenresForTracks(trackIds: List<Long>)

    @Query(
        """
        SELECT g.name
        FROM genre g
        JOIN track_genre tg ON tg.genreId = g.id
        WHERE tg.trackId = :trackId
        ORDER BY g.name
        """
    )
    suspend fun genreNamesForTrack(trackId: Long): List<String>

    @Upsert
    suspend fun upsertArtwork(values: List<ArtworkEntity>): List<Long>

    @Upsert
    suspend fun upsertLyrics(values: List<LyricsEntity>)

    @Query("DELETE FROM lyrics WHERE trackId IN (:trackIds)")
    suspend fun deleteLyricsForTracks(trackIds: List<Long>)

    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    suspend fun getLyrics(trackId: Long): LyricsEntity?

    @Upsert
    suspend fun upsertRawMetadata(values: List<RawMetadataEntity>)

    @Query("DELETE FROM raw_metadata WHERE trackId IN (:trackIds)")
    suspend fun deleteRawMetadataForTracks(trackIds: List<Long>)

    @Query("SELECT * FROM raw_metadata WHERE trackId = :trackId ORDER BY id")
    suspend fun rawMetadataForTrack(trackId: Long): List<RawMetadataEntity>
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM import_job WHERE status IN ('QUEUED', 'RUNNING', 'PAUSED') ORDER BY createdAt")
    fun observeActiveJobs(): Flow<List<ImportJobEntity>>

    @Query("SELECT * FROM import_job ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentJobs(limit: Int): Flow<List<ImportJobEntity>>

    @Upsert
    suspend fun upsertJob(job: ImportJobEntity)

    @Upsert
    suspend fun upsertCursor(cursor: SyncCursorEntity)

    @Query("SELECT * FROM sync_cursor WHERE selectedFolderId = :selectedFolderId")
    suspend fun getCursor(selectedFolderId: Long): SyncCursorEntity?
}
