package io.github.julystar.musicapp.plugin.management

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.presentation.components.DesignDialogDefaults
import io.github.julystar.musicapp.plugin.install.ManifestConfigField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Pure layout and configuration policies shared by the plugin settings UI and tests.
 *
 * These helpers intentionally contain no Compose state or platform-specific behavior,
 * so replacing a screen implementation does not remove the underlying policy contracts.
 */
internal fun isCompactPluginConfigurationDialog(windowWidth: Dp): Boolean =
    DesignDialogDefaults.isCompactWindow(windowWidth)

internal fun shouldPlacePluginConfigMenuAbove(
    anchorTop: Dp,
    anchorBottom: Dp,
    windowHeight: Dp,
    menuHeight: Dp,
): Boolean =
    anchorTop > windowHeight / 2 ||
        anchorBottom + 8.dp + menuHeight > windowHeight - 16.dp

internal fun shouldDismissPluginConfigurationSheet(
    dragOffsetPx: Float,
    velocityPxPerSecond: Float,
    distanceThresholdPx: Float,
    velocityThresholdPxPerSecond: Float,
): Boolean =
    dragOffsetPx >= distanceThresholdPx ||
        velocityPxPerSecond >= velocityThresholdPxPerSecond

internal fun isPluginConfigFieldVisible(
    field: ManifestConfigField,
    values: Map<String, String>,
): Boolean = field.dependency?.matchesPluginConfigValues(values, depth = 0) ?: true

private fun JsonObject.matchesPluginConfigValues(
    values: Map<String, String>,
    depth: Int,
): Boolean {
    if (depth > MAX_PLUGIN_CONFIG_DEPENDENCY_DEPTH) return false

    val dependencyTypeCount = keys.count { key ->
        key == "match" || key == "and" || key == "or" || key == "not"
    }
    if (dependencyTypeCount != 1) return false

    (this["match"] as? JsonObject)?.let { match ->
        val key = (match["key"] as? JsonPrimitive)?.contentOrNull
        val expected = (match["value"] as? JsonPrimitive)?.contentOrNull
        return !key.isNullOrBlank() && expected != null && values[key] == expected
    }

    (this["and"] as? JsonObject)?.let { and ->
        val conditions = and["conditions"] as? JsonArray ?: return false
        return conditions.isNotEmpty() && conditions.all { condition ->
            (condition as? JsonObject)?.matchesPluginConfigValues(values, depth + 1) == true
        }
    }

    (this["or"] as? JsonObject)?.let { or ->
        val conditions = or["conditions"] as? JsonArray ?: return false
        return conditions.isNotEmpty() && conditions.any { condition ->
            (condition as? JsonObject)?.matchesPluginConfigValues(values, depth + 1) == true
        }
    }

    (this["not"] as? JsonObject)?.let { not ->
        val condition = not["condition"] as? JsonObject ?: return false
        return !condition.matchesPluginConfigValues(values, depth + 1)
    }

    return false
}

private const val MAX_PLUGIN_CONFIG_DEPENDENCY_DEPTH = 16
