package com.github.tidetunes.feature.search.data

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.database.TrackFtsDao
import com.github.tidetunes.feature.search.domain.LOCAL_LIBRARY_SOURCE_LABEL
import com.github.tidetunes.feature.search.domain.SearchRepository
import com.github.tidetunes.feature.search.domain.SearchResults
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.storage.toLegacyStorageTrackMediaIdOrNull

class RoomSearchRepository(
    private val trackDao: TrackDao,
    private val trackFtsDao: TrackFtsDao,
    private val storageLookup: LegacyStorageLookup,
) : SearchRepository {
    override suspend fun searchLocalLibrary(
        query: String,
        limit: Int,
    ): SearchResults {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() || limit <= 0) {
            return SearchResults.Empty
        }

        // Use FTS for multi-word or longer queries that don't contain
        // special characters that FTS tokenization would lose
        val tracks = if (canUseFts(normalizedQuery)) {
            tryFtsSearch(normalizedQuery, limit)
        } else {
            null
        } ?: likeBasedSearch(normalizedQuery, limit)

        return SearchResults(
            tracks = tracks.map { track ->
                track.toSearchTrackItem(
                    mediaId = track.toLegacyStorageTrackMediaIdOrNull(storageLookup),
                )
            }
        )
    }

    override suspend fun suggestLocalLibrary(
        query: String,
        limit: Int,
    ): List<String> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() || limit <= 0) {
            return emptyList()
        }

        val ftsSuggestions = if (canUseFts(normalizedQuery)) {
            tryFtsSuggestions(normalizedQuery, limit)
        } else {
            null
        }
        val likeResults = likeBasedSuggestions(normalizedQuery, limit)

        return when {
            ftsSuggestions.isNullOrEmpty() -> likeResults
            likeResults.isNotEmpty() -> {
                val ftsSet = ftsSuggestions.toSet()
                val combined = ftsSuggestions.toMutableList()
                for (s in likeResults) {
                    if (s !in ftsSet) {
                        combined.add(s)
                    }
                }
                combined.take(limit)
            }
            else -> ftsSuggestions
        }
    }

    /**
     * Returns true if the query is suitable for FTS.
     * FTS simple tokenizer splits on characters outside [a-zA-Z0-9],
     * so queries containing such characters would lose precision.
     * We only use FTS for alphanumeric queries with length >= 3
     * or multi-word queries.
     */
    private fun canUseFts(query: String): Boolean {
        if (query.length < 3 && !query.contains(" ")) return false
        // Don't use FTS if query contains token-separator characters
        // that FTS simple tokenizer would strip
        val hasFtsTokenSeparators = query.any { ch ->
            ch !in 'a'..'z' && ch !in 'A'..'Z' && ch !in '0'..'9' && ch != ' '
        }
        return !hasFtsTokenSeparators
    }

    private suspend fun likeBasedSuggestions(query: String, limit: Int): List<String> {
        val pattern = query.toSqlLikeLiteral()
        return trackDao.searchSuggestions(
            query = query,
            prefixQuery = "$pattern%",
            containsQuery = "%$pattern%",
            limit = limit,
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(limit)
    }

    private suspend fun tryFtsSearch(query: String, limit: Int): List<TrackEntity>? {
        return try {
            val ftsQuery = query.toSimpleFtsQuery()
            trackFtsDao.searchFtsExcludingDeleted(ftsQuery, limit)
                .takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun tryFtsSuggestions(query: String, limit: Int): List<String>? {
        return try {
            val ftsQuery = query.toSimpleFtsQuery()
            trackFtsDao.searchFtsSuggestions(ftsQuery, limit)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(limit)
                .takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun likeBasedSearch(query: String, limit: Int): List<TrackEntity> {
        val pattern = query.toSqlLikeLiteral()
        return trackDao.search(
            query = query,
            prefixQuery = "$pattern%",
            containsQuery = "%$pattern%",
            limit = limit,
        )
    }
}

/**
 * Convert a user query to a simple FTS4 MATCH query.
 * Splits words and appends '*' to each for prefix matching.
 */
internal fun String.toSimpleFtsQuery(): String {
    val cleaned = this.trim()
    if (cleaned.isBlank()) return "\"\""

    val words = cleaned.split("\\s+".toRegex())
        .filter { it.isNotBlank() }
        .take(10)

    return words.joinToString(" ") { word ->
        if (word.isBlank()) ""
        else "$word*"
    }.trim()
}

internal fun String.toSqlLikeLiteral(): String {
    return buildString(length) {
        this@toSqlLikeLiteral.forEach { char ->
            when (char) {
                '\\', '%', '_' -> {
                    append('\\')
                    append(char)
                }
                else -> append(char)
            }
        }
    }
}

private fun TrackEntity.toSearchTrackItem(
    mediaId: MediaId? = null,
): SearchTrackItem {
    return SearchTrackItem(
        id = id,
        title = title,
        artist = artist?.takeIf { it.isNotBlank() }
            ?: albumArtist?.takeIf { it.isNotBlank() }
            ?: composer?.takeIf { it.isNotBlank() },
        durationMs = durationMs,
        mediaId = mediaId,
        sourceLabel = LOCAL_LIBRARY_SOURCE_LABEL,
    )
}
