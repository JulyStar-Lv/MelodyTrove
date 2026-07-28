package io.github.julystar.musicapp.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "listening_history",
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["playedAtEpochMs"]),
    ],
)
data class ListeningHistoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val listenedMs: Long,
    val playedAtEpochMs: Long,
)

@Dao
interface ListeningStatisticsDao {
    @Query("SELECT * FROM listening_history ORDER BY playedAtEpochMs DESC, id DESC")
    fun observeHistory(): Flow<List<ListeningHistoryEntity>>

    @Insert
    suspend fun insertHistory(entry: ListeningHistoryEntity): Long

    @Query(
        """
        UPDATE listening_history
        SET listenedMs = listenedMs + :listenedMs
        WHERE id = :historyEntryId
        """
    )
    suspend fun addListenTime(historyEntryId: Long, listenedMs: Long)

    @Query("DELETE FROM listening_history WHERE id = :historyEntryId")
    suspend fun deleteHistoryEntry(historyEntryId: Long)

    @Query("DELETE FROM listening_history")
    suspend fun deleteAllHistory()

    @Query("UPDATE track SET lastPlayedAt = :playedAtEpochMs WHERE id = :trackId")
    suspend fun updateTrackLastPlayedAt(trackId: Long, playedAtEpochMs: Long)

    @Transaction
    suspend fun recordPlay(entry: ListeningHistoryEntity): Long {
        val id = insertHistory(entry)
        updateTrackLastPlayedAt(entry.trackId, entry.playedAtEpochMs)
        return id
    }
}
