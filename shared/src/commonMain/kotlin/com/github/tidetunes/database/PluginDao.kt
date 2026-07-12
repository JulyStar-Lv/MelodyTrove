package com.github.tidetunes.database

import androidx.room.*
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

    @Query("SELECT * FROM plugin_config WHERE pluginId = :pluginId")
    suspend fun configsFor(pluginId: String): List<PluginConfigEntity>

    @Query("SELECT configValue FROM plugin_config WHERE pluginId = :pluginId AND configKey = :key")
    suspend fun configValue(pluginId: String, key: String): String?

    @Query("UPDATE plugin SET enabled = :enabled, updatedAt = :updatedAt WHERE pluginId = :pluginId")
    suspend fun setEnabled(pluginId: String, enabled: Boolean, updatedAt: Long)
}
