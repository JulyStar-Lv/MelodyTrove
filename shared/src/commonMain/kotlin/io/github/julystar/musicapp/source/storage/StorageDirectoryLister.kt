package io.github.julystar.musicapp.source.storage

import uniffi.app_backend.ListStorageEntryChildrenResp
import uniffi.app_backend.StorageId

internal interface StorageDirectoryLister {
    suspend fun listDirectory(storageId: StorageId, path: String): ListStorageEntryChildrenResp
}
