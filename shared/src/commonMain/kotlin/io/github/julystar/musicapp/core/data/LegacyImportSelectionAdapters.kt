package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.core.domain.model.storageSourceAccountId
import io.github.julystar.musicapp.source.api.SourceNode
import io.github.julystar.musicapp.source.api.SourceNodeSelection
import io.github.julystar.musicapp.source.api.SourceNodeType
import io.github.julystar.musicapp.source.api.legacyStorageArtworkMediaId
import io.github.julystar.musicapp.source.storage.toBuiltInSourceId
import uniffi.app_backend.Storage
import uniffi.app_backend.StorageEntryLoc

fun StorageEntryLoc.toSourceNodeSelection(storages: List<Storage>): SourceNodeSelection? {
    if (path.isBlank()) return null
    val storage = storages.firstOrNull { storage -> storage.id == storageId }
    val accountId = storageSourceAccountId(storageId.value)
    return SourceNodeSelection(
        sourceId = storage?.typ?.toBuiltInSourceId() ?: UNKNOWN_LEGACY_STORAGE_SOURCE_ID,
        accountId = accountId,
        node = SourceNode(
            accountId = accountId,
            nodeId = path,
            name = path.substringAfterLast('/').ifBlank { path },
            path = path,
            type = SourceNodeType.Image,
        ),
    )
}

fun SourceNodeSelection.toLegacyStorageArtwork(): Artwork? {
    if (node.type != SourceNodeType.Image) return null
    if (sourceId == UNKNOWN_LEGACY_STORAGE_SOURCE_ID) return null
    return Artwork.SourceMedia(
        legacyStorageArtworkMediaId(
            sourceId = sourceId,
            accountId = accountId,
            path = node.path,
        )
    )
}

private val UNKNOWN_LEGACY_STORAGE_SOURCE_ID = SourceId("legacy-storage")
