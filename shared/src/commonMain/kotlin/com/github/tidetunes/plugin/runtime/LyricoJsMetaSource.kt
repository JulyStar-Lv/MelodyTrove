package com.github.tidetunes.plugin.runtime

import com.github.tidetunes.plugin.currentTimeMillis
import com.github.tidetunes.source.api.MetaCoverCandidate
import com.github.tidetunes.source.api.MetaLyricLine
import com.github.tidetunes.source.api.MetaLyricWord
import com.github.tidetunes.source.api.MetaLyrics
import com.github.tidetunes.source.api.MetaSongCandidate
import com.github.tidetunes.source.api.MetaSongQuery
import com.github.tidetunes.source.api.MetaSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class InstalledPlugin(
    val descriptor: PluginRuntimeDescriptor,
    val capabilities: Set<String>,
    val enabled: Boolean = false,
    val allowManualLookup: Boolean = true,
    val allowAutomaticLookup: Boolean = false,
    val allowBatchLookup: Boolean = false,
)

fun interface PluginConfigProvider {
    suspend fun config(pluginId: String): Map<String, String>
}

class PluginCandidateContextStore(private val ttlMs: Long = 15 * 60 * 1000) {
    private data class Entry(val expires: Long, val value: JsonElement)

    private var next = 0L
    private val entries = mutableMapOf<String, Entry>()

    fun put(value: JsonElement): String {
        sweep()
        val token = "ctx-${(++next).toString(36)}"
        entries[token] = Entry(currentTimeMillis() + ttlMs, value)
        return token
    }

    fun take(token: String?): JsonElement? {
        sweep()
        return token?.let { entries[it]?.value }
    }

    private fun sweep() {
        val now = currentTimeMillis()
        entries.entries.removeAll { it.value.expires <= now }
    }
}

class PluginResultParser(
    private val contexts: PluginCandidateContextStore = PluginCandidateContextStore(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun songs(raw: String): List<MetaSongCandidate> {
        val root = json.parseToJsonElement(raw)
        val array = when (root) {
            is JsonArray -> root
            is JsonObject -> root["songs"] as? JsonArray
                ?: root["items"] as? JsonArray
                ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return array.mapNotNull { it as? JsonObject }.map { obj ->
            MetaSongCandidate(
                id = obj.string("id"),
                title = obj.string("title"),
                artist = obj.stringOrNull("artist"),
                album = obj.stringOrNull("album"),
                durationMs = obj.long("durationMs") ?: obj.long("duration"),
                date = obj.stringOrNull("date"),
                trackNumber = obj.stringOrNull("trackNumber"),
                pictureUrl = obj.stringOrNull("picUrl")
                    ?: obj.stringOrNull("pictureUrl")
                    ?: obj.stringOrNull("coverUrl"),
                fields = obj.fieldsMap(),
                contextToken = obj["internal"]?.let(contexts::put),
            )
        }
    }

    fun covers(raw: String): List<MetaCoverCandidate> {
        val root = json.parseToJsonElement(raw)
        val array = when (root) {
            is JsonArray -> root
            is JsonObject -> root["covers"] as? JsonArray
                ?: root["items"] as? JsonArray
                ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return array.mapNotNull { value ->
            when (value) {
                is JsonPrimitive -> value.contentOrNull?.let { MetaCoverCandidate(it) }
                is JsonObject -> value.stringOrNull("url")
                    ?.let { MetaCoverCandidate(it, value.int("width"), value.int("height"), value.stringOrNull("id")) }
                else -> null
            }
        }
    }

    fun lyrics(raw: String): MetaLyrics? {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: return null
        val lines = (root["lines"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { line ->
                MetaLyricLine(
                    text = line.string("text"),
                    startMs = line.long("startMs") ?: line.long("start"),
                    endMs = line.long("endMs") ?: line.long("end"),
                    words = (line["words"] as? JsonArray)
                        ?.mapNotNull { it as? JsonObject }
                        ?.map { word ->
                            MetaLyricWord(
                                text = word.string("text"),
                                startMs = word.long("startMs") ?: word.long("start"),
                                endMs = word.long("endMs") ?: word.long("end"),
                            )
                        }
                        .orEmpty(),
                    translation = line.stringOrNull("translation"),
                    romanization = line.stringOrNull("romanization"),
                    person = line.stringOrNull("person"),
                )
            }
            .orEmpty()
        return MetaLyrics(
            lines = lines,
            rawPlainLrc = root.stringOrNull("rawPlainLrc"),
            rawVerbatimLrc = root.stringOrNull("rawVerbatimLrc"),
            rawEnhancedLrc = root.stringOrNull("rawEnhancedLrc"),
            rawTtml = root.stringOrNull("rawTtml"),
            rawMultiPersonEnhancedLrc = root.stringOrNull("rawMultiPersonEnhancedLrc"),
            translated = root.stringOrNull("translated"),
            romanization = root.stringOrNull("romanization"),
        )
    }

    fun internal(token: String?) = contexts.take(token)

    private fun JsonObject.fieldsMap(): Map<String, String> =
        (this["fields"] as? JsonObject)
            ?.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }
            ?.toMap()
            .orEmpty()

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun JsonObject.stringOrNull(key: String) = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.long(key: String) = this[key]?.jsonPrimitive?.longOrNull
    private fun JsonObject.int(key: String) = this[key]?.jsonPrimitive?.intOrNull
}

class LyricoJsMetaSource(
    private val plugin: InstalledPlugin,
    private val runtimeManager: PluginRuntimeManager,
    private val configProvider: PluginConfigProvider,
    private val resultParser: PluginResultParser,
) : MetaSource {
    override val id = plugin.descriptor.pluginId
    override val displayName = plugin.descriptor.pluginName

    override suspend fun searchSongs(query: MetaSongQuery): List<MetaSongCandidate> {
        requireUsable("searchSongs")
        val request = buildJsonObject {
            put("title", query.title)
            query.artist?.let { put("artist", it) }
            query.album?.let { put("album", it) }
            query.durationMs?.let { put("duration", it) }
            put("config", configJson(query.config))
        }
        return resultParser.songs(call("searchSongs", request))
    }

    override suspend fun getLyrics(
        candidate: MetaSongCandidate,
        config: Map<String, String>,
    ): MetaLyrics? {
        requireUsable("getLyrics")
        val request = buildJsonObject {
            put("id", candidate.id)
            put("title", candidate.title)
            candidate.artist?.let { put("artist", it) }
            candidate.album?.let { put("album", it) }
            candidate.durationMs?.let { put("duration", it) }
            if (candidate.fields.isNotEmpty()) {
                put("fields", buildJsonObject {
                    candidate.fields.forEach { (key, value) -> put(key, value) }
                })
            }
            resultParser.internal(candidate.contextToken)?.let { put("internal", it) }
            put("config", configJson(config))
        }
        return resultParser.lyrics(call("getLyrics", request))
    }

    override suspend fun searchCovers(query: MetaSongQuery): List<MetaCoverCandidate> {
        requireUsable("searchCovers")
        val request = buildJsonObject {
            put("title", query.title)
            query.artist?.let { put("artist", it) }
            query.album?.let { put("album", it) }
            put("config", configJson(query.config))
        }
        return resultParser.covers(call("searchCovers", request))
    }

    private suspend fun call(name: String, request: JsonObject) =
        runtimeManager.call(plugin.descriptor, name, request.toString(), 5_000)

    private suspend fun configJson(overrides: Map<String, String>) = buildJsonObject {
        (configProvider.config(id) + overrides).forEach { (key, value) -> put(key, value) }
    }

    private fun requireUsable(capability: String) {
        check(plugin.enabled) { "plugin is disabled" }
        check(capability in plugin.capabilities) { "plugin does not declare $capability" }
    }
}
