package com.github.tidetunes.feature.search.data

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.feature.search.domain.LOCAL_LIBRARY_SOURCE_LABEL
import com.github.tidetunes.feature.search.domain.SearchRepository
import com.github.tidetunes.feature.search.domain.SearchResults
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.storage.toLegacyStorageTrackMediaIdOrNull

class RoomSearchRepository(
    private val trackDao: TrackDao,
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

        val pattern = normalizedQuery.toSqlLikeLiteral()
        val tracks = trackDao.search(
            query = normalizedQuery,
            prefixQuery = "$pattern%",
            containsQuery = "%$pattern%",
            limit = limit,
        )
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

        val pattern = normalizedQuery.toSqlLikeLiteral()
        return trackDao.searchSuggestions(
            query = normalizedQuery,
            prefixQuery = "$pattern%",
            containsQuery = "%$pattern%",
            limit = limit,
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(limit)
    }
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
