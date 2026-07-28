package io.github.julystar.musicapp.source.storage

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.database.SourceTrackSearchRow
import io.github.julystar.musicapp.database.TrackDao
import io.github.julystar.musicapp.source.api.LegacyStorageKind
import io.github.julystar.musicapp.source.api.LegacyStorageSearchProvider
import io.github.julystar.musicapp.source.api.SourceMediaItem
import io.github.julystar.musicapp.source.api.SourceSearchFailureReason
import io.github.julystar.musicapp.source.api.SourceSearchResult
import io.github.julystar.musicapp.source.api.legacyStorageTrackMediaId
import uniffi.app_backend.Storage
import uniffi.app_backend.StorageId

class RoomLegacyStorageSearchProvider(
    private val storageLookup: suspend (StorageId) -> Storage?,
    private val trackDao: TrackDao,
) : LegacyStorageSearchProvider {
    override suspend fun search(
        accountId: SourceAccountId,
        query: String,
        limit: Int,
        expectedStorageKind: LegacyStorageKind,
        sourceId: SourceId,
    ): SourceSearchResult {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() || limit <= 0) {
            return SourceSearchResult.Success(emptyList())
        }

        val storageId = accountId.toLegacyStorageIdOrNull()
            ?: return SourceSearchResult.Failure(SourceSearchFailureReason.UnsupportedAccount)
        val storage = storageLookup(storageId)
            ?: return SourceSearchResult.Failure(SourceSearchFailureReason.UnsupportedAccount)
        val expectedStorageType = expectedStorageKind.toStorageType()
        if (storage.typ != expectedStorageType) {
            return SourceSearchResult.Failure(SourceSearchFailureReason.UnsupportedAccount)
        }

        val pattern = normalizedQuery.toSqlLikeLiteral()
        val rows = trackDao.searchBySourceStorage(
            sourceAccountId = storageId.value,
            query = normalizedQuery,
            prefixQuery = "$pattern%",
            containsQuery = "%$pattern%",
            limit = limit,
        )
        return SourceSearchResult.Success(
            rows.map { row ->
                row.toSourceMediaItem(
                    sourceId = sourceId,
                    accountId = accountId,
                )
            }
        )
    }
}

private fun SourceTrackSearchRow.toSourceMediaItem(
    sourceId: SourceId,
    accountId: SourceAccountId,
): SourceMediaItem {
    val track = track
    return SourceMediaItem(
        mediaId = legacyStorageTrackMediaId(
            sourceId = sourceId,
            accountId = accountId,
            path = resolvedSourcePath,
        ),
        accountId = accountId,
        title = track.title,
        artist = track.artist?.takeIf { it.isNotBlank() }
            ?: track.albumArtist?.takeIf { it.isNotBlank() }
            ?: track.composer?.takeIf { it.isNotBlank() },
        album = resolvedAlbum,
        durationMs = track.durationMs,
        path = resolvedSourcePath,
    )
}

private fun String.toSqlLikeLiteral(): String {
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
