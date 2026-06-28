package com.github.tidetunes.database

import uniffi.tidetunes_core.StorageEntry

fun RemoteFileEntity.hasSameRemoteContent(entry: StorageEntry): Boolean {
    if (isDeleted || storageId != entry.storageId.value || canonicalPath != entry.path) {
        return false
    }
    return hasSameRemoteRevision(entry)
}

fun RemoteFileEntity.hasSameRemoteRevision(entry: StorageEntry): Boolean {
    if (storageId != entry.storageId.value) {
        return false
    }
    val remoteSize = entry.size
    if (remoteSize == null || remoteSize > Long.MAX_VALUE.toULong() || size != remoteSize.toLong()) {
        return false
    }
    val remoteEtag = entry.etag
    if (!remoteEtag.isNullOrEmpty()) {
        return etag == remoteEtag
    }
    val remoteModifiedAt = entry.modifiedAt
    return remoteModifiedAt != null && modifiedAt == remoteModifiedAt
}
