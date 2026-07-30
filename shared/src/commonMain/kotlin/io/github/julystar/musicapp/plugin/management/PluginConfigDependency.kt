package io.github.julystar.musicapp.plugin.management

import io.github.julystar.musicapp.plugin.install.ManifestConfigField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val MAX_PLUGIN_CONFIG_DEPENDENCY_DEPTH = 16

/**
 * Evaluates a Lyrico v3 config-field dependency against the current plugin
 * configuration. Fields without a dependency are visible by default.
 */
internal fun isPluginConfigFieldVisible(
    field: ManifestConfigField,
    values: Map<String, String>,
): Boolean = field.dependency?.matchesPluginConfigDependency(values) ?: true

private fun JsonObject.matchesPluginConfigDependency(
    values: Map<String, String>,
    depth: Int = 0,
): Boolean {
    if (depth > MAX_PLUGIN_CONFIG_DEPENDENCY_DEPTH) return false

    val dependencyTypeCount = keys.count { key ->
        key == "match" || key == "and" || key == "or" || key == "not"
    }
    if (dependencyTypeCount != 1) return false

    (this["match"] as? JsonObject)?.let { match ->
        val key = match["key"]?.jsonPrimitive?.contentOrNull
        val expectedValue = match["value"]?.jsonPrimitive?.contentOrNull
        return !key.isNullOrBlank() && expectedValue != null && values[key] == expectedValue
    }

    (this["and"] as? JsonObject)?.let { and ->
        val conditions = and["conditions"] as? JsonArray ?: return false
        return conditions.isNotEmpty() && conditions.all { condition ->
            (condition as? JsonObject)?.matchesPluginConfigDependency(values, depth + 1) == true
        }
    }

    (this["or"] as? JsonObject)?.let { or ->
        val conditions = or["conditions"] as? JsonArray ?: return false
        return conditions.isNotEmpty() && conditions.any { condition ->
            (condition as? JsonObject)?.matchesPluginConfigDependency(values, depth + 1) == true
        }
    }

    (this["not"] as? JsonObject)?.let { not ->
        val condition = not["condition"] as? JsonObject ?: return false
        return !condition.matchesPluginConfigDependency(values, depth + 1)
    }

    return false
}
