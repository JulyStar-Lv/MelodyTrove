package com.github.tidetunes.source.storage

import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.MetadataScanOptions
import com.github.tidetunes.core.domain.model.toOptions
import com.github.tidetunes.singleton.Bridge

import uniffi.tidetunes_backend.MetadataReadOptions
import uniffi.tidetunes_backend.RemoteMetadata
import uniffi.tidetunes_backend.RemoteMetadataRequest
import uniffi.tidetunes_backend.RemoteMetadataResult
import uniffi.tidetunes_backend.StorageEntry
import uniffi.tidetunes_backend.StorageEntryLoc
import uniffi.tidetunes_backend.ctReadRemoteMetadata
import uniffi.tidetunes_backend.ctReadRemoteMetadataBatch

interface RemoteMetadataReader {
    suspend fun read(
        entry: StorageEntry,
        options: MetadataScanOptions = MetadataScanMode.Full.toOptions(),
    ): RemoteMetadata?

    suspend fun readBatch(
        entries: List<StorageEntry>,
        concurrency: UInt = 4u,
        options: MetadataScanOptions = MetadataScanMode.Full.toOptions(),
    ): List<RemoteMetadataResult>
}

class MetadataRepository(
    private val bridge: Bridge,
    private val storageRepository: StorageRepositoryImpl,
) : RemoteMetadataReader {
    override suspend fun read(
        entry: StorageEntry,
        options: MetadataScanOptions,
    ): RemoteMetadata? {
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
                options = options.toRustOptions(),
            )
        }
    }

    override suspend fun readBatch(
        entries: List<StorageEntry>,
        concurrency: UInt,
        options: MetadataScanOptions,
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
                options = options.toRustOptions(),
                concurrency = concurrency,
            )
        }
    }
}

internal fun MetadataScanOptions.toRustOptions(): MetadataReadOptions {
    return MetadataReadOptions(
        readArtwork = readArtwork,
        readLyrics = readLyrics,
        readRawMetadata = readRawMetadata,
    )
}
