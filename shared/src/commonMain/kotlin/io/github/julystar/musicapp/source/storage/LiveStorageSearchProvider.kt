package io.github.julystar.musicapp.source.storage

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.source.api.LegacyStorageKind
import io.github.julystar.musicapp.source.api.LegacyStorageSearchProvider
import io.github.julystar.musicapp.source.api.SourceMediaItem
import io.github.julystar.musicapp.source.api.SourceSearchFailureReason
import io.github.julystar.musicapp.source.api.SourceSearchResult
import io.github.julystar.musicapp.source.api.legacyStorageTrackMediaId
import uniffi.app_backend.ListStorageEntryChildrenResp
import uniffi.app_backend.Storage
import uniffi.app_backend.StorageEntry
import uniffi.app_backend.StorageId

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
        if (
            entry.mimeType
                ?.substringBefore(';')
                ?.trim()
                ?.lowercase()
                ?.startsWith("video/") == true
        ) {
            return false
        }
        val lowerPath = entry.path.lowercase()
        return MUSIC_EXTENSIONS.any { lowerPath.endsWith(it) }
    }

    companion object {
        internal val MUSIC_EXTENSIONS = arrayOf(
            ".wav", ".mp3", ".aac", ".flac", ".ogg", ".oga", ".opus", ".m4a",
            ".mp4", ".ape", ".wv", ".aif", ".aiff",
        )
    }
}
