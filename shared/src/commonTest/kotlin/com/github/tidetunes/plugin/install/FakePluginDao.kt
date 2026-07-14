package com.github.tidetunes.plugin.install

import com.github.tidetunes.database.PluginConfigEntity
import com.github.tidetunes.database.PluginDao
import com.github.tidetunes.database.PluginEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePluginDao : PluginDao {
    private val pluginsFlow = MutableStateFlow<List<PluginEntity>>(emptyList())
    private val configs = linkedMapOf<Pair<String, String>, PluginConfigEntity>()

    override fun all(): Flow<List<PluginEntity>> = pluginsFlow

    override suspend fun allSnapshot(): List<PluginEntity> = pluginsFlow.value

    override suspend fun findByPluginId(pluginId: String): PluginEntity? =
        pluginsFlow.value.find { it.pluginId == pluginId }

    override suspend fun upsert(plugin: PluginEntity): Long {
        val plugins = pluginsFlow.value.toMutableList()
        val index = plugins.indexOfFirst { it.id == plugin.id || it.pluginId == plugin.pluginId }
        val stored = if (plugin.id == 0L) plugin.copy(id = nextId(plugins)) else plugin
        if (index >= 0) plugins[index] = stored else plugins += stored
        pluginsFlow.value = plugins
        return stored.id
    }

    override suspend fun deleteByPluginId(pluginId: String) {
        pluginsFlow.value = pluginsFlow.value.filter { it.pluginId != pluginId }
    }

    override suspend fun deleteConfigs(pluginId: String) {
        configs.keys.removeAll { it.first == pluginId }
    }

    override suspend fun setConfig(entry: PluginConfigEntity) {
        configs[entry.pluginId to entry.configKey] = entry
    }

    override suspend fun deleteConfig(pluginId: String, key: String) {
        configs.remove(pluginId to key)
    }

    override suspend fun configsFor(pluginId: String): List<PluginConfigEntity> =
        configs.values.filter { it.pluginId == pluginId }

    override suspend fun configValue(pluginId: String, key: String): String? =
        configs[pluginId to key]?.configValue

    override suspend fun setEnabled(pluginId: String, enabled: Boolean) {
        update(pluginId) { it.copy(enabled = enabled) }
    }

    override suspend fun setLookupPermissions(
        pluginId: String,
        allowManual: Boolean,
        allowAutomatic: Boolean,
        allowBatch: Boolean,
    ) {
        update(pluginId) {
            it.copy(
                allowManualLookup = allowManual,
                allowAutomaticLookup = allowAutomatic,
                allowBatchLookup = allowBatch,
            )
        }
    }

    override suspend fun setLastError(
        pluginId: String,
        message: String,
        occurredAt: Long,
    ) {
        update(pluginId) { it.copy(lastError = message, lastErrorAt = occurredAt) }
    }

    override suspend fun clearLastError(pluginId: String) {
        update(pluginId) { it.copy(lastError = null, lastErrorAt = null) }
    }

    private fun update(
        pluginId: String,
        transform: (PluginEntity) -> PluginEntity,
    ) {
        pluginsFlow.value = pluginsFlow.value.map { plugin ->
            if (plugin.pluginId == pluginId) transform(plugin) else plugin
        }
    }

    private fun nextId(plugins: List<PluginEntity>): Long =
        (plugins.maxOfOrNull(PluginEntity::id) ?: 0L) + 1L
}
