package com.github.tidetunes.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "plugin", indices = [Index(value = ["pluginId"], unique = true)])
data class PluginEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pluginId: String,
    val name: String,
    val versionCode: Long,
    val versionName: String,
    val author: String,
    val description: String,
    val apiVersion: Int,
    val minHostApiVersion: Int,
    val entryFile: String,
    val includeDirsJson: String,
    val iconPath: String?,
    val capabilitiesJson: String,
    val manifestRawJson: String,
    val installedAt: Long,
    val updatedAt: Long,
    val enabled: Boolean,
)

@Entity(tableName = "plugin_config", primaryKeys = ["pluginId", "configKey"])
data class PluginConfigEntity(
    val pluginId: String,
    val configKey: String,
    val configValue: String,
    val updatedAt: Long,
)
