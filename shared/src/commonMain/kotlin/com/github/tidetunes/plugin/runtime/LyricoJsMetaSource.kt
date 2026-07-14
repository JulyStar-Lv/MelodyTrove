package com.github.tidetunes.plugin.runtime

import com.github.tidetunes.plugin.currentTimeMillis
import com.github.tidetunes.source.api.MetaCoverCandidate
import com.github.tidetunes.source.api.MetaLyricLine
import com.github.tidetunes.source.api.MetaLyricWord
import com.github.tidetunes.source.api.MetaLyrics
import com.github.tidetunes.source.api.MetaSongCandidate
import com.github.tidetunes.source.api.MetaSongQuery
import com.github.tidetunes.source.api.MetaSource
import kotlin.random.Random
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
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

fun interface PluginConfigProvider {
    suspend fun config(pluginId: String): Map<String, String>
}

class PluginCandidateContextStore(
    private val ttlMs: Long = 15 * 60 * 1000,
    private val maxEntries: Int = 2_000,
) {
    private data class Entry(
        val pluginId: String,
        val expiresAt: Long,
        val value: JsonElement,
    )

    private val mutex = Mutex()
    private val entries = linkedMapOf<String, Entry>()

    suspend fun put(pluginId: String, value: JsonElement): String = mutex.withLock {
        sweepLocked()
        while (entries.size >= maxEntries) entries.remove(entries.keys.first())
        var token: String
        do {
            token = buildString {
                append("ctx-")
                append(currentTimeMillis().toString(36))
                append('-')
                repeat(4) { append(Random.nextLong().toULong().toString(36)) }
            }
        } while (entries.containsKey(token))
        entries[token] = Entry(pluginId, currentTimeMillis() + ttlMs, value)
        token
    }

    suspend fun get(pluginId: String, token: String?): JsonElement? = mutex.withLock {
        sweepLocked()
        token?.let(entries::get)?.takeIf { it.pluginId == pluginId }?.value
    }

    suspend fun clearPlugin(pluginId: String) = mutex.withLock {
        entries.entries.removeAll { it.value.pluginId == pluginId }
    }

    suspend fun clearAll() = mutex.withLock { entries.clear() }

    private fun sweepLocked() {
        val now = currentTimeMillis()
        entries.entries.removeAll { it.value.expiresAt <= now }
    }
}

class PluginResultParser(
    private val contexts: PluginCandidateContextStore = PluginCandidateContextStore(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun songs(
        pluginId: String,
        raw: String,
        separator: String = "/",
    ): List<MetaSongCandidate> = resultArray(parseRoot(raw), "items", "results", "songs", "data")
        .mapNotNull { value ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            val id = obj.firstString("id", "songId", "trackId")?.trim().orEmpty()
            val title = obj.firstString("title", "name", "songName")?.trim().orEmpty()
            if (id.isEmpty() || title.isEmpty()) return@mapNotNull null
            val artist = when (val artistValue = obj.first("artist", "artists", "singer")) {
                is JsonArray -> artistValue.mapNotNull { it.primitiveString() }.joinToString(separator).ifBlank { null }
                else -> artistValue?.primitiveString()
            }
            MetaSongCandidate(
                id = id,
                title = title,
                artist = artist,
                album = obj.firstString("album", "albumName"),
                durationMs = obj.firstLong("duration", "durationMs", "duration_ms"),
                date = obj.firstString("date", "releaseDate", "release_date"),
                trackNumber = obj.firstString("trackNumber", "trackerNumber", "track_number"),
                pictureUrl = obj.firstString("picUrl", "coverUrl", "cover_url", "artworkUrl", "pictureUrl"),
                fields = obj.fieldsMap(),
                contextToken = obj["internal"]?.takeUnless { it is JsonNull }?.let { contexts.put(pluginId, it) },
            )
        }

    fun covers(pluginId: String, raw: String): List<MetaCoverCandidate> =
        resultArray(parseRoot(raw), "items", "results", "songs", "data", "covers")
            .mapNotNull { value ->
                when (value) {
                    is JsonPrimitive -> value.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let { MetaCoverCandidate(it, sourceId = pluginId) }
                    is JsonObject -> value.firstString(
                        "url",
                        "picUrl",
                        "coverUrl",
                        "cover_url",
                        "artworkUrl",
                        "pictureUrl",
                    )?.takeIf { it.isNotBlank() }?.let { url ->
                        MetaCoverCandidate(
                            url = url,
                            width = value.firstInt("width"),
                            height = value.firstInt("height"),
                            sourceId = value.firstString("sourceId", "pluginId", "id") ?: pluginId,
                        )
                    }
                    else -> null
                }
            }

    fun lyrics(raw: String): MetaLyrics? {
        val root = parseRoot(raw) as? JsonObject ?: return null
        if ((root["notFound"] as? JsonPrimitive)?.contentOrNull == "true") return null
        val legacyLines = root["lines"] as? JsonArray
        if (legacyLines != null) return parseLegacyLyrics(root, legacyLines)

        return when (root.firstString("type") ?: "structured") {
            "structured" -> parseStructuredLyrics(root)
            "rawPlainLrc" -> MetaLyrics(rawPlainLrc = root.firstString("rawPlainLrc", "content", "lyrics"))
            "rawVerbatimLrc" -> MetaLyrics(rawVerbatimLrc = root.firstString("rawVerbatimLrc", "content", "lyrics"))
            "rawEnhancedLrc" -> MetaLyrics(rawEnhancedLrc = root.firstString("rawEnhancedLrc", "content", "lyrics"))
            "rawTtml" -> MetaLyrics(rawTtml = root.firstString("rawTtml", "content", "lyrics"))
            "rawMultiPersonEnhancedLrc" -> MetaLyrics(
                rawMultiPersonEnhancedLrc = root.firstString("rawMultiPersonEnhancedLrc", "content", "lyrics"),
            )
            else -> MetaLyrics(
                rawPlainLrc = root.firstString("rawPlainLrc"),
                rawVerbatimLrc = root.firstString("rawVerbatimLrc"),
                rawEnhancedLrc = root.firstString("rawEnhancedLrc"),
                rawTtml = root.firstString("rawTtml"),
                rawMultiPersonEnhancedLrc = root.firstString("rawMultiPersonEnhancedLrc"),
            )
        }
    }

    suspend fun internal(pluginId: String, token: String?) = contexts.get(pluginId, token)

    suspend fun clearPlugin(pluginId: String) = contexts.clearPlugin(pluginId)

    private fun parseStructuredLyrics(root: JsonObject): MetaLyrics {
        val translated = timedTexts(root["translated"] as? JsonArray)
        val romanized = timedTexts(root["romanization"] as? JsonArray)
        val original = root["original"] as? JsonArray ?: JsonArray(emptyList())
        val lines = original.mapIndexedNotNull { index, element ->
            val tuple = element as? JsonArray ?: return@mapIndexedNotNull null
            val start = tuple.getOrNull(0)?.primitiveLong()
            val end = tuple.getOrNull(1)?.primitiveLong()
            val payload = tuple.getOrNull(2) ?: return@mapIndexedNotNull null
            val words = (payload as? JsonArray)?.mapNotNull { wordValue ->
                val word = wordValue as? JsonArray ?: return@mapNotNull null
                MetaLyricWord(
                    text = word.getOrNull(2)?.primitiveString().orEmpty(),
                    startMs = word.getOrNull(0)?.primitiveLong(),
                    endMs = word.getOrNull(1)?.primitiveLong(),
                )
            }.orEmpty()
            val text = if (words.isNotEmpty()) words.joinToString("") { it.text } else payload.primitiveString().orEmpty()
            MetaLyricLine(
                text = text,
                startMs = start,
                endMs = end,
                words = words,
                translation = matchTimedText(translated, start, index),
                romanization = matchTimedText(romanized, start, index),
            )
        }
        return MetaLyrics(lines = lines)
    }

    private fun parseLegacyLyrics(root: JsonObject, lineValues: JsonArray): MetaLyrics = MetaLyrics(
        lines = lineValues.mapNotNull { it as? JsonObject }.map { line ->
            MetaLyricLine(
                text = line.firstString("text").orEmpty(),
                startMs = line.firstLong("startMs", "start"),
                endMs = line.firstLong("endMs", "end"),
                words = (line["words"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map { word ->
                    MetaLyricWord(
                        text = word.firstString("text").orEmpty(),
                        startMs = word.firstLong("startMs", "start"),
                        endMs = word.firstLong("endMs", "end"),
                    )
                },
                translation = line.firstString("translation"),
                romanization = line.firstString("romanization"),
                person = line.firstString("person"),
            )
        },
        rawPlainLrc = root.firstString("rawPlainLrc"),
        rawVerbatimLrc = root.firstString("rawVerbatimLrc"),
        rawEnhancedLrc = root.firstString("rawEnhancedLrc"),
        rawTtml = root.firstString("rawTtml"),
        rawMultiPersonEnhancedLrc = root.firstString("rawMultiPersonEnhancedLrc"),
        translated = root.firstString("translated"),
        romanization = root.firstString("romanization"),
    )

    private fun timedTexts(array: JsonArray?): List<Pair<Long?, String>> = array.orEmpty().mapNotNull { value ->
        val tuple = value as? JsonArray ?: return@mapNotNull null
        tuple.getOrNull(0)?.primitiveLong() to tuple.getOrNull(2)?.primitiveString().orEmpty()
    }

    private fun matchTimedText(values: List<Pair<Long?, String>>, startMs: Long?, index: Int): String? =
        values.firstOrNull { it.first != null && it.first == startMs }?.second
            ?: values.getOrNull(index)?.second
            ?: startMs?.let { start -> values.filter { it.first != null }.minByOrNull { kotlin.math.abs(it.first!! - start) }?.second }

    private fun parseRoot(raw: String): JsonElement {
        val first = json.parseToJsonElement(raw.ifBlank { "null" })
        return if (first is JsonPrimitive && first.isString) {
            runCatching { json.parseToJsonElement(first.content) }.getOrDefault(first)
        } else {
            first
        }
    }

    private fun resultArray(root: JsonElement, vararg wrappers: String): JsonArray = when (root) {
        is JsonArray -> root
        is JsonObject -> wrappers.firstNotNullOfOrNull { root[it] as? JsonArray } ?: JsonArray(emptyList())
        is JsonNull -> JsonArray(emptyList())
        else -> throw IllegalArgumentException("Plugin result must be a JSON array or a supported wrapper object")
    }

    private fun JsonObject.fieldsMap(): Map<String, String> = (this["fields"] as? JsonObject)
        ?.mapNotNull { (key, value) -> value.primitiveString()?.let { key to it } }
        ?.toMap()
        .orEmpty()

    private fun JsonObject.first(vararg keys: String): JsonElement? = keys.firstNotNullOfOrNull { this[it] }
    private fun JsonObject.firstString(vararg keys: String): String? = first(*keys)?.primitiveString()
    private fun JsonObject.firstLong(vararg keys: String): Long? = first(*keys)?.primitiveLong()
    private fun JsonObject.firstInt(vararg keys: String): Int? = first(*keys)?.let { (it as? JsonPrimitive)?.intOrNull }
    private fun JsonElement.primitiveString(): String? = (this as? JsonPrimitive)?.contentOrNull
    private fun JsonElement.primitiveLong(): Long? = (this as? JsonPrimitive)?.longOrNull
}

class LyricoJsMetaSource(
    private val plugin: InstalledPlugin,
    private val runtimeManager: PluginRuntimeManager,
    private val configProvider: PluginConfigProvider,
    private val resultParser: PluginResultParser,
) : MetaSource {
    override val id = plugin.descriptor.pluginId
    override val displayName = plugin.descriptor.pluginName

    override suspend fun searchSongs(query: MetaSongQuery): List<MetaSongCandidate> =
        searchSongs(query, PluginLookupMode.MANUAL)

    suspend fun searchSongs(query: MetaSongQuery, mode: PluginLookupMode): List<MetaSongCandidate> {
        requireUsable("searchSongs", mode)
        val request = buildJsonObject {
            put("keyword", query.keyword ?: query.defaultKeyword())
            put("page", query.page.coerceAtLeast(1))
            put("pageSize", query.pageSize.coerceAtLeast(1))
            put("separator", query.separator.ifEmpty { "/" })
            put("config", configJson(query.config))
        }
        return resultParser.songs(id, call("searchSongs", request), query.separator)
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
                resultParser.internal(id, candidate.contextToken)?.let { put("internal", it) }
            })
            put("config", configJson(config))
        }
        return resultParser.lyrics(call("getLyrics", request))
    }

    override suspend fun searchCovers(query: MetaSongQuery): List<MetaCoverCandidate> =
        searchCovers(query, PluginLookupMode.MANUAL)

    suspend fun searchCovers(query: MetaSongQuery, mode: PluginLookupMode): List<MetaCoverCandidate> {
        requireUsable("searchCovers", mode)
        val request = buildJsonObject {
            put("keyword", query.keyword ?: query.defaultKeyword())
            put("pageSize", if (query.pageSize == 20) 5 else query.pageSize.coerceAtLeast(1))
            put("config", configJson(query.config))
        }
        return resultParser.covers(id, call("searchCovers", request))
    }

    suspend fun clearPrivateContexts() = resultParser.clearPlugin(id)

    private suspend fun call(name: String, request: JsonObject) =
        runtimeManager.call(plugin.descriptor, name, request.toString(), 15_000)

    private suspend fun configJson(overrides: Map<String, String>) = buildJsonObject {
        (configProvider.config(id) + overrides).forEach { (key, value) -> put(key, value) }
    }

    private fun requireUsable(capability: String, mode: PluginLookupMode) {
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
            throw PluginLookupDeniedException(id, mode, "Plugin $id does not declare $capability")
        }
    }

    private fun MetaSongQuery.defaultKeyword(): String = listOfNotNull(
        title.takeIf { it.isNotBlank() },
        artist?.takeIf { it.isNotBlank() },
        album?.takeIf { it.isNotBlank() },
    ).joinToString(" ")
}
