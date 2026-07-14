package com.github.tidetunes.plugin.runtime

import com.github.tidetunes.plugin.currentTimeMillis
import kotlin.random.Random
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement

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
