package com.github.tidetunes.source.storage

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.source.api.LegacyStorageKind
import com.github.tidetunes.source.api.LegacyStorageSearchProvider
import com.github.tidetunes.source.api.SourceMediaItem
import com.github.tidetunes.source.api.SourceSearchFailureReason
import com.github.tidetunes.source.api.SourceSearchResult
import com.github.tidetunes.source.api.legacyStorageTrackMediaId
import uniffi.tidetunes_core.ListStorageEntryChildrenResp
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageEntry
import uniffi.tidetunes_core.StorageId

internal fun interface LiveStorageLookup {
    suspend fun storageForRust(storageId: StorageId): Storage?
}

internal class LiveStorageSearchProvider(
    private val directoryLister: StorageDirectoryLister,
    private val storageLookup: LiveStorageLookup,
) : LegacyStorageSearchProvider {
    override suspend fun search(
        accountId: SourceAccountId,
        query: String,
        limit: Int,
        expectedStorageKind: LegacyStorageKind,
        sourceId: SourceId,
    ): SourceSearchResult {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank() || limit <= 0) {
            return SourceSearchResult.Success(emptyList())
        }

        val storageId = accountId.toLegacyStorageIdOrNull()
            ?: return SourceSearchResult.Failure(SourceSearchFailureReason.UnsupportedAccount)
        val storage = storageLookup.storageForRust(storageId)
            ?: return SourceSearchResult.Failure(SourceSearchFailureReason.UnsupportedAccount)
        val expectedStorageType = expectedStorageKind.toStorageType()
        if (storage.typ != expectedStorageType) {
            return SourceSearchResult.Failure(SourceSearchFailureReason.UnsupportedAccount)
        }

        val collected = mutableListOf<SourceMediaItem>()
        collectRecursive(
            storageId = storageId,
            path = "/",
            query = normalizedQuery,
            limit = limit,
            sourceId = sourceId,
            accountId = accountId,
            collected = collected,
        )

        return SourceSearchResult.Success(collected)
    }

    private suspend fun collectRecursive(
        storageId: StorageId,
        path: String,
        query: String,
        limit: Int,
        sourceId: SourceId,
        accountId: SourceAccountId,
        collected: MutableList<SourceMediaItem>,
    ) {
        if (collected.size >= limit) return

        val resp = directoryLister.listDirectory(storageId, path)
        if (resp !is ListStorageEntryChildrenResp.Ok) return

        val entries = resp.v1.sortedBy { it.isDir }

        for (entry in entries) {
            if (collected.size >= limit) break
            val lowerName = entry.name.lowercase()

            if (entry.isDir) {
                if (lowerName.startsWith(".")) continue
                collectRecursive(
                    storageId = storageId,
                    path = entry.path,
                    query = query,
                    limit = limit,
                    sourceId = sourceId,
                    accountId = accountId,
                    collected = collected,
                )
            } else if (lowerName.contains(query) && isMusicFile(entry)) {
                collected.add(
                    SourceMediaItem(
                        mediaId = legacyStorageTrackMediaId(
                            sourceId = sourceId,
                            accountId = accountId,
                            path = entry.path,
                        ),
                        accountId = accountId,
                        title = entry.name,
                        path = entry.path,
                    )
                )
            }
        }
    }

    private fun isMusicFile(entry: StorageEntry): Boolean {
        val lowerPath = entry.path.lowercase()
        return MUSIC_EXTENSIONS.any { lowerPath.endsWith(it) }
    }

    companion object {
        internal val MUSIC_EXTENSIONS = arrayOf(
            ".wav", ".mp3", ".aac", ".flac", ".ogg", ".oga", ".opus", ".m4a",
        )
    }
}
