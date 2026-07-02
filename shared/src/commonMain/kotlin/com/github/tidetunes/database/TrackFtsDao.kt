package com.github.tidetunes.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface TrackFtsDao {
    @Query(
        """
        SELECT t.*
        FROM track t
        JOIN track_fts fts ON t.id = fts.rowid
        WHERE track_fts MATCH :matchQuery
        ORDER BY fts.rowid
        LIMIT :limit
        """
    )
    suspend fun searchFts(matchQuery: String, limit: Int): List<TrackEntity>

    @Query(
        """
        SELECT t.*
        FROM track t
        JOIN track_fts fts ON t.id = fts.rowid
        WHERE track_fts MATCH :matchQuery
          AND EXISTS (
              SELECT 1 FROM track_source_ref ref
              WHERE ref.trackId = t.id
                AND ref.isAvailable = 1
          )
        ORDER BY fts.rowid
        LIMIT :limit
        """
    )
    suspend fun searchFtsExcludingDeleted(matchQuery: String, limit: Int): List<TrackEntity>

    @Query(
        """
        SELECT suggestion FROM (
            SELECT DISTINCT title AS suggestion
            FROM track_fts
            WHERE track_fts MATCH :matchQuery
            UNION
            SELECT DISTINCT artist AS suggestion
            FROM track_fts
            WHERE track_fts MATCH :matchQuery
            UNION
            SELECT DISTINCT albumArtist AS suggestion
            FROM track_fts
            WHERE track_fts MATCH :matchQuery
            UNION
            SELECT DISTINCT composer AS suggestion
            FROM track_fts
            WHERE track_fts MATCH :matchQuery
        )
        WHERE TRIM(suggestion) != ''
        ORDER BY suggestion COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun searchFtsSuggestions(matchQuery: String, limit: Int): List<String>
}
