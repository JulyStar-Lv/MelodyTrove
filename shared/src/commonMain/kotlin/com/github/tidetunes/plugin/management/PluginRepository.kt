package com.github.tidetunes.plugin.management

import com.github.tidetunes.plugin.currentTimeMillis

import com.github.tidetunes.database.PluginConfigEntity
import com.github.tidetunes.database.PluginDao
import com.github.tidetunes.database.PluginEntity
import com.github.tidetunes.plugin.install.ManifestConfigField
import com.github.tidetunes.plugin.install.ParsedManifest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*

data class PluginSummary(
    val id: String, val name: String, val versionName: String, val versionCode: Long,
    val author: String, val description: String, val capabilities: List<String>,
    val enabled: Boolean, val installedAt: Long, val configFields: List<ManifestConfigField>,
)

class PluginRepository(private val pluginDao: PluginDao) {
    private val json = Json { ignoreUnknownKeys = true }

    fun allPlugins(): Flow<List<PluginSummary>> = pluginDao.all().map { list ->
        list.map { it.toSummary() }
    }

    suspend fun getPlugin(pluginId: String): PluginSummary? =
        pluginDao.findByPluginId(pluginId)?.toSummary()

    suspend fun config(pluginId: String): Map<String, String> =
        pluginDao.configsFor(pluginId).associate { it.configKey to it.configValue }

    suspend fun setConfig(pluginId: String, key: String, value: String) {
        pluginDao.setConfig(PluginConfigEntity(pluginId, key, value, currentTimeMillis()))
    }

    suspend fun importPluginDefaults(manifest: ParsedManifest) {
        manifest.configFields.filter { it.defaultValue != null && it.defaultValue.isNotEmpty() }.forEach { field ->
            if (pluginDao.configValue(manifest.id, field.key) == null) {
                pluginDao.setConfig(PluginConfigEntity(manifest.id, field.key, field.defaultValue!!,
                    currentTimeMillis()))
            }
        }
    }

    suspend fun setEnabled(pluginId: String, enabled: Boolean) {
        pluginDao.setEnabled(pluginId, enabled, currentTimeMillis())
    }

    private suspend fun PluginEntity.toSummary(): PluginSummary {
        val caps = try {
            json.decodeFromString<List<String>>(capabilitiesJson)
        } catch (_: Exception) { emptyList() }
        val configFields = try {
            val manifest = json.parseToJsonElement(manifestRawJson).jsonObject
            (manifest["configFields"] as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }
                ?.map { field ->
                    ManifestConfigField(
                        key = field["key"]?.jsonPrimitive?.content.orEmpty(),
                        title = field["title"]?.jsonPrimitive?.content.orEmpty(),
                        summary = field["summary"]?.jsonPrimitive?.contentOrNull,
                        group = field["group"]?.jsonPrimitive?.contentOrNull,
                        type = field["type"]?.jsonPrimitive?.content ?: "text",
                        required = field["required"]?.jsonPrimitive?.booleanOrNull == true,
                        defaultValue = field["defaultValue"]?.jsonPrimitive?.contentOrNull,
                        dependency = field["dependency"] as? kotlinx.serialization.json.JsonObject,
                    )
                }
                .orEmpty()
        } catch (_: Exception) { emptyList() }
        return PluginSummary(
            id = pluginId, name = name, versionName = versionName, versionCode = versionCode,
            author = author, description = description, capabilities = caps,
            enabled = enabled, installedAt = installedAt, configFields = configFields,
        )
    }
}
