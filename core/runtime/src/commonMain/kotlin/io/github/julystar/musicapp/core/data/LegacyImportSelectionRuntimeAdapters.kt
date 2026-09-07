package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.toStorageRouteIdOrNull
import io.github.julystar.musicapp.source.api.SourceNodeSelection
import io.github.julystar.musicapp.source.api.SourceNodeType
import uniffi.app_backend.StorageEntry
import uniffi.app_backend.StorageEntryLoc
import uniffi.app_backend.StorageId

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
    return StorageEntryLoc(storageId = storageId, path = node.path)
}

private fun SourceAccountId.toLegacyStorageIdOrNull(): StorageId? =
    toStorageRouteIdOrNull()?.let(::StorageId)
