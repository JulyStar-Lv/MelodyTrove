package com.github.tidetunes.database

import androidx.room.Dao
import androidx.room.Index
import kotlinx.coroutines.flow.Flow
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

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

    @Query(
        """
        SELECT t.*
        FROM track t
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.albumId = :albumId AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)
        ORDER BY t.discNumber ASC, t.trackNumber ASC, t.title COLLATE NOCASE
        """
    )
    suspend fun findByAlbumId(albumId: Long): List<TrackEntity>

    @Query("SELECT * FROM track WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<TrackEntity>

    @Query(
        """
        SELECT t.*
        FROM track t
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.remoteFileId IS NULL OR rf.isDeleted = 0
        ORDER BY t.createdAt DESC LIMIT :limit
        """
    )
    suspend fun findRecentlyAdded(limit: Int): List<TrackEntity>

    @Query(
        """
        SELECT t.*
        FROM track t
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.lastPlayedAt IS NOT NULL AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)
        ORDER BY t.lastPlayedAt DESC LIMIT :limit
        """
    )
    suspend fun findRecentlyPlayed(limit: Int): List<TrackEntity>

    @Query(
        """
        SELECT t.*
        FROM track t
        JOIN track_artist ta ON ta.trackId = t.id
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE ta.artistId = :artistId AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)
        ORDER BY t.year ASC, t.albumId ASC, t.discNumber ASC, t.trackNumber ASC, t.title COLLATE NOCASE
        """
    )
    suspend fun findTracksByArtistId(artistId: Long): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM track t LEFT JOIN remote_file rf ON rf.id = t.remoteFileId WHERE t.albumId = :albumId AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)")
    suspend fun countTracksByAlbumId(albumId: Long): Int

    @Query("SELECT COUNT(*) FROM track t JOIN track_artist ta ON ta.trackId = t.id LEFT JOIN remote_file rf ON rf.id = t.remoteFileId WHERE ta.artistId = :artistId AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)")
    suspend fun countTracksByArtistId(artistId: Long): Int


    @Query(
        """
        SELECT t.*
        FROM track t
        JOIN track_genre tg ON tg.trackId = t.id
        JOIN genre g ON g.id = tg.genreId
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE g.name = :genreName AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)
        ORDER BY t.title COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun findTracksByGenre(genreName: String, limit: Int): List<TrackEntity>

    @Query("SELECT * FROM track WHERE remoteFileId IN (:remoteFileIds)")
    suspend fun findByRemoteFileIds(remoteFileIds: List<Long>): List<TrackEntity>

    @Query(
        """
        SELECT t.*
        FROM track t
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE (t.remoteFileId IS NULL OR rf.isDeleted = 0)
          AND (
              t.title COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
              OR t.artist COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
              OR t.albumArtist COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
              OR t.composer COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
          )
        ORDER BY
          CASE
              WHEN t.title COLLATE NOCASE = :query THEN 0
              WHEN t.title COLLATE NOCASE LIKE :prefixQuery ESCAPE '\' THEN 1
              ELSE 2
          END,
          t.title COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun search(
        query: String,
        prefixQuery: String,
        containsQuery: String,
        limit: Int,
    ): List<TrackEntity>

    @Query(
        """
        SELECT t.*,
               COALESCE(NULLIF(t.sourcePath, ''), rf.displayPath, rf.canonicalPath) AS resolvedSourcePath,
               a.name AS resolvedAlbum
        FROM track t
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        LEFT JOIN album a ON a.id = t.albumId
        WHERE (t.sourceStorageId = :storageId OR rf.storageId = :storageId)
          AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)
          AND TRIM(COALESCE(NULLIF(t.sourcePath, ''), rf.displayPath, rf.canonicalPath)) != ''
          AND (
              t.title COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
              OR t.artist COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
              OR t.albumArtist COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
              OR t.composer COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
          )
        ORDER BY
          CASE
              WHEN t.title COLLATE NOCASE = :query THEN 0
              WHEN t.title COLLATE NOCASE LIKE :prefixQuery ESCAPE '\' THEN 1
              ELSE 2
          END,
          t.title COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun searchBySourceStorage(
        storageId: Long,
        query: String,
        prefixQuery: String,
        containsQuery: String,
        limit: Int,
    ): List<SourceTrackSearchRow>

    @Query(
        """
        SELECT suggestion
        FROM (
            SELECT suggestion, MIN(score) AS bestScore
            FROM (
                SELECT t.title AS suggestion,
                       CASE
                           WHEN t.title COLLATE NOCASE = :query THEN 0
                           WHEN t.title COLLATE NOCASE LIKE :prefixQuery ESCAPE '\' THEN 10
                           ELSE 20
                       END AS score
                FROM track t
                LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
                WHERE (t.remoteFileId IS NULL OR rf.isDeleted = 0)
                  AND t.title COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
                UNION ALL
                SELECT t.artist AS suggestion,
                       CASE
                           WHEN t.artist COLLATE NOCASE = :query THEN 1
                           WHEN t.artist COLLATE NOCASE LIKE :prefixQuery ESCAPE '\' THEN 11
                           ELSE 21
                       END AS score
                FROM track t
                LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
                WHERE (t.remoteFileId IS NULL OR rf.isDeleted = 0)
                  AND t.artist COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
                UNION ALL
                SELECT t.albumArtist AS suggestion,
                       CASE
                           WHEN t.albumArtist COLLATE NOCASE = :query THEN 2
                           WHEN t.albumArtist COLLATE NOCASE LIKE :prefixQuery ESCAPE '\' THEN 12
                           ELSE 22
                       END AS score
                FROM track t
                LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
                WHERE (t.remoteFileId IS NULL OR rf.isDeleted = 0)
                  AND t.albumArtist COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
                UNION ALL
                SELECT t.composer AS suggestion,
                       CASE
                           WHEN t.composer COLLATE NOCASE = :query THEN 3
                           WHEN t.composer COLLATE NOCASE LIKE :prefixQuery ESCAPE '\' THEN 13
                           ELSE 23
                       END AS score
                FROM track t
                LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
                WHERE (t.remoteFileId IS NULL OR rf.isDeleted = 0)
                  AND t.composer COLLATE NOCASE LIKE :containsQuery ESCAPE '\'
            ) AS rawSuggestions
            WHERE TRIM(suggestion) != ''
            GROUP BY suggestion COLLATE NOCASE
        ) AS rankedSuggestions
        ORDER BY bestScore, suggestion COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun searchSuggestions(
        query: String,
        prefixQuery: String,
        containsQuery: String,
        limit: Int,
    ): List<String>

    @Query("SELECT COUNT(*) FROM track")
    suspend fun count(): Long

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("UPDATE track SET durationMs = :durationMs, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDuration(id: Long, durationMs: Long, updatedAt: Long)

    @Query("UPDATE track SET lastPlayedAt = :playedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLastPlayedAt(id: Long, playedAt: Long, updatedAt: Long)
}

data class SourceTrackSearchRow(
    @Embedded val track: TrackEntity,
    val resolvedSourcePath: String,
    val resolvedAlbum: String?,
)

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
    suspend fun upsertGenres(genres: List<GenreEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbums(albums: List<AlbumEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenres(genres: List<GenreEntity>): List<Long>

    @Query("SELECT * FROM album WHERE normalizedName IN (:normalizedNames)")
    suspend fun findAlbumsByNormalizedNames(normalizedNames: List<String>): List<AlbumEntity>

    @Query("SELECT * FROM album WHERE id = :id")
    suspend fun getAlbum(id: Long): AlbumEntity?

    @Query("SELECT * FROM artist WHERE normalizedName IN (:normalizedNames)")
    suspend fun findArtistsByNormalizedNames(normalizedNames: List<String>): List<ArtistEntity>

    @Query("SELECT * FROM artist WHERE id = :id")
    suspend fun getArtist(id: Long): ArtistEntity?

    @Query(
        """
        SELECT DISTINCT a.*
        FROM album a
        JOIN album_artist aa ON aa.albumId = a.id
        JOIN track t ON t.albumId = a.id
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE aa.artistId = :artistId AND (t.remoteFileId IS NULL OR rf.isDeleted = 0)
        ORDER BY a.year ASC, a.name COLLATE NOCASE
        """
    )
    suspend fun albumsByArtistId(artistId: Long): List<AlbumEntity>

    @Query(
        """
        SELECT DISTINCT a.*
        FROM album a
        JOIN track t ON t.albumId = a.id
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.remoteFileId IS NULL OR rf.isDeleted = 0
        ORDER BY a.name COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun listAlbumsWithTracks(limit: Int): List<AlbumEntity>

    @Query(
        """
        SELECT DISTINCT ar.*
        FROM artist ar
        JOIN track_artist ta ON ta.artistId = ar.id
        JOIN track t ON t.id = ta.trackId
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.remoteFileId IS NULL OR rf.isDeleted = 0
        ORDER BY ar.name COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun listArtistsWithTracks(limit: Int): List<ArtistEntity>

    @Query(
        """
        SELECT DISTINCT g.name
        FROM genre g
        JOIN track_genre tg ON tg.genreId = g.id
        JOIN track t ON t.id = tg.trackId
        LEFT JOIN remote_file rf ON rf.id = t.remoteFileId
        WHERE t.remoteFileId IS NULL OR rf.isDeleted = 0
        ORDER BY g.name COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun listGenreNames(limit: Int): List<String>

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

    @Query("SELECT * FROM artwork WHERE trackId = :trackId ORDER BY id DESC LIMIT 1")
    suspend fun getArtworkForTrack(trackId: Long): ArtworkEntity?

    @Query("SELECT * FROM artwork WHERE albumId = :albumId ORDER BY id DESC LIMIT 1")
    suspend fun getArtworkForAlbum(albumId: Long): ArtworkEntity?

    @Query("SELECT * FROM artwork WHERE contentHash = :contentHash LIMIT 1")
    suspend fun getArtworkByContentHash(contentHash: String): ArtworkEntity?

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

    @Query(
        """
        SELECT j.*, sf.storageId AS folderStorageId, sf.remoteId AS folderRemoteId,
               sf.canonicalPath AS folderCanonicalPath, sf.displayPath AS folderDisplayPath
        FROM import_job j
        JOIN selected_folder sf ON sf.id = j.selectedFolderId
        WHERE j.status IN ('QUEUED', 'RUNNING', 'PAUSED')
        ORDER BY j.createdAt
        """
    )
    fun observeActiveJobsWithFolder(): Flow<List<ImportJobWithFolder>>

    @Query(
        """
        SELECT j.*, sf.storageId AS folderStorageId, sf.remoteId AS folderRemoteId,
               sf.canonicalPath AS folderCanonicalPath, sf.displayPath AS folderDisplayPath
        FROM import_job j
        JOIN selected_folder sf ON sf.id = j.selectedFolderId
        ORDER BY j.updatedAt DESC LIMIT :limit
        """
    )
    fun observeRecentJobsWithFolder(limit: Int): Flow<List<ImportJobWithFolder>>

    @Query(
        """
        SELECT j.*, sf.storageId AS folderStorageId, sf.remoteId AS folderRemoteId,
               sf.canonicalPath AS folderCanonicalPath, sf.displayPath AS folderDisplayPath
        FROM import_job j
        JOIN selected_folder sf ON sf.id = j.selectedFolderId
        WHERE j.id = :jobId
        """
    )
    suspend fun getJobWithFolder(jobId: String): ImportJobWithFolder?

    @Query(
        """
        SELECT COUNT(*)
        FROM import_job j
        JOIN selected_folder sf ON sf.id = j.selectedFolderId
        WHERE sf.storageId = :storageId
          AND j.status IN ('QUEUED', 'RUNNING', 'PAUSED')
          AND (:excludedJobId = '' OR j.id != :excludedJobId)
        """
    )
    suspend fun activeJobCountForStorage(storageId: Long, excludedJobId: String): Int

    @Query("UPDATE import_job SET status = 'PAUSED', updatedAt = :now WHERE id = :jobId")
    suspend fun markJobPaused(jobId: String, now: Long): Int

    @Query(
        """
        UPDATE selected_folder
        SET syncStatus = 'PAUSED', lastSyncAt = :now
        WHERE id = (SELECT selectedFolderId FROM import_job WHERE id = :jobId)
        """
    )
    suspend fun markSelectedFolderPausedForJob(jobId: String, now: Long): Int

    @Query("UPDATE import_job SET status = 'CANCELLED', updatedAt = :now WHERE id = :jobId")
    suspend fun markJobCancelled(jobId: String, now: Long): Int

    @Query(
        """
        UPDATE selected_folder
        SET syncStatus = 'IDLE', lastSyncAt = :now
        WHERE id = (SELECT selectedFolderId FROM import_job WHERE id = :jobId)
        """
    )
    suspend fun markSelectedFolderCancelledForJob(jobId: String, now: Long): Int

    @Upsert
    suspend fun upsertJob(job: ImportJobEntity)

    @Upsert
    suspend fun upsertCursor(cursor: SyncCursorEntity)

    @Query("SELECT * FROM sync_cursor WHERE selectedFolderId = :selectedFolderId")
    suspend fun getCursor(selectedFolderId: Long): SyncCursorEntity?
}

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_task ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_task WHERE status IN ('Queued', 'Resolving', 'Downloading', 'Paused') ORDER BY updatedAt")
    fun observeActive(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_task WHERE id = :id")
    fun observe(id: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM download_task WHERE id = :id")
    suspend fun get(id: String): DownloadTaskEntity?

    @Upsert
    suspend fun upsert(task: DownloadTaskEntity)

    @Query("DELETE FROM download_task WHERE id = :id")
    suspend fun delete(id: String)
}
