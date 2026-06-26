package com.github.tidetune.singleton

import uniffi.tidetune_core.RemoteMetadata
import uniffi.tidetune_core.RemoteMetadataRequest
import uniffi.tidetune_core.RemoteMetadataResult
import uniffi.tidetune_core.StorageEntry
import uniffi.tidetune_core.StorageEntryLoc
import uniffi.tidetune_core.ctReadRemoteMetadata
import uniffi.tidetune_core.ctReadRemoteMetadataBatch

class MetadataRepository(
    private val bridge: Bridge,
    private val storageRepository: StorageRepository,
) {
    suspend fun read(entry: StorageEntry): RemoteMetadata? {
        val size = entry.size ?: return null
        if (entry.isDir || size == 0uL) return null
        val storage = storageRepository.storageForRust(entry.storageId) ?: return null
        return bridge.run {
            ctReadRemoteMetadata(
                backend = it,
                storage = storage,
                entry = StorageEntryLoc(
                    storageId = entry.storageId,
                    path = entry.path,
                ),
                size = size,
            )
        }
    }

    suspend fun readBatch(
        entries: List<StorageEntry>,
        concurrency: UInt = 4u,
    ): List<RemoteMetadataResult> {
        require(concurrency in 1u..16u) {
            "metadata concurrency must be between 1 and 16"
        }
        val requests = entries.mapNotNull { entry ->
            val size = entry.size
            if (entry.isDir || size == null || size == 0uL) {
                null
            } else {
                RemoteMetadataRequest(
                    entry = StorageEntryLoc(
                        storageId = entry.storageId,
                        path = entry.path,
                    ),
                    size = size,
                )
            }
        }
        if (requests.isEmpty()) return emptyList()
        val storageId = requests.first().entry.storageId
        val storage = storageRepository.storageForRust(storageId) ?: return emptyList()

        return bridge.runRaw {
            ctReadRemoteMetadataBatch(
                backend = it,
                storage = storage,
                requests = requests,
                concurrency = concurrency,
            )
        }
    }
}
