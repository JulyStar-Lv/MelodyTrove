package com.github.tidetunes.plugin.install

import com.github.tidetunes.database.PluginConfigEntity
import com.github.tidetunes.database.PluginEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePluginDao : com.github.tidetunes.database.PluginDao {
    private val pluginsFlow = MutableStateFlow<List<PluginEntity>>(emptyList())

    override fun all(): Flow<List<PluginEntity>> = pluginsFlow
    override suspend fun allSnapshot(): List<PluginEntity> = pluginsFlow.value
    override suspend fun findByPluginId(pluginId: String): PluginEntity? = pluginsFlow.value.find { it.pluginId == pluginId }
    override suspend fun upsert(plugin: PluginEntity): Long {
        val list = pluginsFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == plugin.id || it.pluginId == plugin.pluginId }
        if (idx >= 0) list[idx] = plugin else list.add(plugin)
        pluginsFlow.value = list
        return plugin.id
    }
    override suspend fun deleteByPluginId(pluginId: String) {
        pluginsFlow.value = pluginsFlow.value.filter { it.pluginId != pluginId }
    }
    override suspend fun deleteConfigs(pluginId: String) { /* no-op for tests */ }
    override suspend fun setConfig(entry: PluginConfigEntity) { /* no-op for tests */ }
    override suspend fun configsFor(pluginId: String): List<PluginConfigEntity> = emptyList()
    override suspend fun configValue(pluginId: String, key: String): String? = null
    override suspend fun setEnabled(pluginId: String, enabled: Boolean, updatedAt: Long) {
        pluginsFlow.value = pluginsFlow.value.map { if (it.pluginId == pluginId) it.copy(enabled = enabled, updatedAt = updatedAt) else it }
    }
}
