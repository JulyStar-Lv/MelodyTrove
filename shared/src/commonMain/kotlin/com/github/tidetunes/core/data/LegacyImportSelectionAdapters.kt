package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.core.domain.model.toStorageRouteIdOrNull
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeSelection
import com.github.tidetunes.source.api.SourceNodeType
import com.github.tidetunes.source.api.legacyStorageArtworkMediaId
import com.github.tidetunes.source.storage.toBuiltInSourceId
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageEntry
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.StorageId

fun SourceNodeSelection.toLegacyStorageEntry(): StorageEntry? {
    val storageId = accountId.toLegacyStorageIdOrNull() ?: return null
    return StorageEntry(
        storageId = storageId,
        name = node.name,
        path = node.path,
        size = node.sizeBytes,
        isDir = node.type == SourceNodeType.Folder,
        remoteId = node.remoteId,
        parentRemoteId = node.parentNodeId,
        mimeType = node.mimeType,
        etag = node.etag,
        ctag = node.ctag,
        createdAt = node.createdAtEpochMs,
        modifiedAt = node.modifiedAtEpochMs,
    )
}

fun SourceNodeSelection.toLegacyStorageEntryLoc(): StorageEntryLoc? {
    val storageId = accountId.toLegacyStorageIdOrNull() ?: return null
    return StorageEntryLoc(
        storageId = storageId,
        path = node.path,
    )
}

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

private fun SourceAccountId.toLegacyStorageIdOrNull(): StorageId? {
    return toStorageRouteIdOrNull()?.let(::StorageId)
}

private val UNKNOWN_LEGACY_STORAGE_SOURCE_ID = SourceId("legacy-storage")
