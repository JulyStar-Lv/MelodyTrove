package com.github.tidetunes.plugin.runtime

import com.github.tidetunes.plugin.currentTimeMillis
import com.github.tidetunes.source.api.MetaCoverCandidate
import com.github.tidetunes.source.api.MetaLyricLine
import com.github.tidetunes.source.api.MetaLyricWord
import com.github.tidetunes.source.api.MetaLyrics
import com.github.tidetunes.source.api.MetaSongCandidate
import com.github.tidetunes.source.api.MetaSongQuery
import com.github.tidetunes.source.api.MetaSource
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
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

class PluginResultParseException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

fun interface PluginConfigProvider {
    suspend fun config(pluginId: String): Map<String, String>
}

class PluginCandidateContextStore(
    private val ttlMs: Long = 15 * 60 * 1000,
    private val maxEntries: Int = 2_000,
) {
    init {
        require(ttlMs > 0) { "Context TTL must be positive" }
        require(maxEntries > 0) { "Context capacity must be positive" }
    }

    private data class Entry(
        val pluginId: String,
        val expiresAt: Long,
        val value: JsonElement,
    )

    private val mutex = Mutex()
    private val entries = linkedMapOf<String, Entry>()

    suspend fun put(pluginId: String, value: JsonElement): String = mutex.withLock {
        sweepLocked()
        while (entries.size >= maxEntries) {
            entries.remove(entries.keys.first())
        }
        var token: String
        do {
            token = buildString {
                append("ctx-")
                append(currentTimeMillis().toString(36))
                append('-')
                repeat(4) {
                    append(Random.nextLong().toULong().toString(36))
                    append('-')
                }
            }.trimEnd('-')
        } while (entries.containsKey(token))
        entries[token] = Entry(
            pluginId = pluginId,
            expiresAt = currentTimeMillis() + ttlMs,
            value = value,
        )
        token
    }

    suspend fun get(pluginId: String, token: String?): JsonElement? = mutex.withLock {
        sweepLocked()
        token
            ?.let(entries::get)
            ?.takeIf { it.pluginId == pluginId }
            ?.value
    }

    suspend fun clearPlugin(pluginId: String) = mutex.withLock {
        entries.entries.removeAll { it.value.pluginId == pluginId }
    }

    suspend fun clearAll() = mutex.withLock {
        entries.clear()
    }

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
    ): List<MetaSongCandidate> {
        val actualSeparator = separator.ifEmpty { "/" }
        return resultArray(
            root = parseRoot(raw),
            resultName = "song search",
            wrappers = arrayOf("items", "results", "songs", "data"),
        ).mapNotNull { value ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            val id = obj.firstString("id", "songId", "trackId")?.trim().orEmpty()
            val title = obj.firstString("title", "name", "songName")?.trim().orEmpty()
            if (id.isEmpty() || title.isEmpty()) return@mapNotNull null

            val artist = when (val artistValue = obj.first("artist", "artists", "singer")) {
                is JsonArray -> artistValue
                    .mapNotNull(JsonElement::primitiveString)
                    .filter(String::isNotBlank)
                    .joinToString(actualSeparator)
                    .ifBlank { null }
                else -> artistValue?.primitiveString()?.takeIf(String::isNotBlank)
            }
            MetaSongCandidate(
                id = id,
                title = title,
                artist = artist,
                album = obj.firstString("album", "albumName"),
                durationMs = obj.firstLong("duration", "durationMs", "duration_ms"),
                date = obj.firstString("date", "releaseDate", "release_date"),
                trackNumber = obj.firstString("trackNumber", "trackerNumber", "track_number"),
                pictureUrl = obj.firstString(
                    "picUrl",
                    "coverUrl",
                    "cover_url",
                    "artworkUrl",
                    "pictureUrl",
                ),
                fields = obj.fieldsMap(),
                contextToken = obj["internal"]
                    ?.takeUnless { it is JsonNull }
                    ?.let { contexts.put(pluginId, it) },
                sourceId = pluginId,
            )
        }
    }

    fun covers(
        pluginId: String,
        raw: String,
    ): List<MetaCoverCandidate> = resultArray(
        root = parseRoot(raw),
        resultName = "cover search",
        wrappers = arrayOf("items", "results", "songs", "data", "covers"),
    ).mapNotNull { value ->
        when (value) {
            is JsonPrimitive -> value.contentOrNull
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { url -> MetaCoverCandidate(url = url, sourceId = pluginId) }
            is JsonObject -> value.firstString(
                "url",
                "picUrl",
                "coverUrl",
                "cover_url",
                "artworkUrl",
                "pictureUrl",
            )?.trim()?.takeIf(String::isNotEmpty)?.let { url ->
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
        val element = parseRoot(raw)
        if (element is JsonNull) return null
        val root = element as? JsonObject
            ?: throw PluginResultParseException("Lyrics result must be a JSON object or null")
        if (root.firstBoolean("notFound") == true) return null

        val legacyLines = root["lines"] as? JsonArray
        if (legacyLines != null) {
            return parseLegacyLyrics(root, legacyLines)
        }

        return when (root.firstString("type") ?: "structured") {
            "structured" -> parseStructuredLyrics(root)
            "rawPlainLrc" -> MetaLyrics(
                rawPlainLrc = root.firstString("rawPlainLrc", "content", "lyrics"),
            )
            "rawVerbatimLrc" -> MetaLyrics(
                rawVerbatimLrc = root.firstString("rawVerbatimLrc", "content", "lyrics"),
            )
            "rawEnhancedLrc" -> MetaLyrics(
                rawEnhancedLrc = root.firstString("rawEnhancedLrc", "content", "lyrics"),
            )
            "rawTtml" -> MetaLyrics(
                rawTtml = root.firstString("rawTtml", "content", "lyrics"),
            )
            "rawMultiPersonEnhancedLrc" -> MetaLyrics(
                rawMultiPersonEnhancedLrc = root.firstString(
                    "rawMultiPersonEnhancedLrc",
                    "content",
                    "lyrics",
                ),
            )
            else -> parseUnknownRawLyrics(root)
        }
    }

    suspend fun internal(pluginId: String, token: String?): JsonElement? =
        contexts.get(pluginId, token)

    suspend fun clearPlugin(pluginId: String) {
        contexts.clearPlugin(pluginId)
    }

    private fun parseStructuredLyrics(root: JsonObject): MetaLyrics {
        val translated = timedTexts(root["translated"] as? JsonArray)
        val romanized = timedTexts(root["romanization"] as? JsonArray)
        val original = root["original"] as? JsonArray ?: JsonArray(emptyList())
        val lines = original.mapIndexedNotNull { index, element ->
            val tuple = element as? JsonArray ?: return@mapIndexedNotNull null
            val start = tuple.getOrNull(0)?.primitiveLong()
            val end = tuple.getOrNull(1)?.primitiveLong()
            val payload = tuple.getOrNull(2) ?: return@mapIndexedNotNull null
            val words = (payload as? JsonArray).orEmpty().mapNotNull { wordValue ->
                val word = wordValue as? JsonArray ?: return@mapNotNull null
                val text = word.getOrNull(2)?.primitiveString() ?: return@mapNotNull null
                MetaLyricWord(
                    text = text,
                    startMs = word.getOrNull(0)?.primitiveLong(),
                    endMs = word.getOrNull(1)?.primitiveLong(),
                )
            }
            val text = if (payload is JsonArray) {
                words.joinToString(separator = "", transform = MetaLyricWord::text)
            } else {
                payload.primitiveString().orEmpty()
            }
            if (text.isEmpty() && words.isEmpty()) return@mapIndexedNotNull null
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

    private fun parseLegacyLyrics(
        root: JsonObject,
        lineValues: JsonArray,
    ): MetaLyrics = MetaLyrics(
        lines = lineValues.mapNotNull { it as? JsonObject }.mapNotNull { line ->
            val text = line.firstString("text") ?: return@mapNotNull null
            MetaLyricLine(
                text = text,
                startMs = line.firstLong("startMs", "start"),
                endMs = line.firstLong("endMs", "end"),
                words = (line["words"] as? JsonArray).orEmpty()
                    .mapNotNull { it as? JsonObject }
                    .mapNotNull { word ->
                        word.firstString("text")?.let { wordText ->
                            MetaLyricWord(
                                text = wordText,
                                startMs = word.firstLong("startMs", "start"),
                                endMs = word.firstLong("endMs", "end"),
                            )
                        }
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

    private fun parseUnknownRawLyrics(root: JsonObject): MetaLyrics {
        val lyrics = MetaLyrics(
            rawPlainLrc = root.firstString("rawPlainLrc"),
            rawVerbatimLrc = root.firstString("rawVerbatimLrc"),
            rawEnhancedLrc = root.firstString("rawEnhancedLrc"),
            rawTtml = root.firstString("rawTtml"),
            rawMultiPersonEnhancedLrc = root.firstString("rawMultiPersonEnhancedLrc"),
        )
        if (
            lyrics.rawPlainLrc == null &&
            lyrics.rawVerbatimLrc == null &&
            lyrics.rawEnhancedLrc == null &&
            lyrics.rawTtml == null &&
            lyrics.rawMultiPersonEnhancedLrc == null
        ) {
            throw PluginResultParseException(
                "Unsupported lyrics result type: ${root.firstString("type").orEmpty()}",
            )
        }
        return lyrics
    }

    private fun timedTexts(array: JsonArray?): List<TimedText> = array.orEmpty().mapNotNull { value ->
        val tuple = value as? JsonArray ?: return@mapNotNull null
        val text = tuple.getOrNull(2)?.primitiveString() ?: return@mapNotNull null
        TimedText(
            startMs = tuple.getOrNull(0)?.primitiveLong(),
            text = text,
        )
    }

    private fun matchTimedText(
        values: List<TimedText>,
        startMs: Long?,
        index: Int,
    ): String? = values.firstOrNull { value ->
        value.startMs != null && value.startMs == startMs
    }?.text
        ?: values.getOrNull(index)?.text
        ?: startMs?.let { start ->
            values
                .filter { it.startMs != null }
                .minByOrNull { value -> abs(value.startMs!! - start) }
                ?.text
        }

    private fun parseRoot(raw: String): JsonElement {
        val normalized = raw.ifBlank { "null" }
        val first = try {
            json.parseToJsonElement(normalized)
        } catch (error: SerializationException) {
            throw PluginResultParseException("Plugin returned invalid JSON", error)
        }
        if (first is JsonPrimitive && first.isString) {
            return try {
                json.parseToJsonElement(first.content)
            } catch (_: SerializationException) {
                first
            }
        }
        return first
    }

    private fun resultArray(
        root: JsonElement,
        resultName: String,
        wrappers: Array<out String>,
    ): JsonArray = when (root) {
        is JsonArray -> root
        is JsonObject -> wrappers
            .firstNotNullOfOrNull { key -> root[key] as? JsonArray }
            ?: throw PluginResultParseException(
                "$resultName result object must contain one of: ${wrappers.joinToString()}",
            )
        is JsonNull -> JsonArray(emptyList())
        else -> throw PluginResultParseException(
            "$resultName result must be a JSON array, null, or a supported wrapper object",
        )
    }

    private fun JsonObject.fieldsMap(): Map<String, String> =
        (this["fields"] as? JsonObject)
            ?.mapNotNull { (key, value) ->
                value.primitiveString()?.let { key to it }
            }
            ?.toMap()
            .orEmpty()

    private fun JsonObject.first(vararg keys: String): JsonElement? =
        keys.firstNotNullOfOrNull { key -> this[key] }

    private fun JsonObject.firstString(vararg keys: String): String? =
        first(*keys)?.primitiveString()

    private fun JsonObject.firstLong(vararg keys: String): Long? =
        first(*keys)?.primitiveLong()

    private fun JsonObject.firstInt(vararg keys: String): Int? =
        first(*keys)?.let { value -> (value as? JsonPrimitive)?.intOrNull }

    private fun JsonObject.firstBoolean(vararg keys: String): Boolean? =
        first(*keys)?.primitiveString()?.toBooleanStrictOrNull()

    private fun JsonElement.primitiveString(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement.primitiveLong(): Long? =
        (this as? JsonPrimitive)?.longOrNull

    private data class TimedText(
        val startMs: Long?,
        val text: String,
    )
}

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
    ): String = runtimeManager.call(
        plugin = plugin.descriptor,
        functionName = name,
        requestJson = request.toString(),
    )

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
