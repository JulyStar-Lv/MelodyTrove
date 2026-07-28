package io.github.julystar.musicapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugin ORDER BY name")
    fun all(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugin ORDER BY name")
    suspend fun allSnapshot(): List<PluginEntity>

    @Query("SELECT * FROM plugin WHERE pluginId = :pluginId")
    suspend fun findByPluginId(pluginId: String): PluginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plugin: PluginEntity): Long

    @Query("DELETE FROM plugin WHERE pluginId = :pluginId")
    suspend fun deleteByPluginId(pluginId: String)

    @Query("DELETE FROM plugin_config WHERE pluginId = :pluginId")
    suspend fun deleteConfigs(pluginId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(entry: PluginConfigEntity)

    @Query("DELETE FROM plugin_config WHERE pluginId = :pluginId AND configKey = :key")
    suspend fun deleteConfig(pluginId: String, key: String)

    @Query("SELECT * FROM plugin_config WHERE pluginId = :pluginId")
    suspend fun configsFor(pluginId: String): List<PluginConfigEntity>

    @Query("SELECT configValue FROM plugin_config WHERE pluginId = :pluginId AND configKey = :key")
    suspend fun configValue(pluginId: String, key: String): String?

    @Query("UPDATE plugin SET enabled = :enabled WHERE pluginId = :pluginId")
    suspend fun setEnabled(pluginId: String, enabled: Boolean)

    @Query("UPDATE plugin SET enabled = 0")
    suspend fun disableAll()

    @Query(
        """
        UPDATE plugin SET
            allowManualLookup = :allowManual,
            allowAutomaticLookup = :allowAutomatic,
            allowBatchLookup = :allowBatch
        WHERE pluginId = :pluginId
        """,
    )
    suspend fun setLookupPermissions(
        pluginId: String,
        allowManual: Boolean,
        allowAutomatic: Boolean,
        allowBatch: Boolean,
    )

    @Query(
        """
        UPDATE plugin SET lastError = :message, lastErrorAt = :occurredAt
        WHERE pluginId = :pluginId
        """,
    )
    suspend fun setLastError(
        pluginId: String,
        message: String,
        occurredAt: Long,
    )

    @Query("UPDATE plugin SET lastError = NULL, lastErrorAt = NULL WHERE pluginId = :pluginId")
    suspend fun clearLastError(pluginId: String)
}
