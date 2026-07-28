package io.github.julystar.musicapp.plugin.runtime

import io.github.julystar.musicapp.source.api.MetaCoverCandidate
import io.github.julystar.musicapp.source.api.MetaLyrics
import io.github.julystar.musicapp.source.api.MetaSongCandidate
import io.github.julystar.musicapp.source.api.MetaSongQuery
import io.github.julystar.musicapp.source.api.MetaSource
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LyricoJsMetaSource(
    private val plugin: InstalledPlugin,
    private val runtimeManager: PluginRuntimeManager,
    private val configProvider: PluginConfigProvider,
    private val resultParser: PluginResultParser,
) : MetaSource {
    override val id: String = plugin.descriptor.pluginId
    override val displayName: String = plugin.descriptor.pluginName

    override suspend fun searchSongs(query: MetaSongQuery): List<MetaSongCandidate> =
        searchSongs(query, PluginLookupMode.MANUAL)

    suspend fun searchSongs(
        query: MetaSongQuery,
        mode: PluginLookupMode,
    ): List<MetaSongCandidate> {
        requireUsable("searchSongs", mode)
        val separator = query.separator.ifEmpty { "/" }
        val request = buildJsonObject {
            put("keyword", query.keyword ?: query.defaultKeyword())
            put("page", query.page.coerceAtLeast(1))
            put("pageSize", query.pageSize.coerceAtLeast(1))
            put("separator", separator)
            put("config", configJson(query.config))
        }
        return resultParser.songs(
            pluginId = id,
            raw = call("searchSongs", request),
            separator = separator,
        )
    }

    override suspend fun getLyrics(
        candidate: MetaSongCandidate,
        config: Map<String, String>,
    ): MetaLyrics? = getLyrics(candidate, config, PluginLookupMode.MANUAL)

    suspend fun getLyrics(
        candidate: MetaSongCandidate,
        config: Map<String, String>,
        mode: PluginLookupMode,
    ): MetaLyrics? {
        requireUsable("getLyrics", mode)
        val internal = resultParser.internal(id, candidate.contextToken) ?: JsonObject(emptyMap())
        val request = buildJsonObject {
            put("song", buildJsonObject {
                put("id", candidate.id)
                put("title", candidate.title)
                candidate.artist?.let { put("artist", it) }
                candidate.album?.let { put("album", it) }
                candidate.durationMs?.let { put("duration", it) }
                put("sourceId", id)
                put("pluginId", id)
                put("fields", buildJsonObject {
                    candidate.fields.forEach { (key, value) -> put(key, value) }
                })
                put("internal", internal)
            })
            put("config", configJson(config))
        }
        return resultParser.lyrics(call("getLyrics", request))
    }

    override suspend fun searchCovers(query: MetaSongQuery): List<MetaCoverCandidate> =
        searchCovers(query, PluginLookupMode.MANUAL)

    suspend fun searchCovers(
        query: MetaSongQuery,
        mode: PluginLookupMode,
    ): List<MetaCoverCandidate> {
        requireUsable("searchCovers", mode)
        val request = buildJsonObject {
            put("keyword", query.keyword ?: query.defaultKeyword())
            put("pageSize", if (query.pageSize == 20) 5 else query.pageSize.coerceAtLeast(1))
            put("config", configJson(query.config))
        }
        return resultParser.covers(id, call("searchCovers", request))
    }

    suspend fun clearPrivateContexts() {
        resultParser.clearPlugin(id)
    }

    private suspend fun call(
        name: String,
        request: JsonObject,
    ): String = try {
        runtimeManager.call(
            plugin = plugin.descriptor,
            functionName = name,
            requestJson = request.toString(),
        )
    } catch (error: PluginRuntimeError) {
        if (error.requiresRuntimeRebuild()) {
            resultParser.clearPlugin(id)
        }
        throw error
    }

    private suspend fun configJson(overrides: Map<String, String>): JsonObject = buildJsonObject {
        (configProvider.config(id) + overrides).forEach { (key, value) ->
            put(key, value)
        }
    }

    private fun requireUsable(
        capability: String,
        mode: PluginLookupMode,
    ) {
        if (!plugin.enabled) {
            throw PluginLookupDeniedException(id, mode, "Plugin $id is disabled")
        }
        val allowed = when (mode) {
            PluginLookupMode.MANUAL -> plugin.allowManualLookup
            PluginLookupMode.AUTOMATIC -> plugin.allowAutomaticLookup
            PluginLookupMode.BATCH -> plugin.allowBatchLookup
        }
        if (!allowed) {
            throw PluginLookupDeniedException(id, mode, "Plugin $id does not allow $mode lookup")
        }
        if (capability !in plugin.capabilities) {
            throw PluginLookupDeniedException(
                id,
                mode,
                "Plugin $id does not declare $capability",
            )
        }
    }

    private fun MetaSongQuery.defaultKeyword(): String = listOfNotNull(
        title.takeIf(String::isNotBlank),
        artist?.takeIf(String::isNotBlank),
        album?.takeIf(String::isNotBlank),
    ).joinToString(" ")
}
