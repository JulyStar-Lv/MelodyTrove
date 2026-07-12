package com.github.tidetunes.source.storage

import uniffi.tidetunes_backend.ListStorageEntryChildrenResp
import uniffi.tidetunes_backend.StorageId

internal interface StorageDirectoryLister {
    suspend fun listDirectory(storageId: StorageId, path: String): ListStorageEntryChildrenResp
}
