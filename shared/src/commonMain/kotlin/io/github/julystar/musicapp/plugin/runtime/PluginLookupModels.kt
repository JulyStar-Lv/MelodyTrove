package io.github.julystar.musicapp.plugin.runtime

data class InstalledPlugin(
    val descriptor: PluginRuntimeDescriptor,
    val capabilities: Set<String>,
    val enabled: Boolean = false,
    val allowManualLookup: Boolean = true,
    val allowAutomaticLookup: Boolean = false,
    val allowBatchLookup: Boolean = false,
)

enum class PluginLookupMode {
    MANUAL,
    AUTOMATIC,
    BATCH,
}

class PluginLookupDeniedException(
    val pluginId: String,
    val mode: PluginLookupMode,
    message: String,
) : IllegalStateException(message)

class PluginResultParseException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

fun interface PluginConfigProvider {
    suspend fun config(pluginId: String): Map<String, String>
}
