package com.github.tidetunes.source.storage

import uniffi.tidetunes_core.ListStorageEntryChildrenResp
import uniffi.tidetunes_core.StorageId

internal interface StorageDirectoryLister {
    suspend fun listDirectory(storageId: StorageId, path: String): ListStorageEntryChildrenResp
}
