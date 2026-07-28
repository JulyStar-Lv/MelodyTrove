package io.github.julystar.musicapp.source.storage

import io.github.julystar.musicapp.core.data.StorageRepositoryImpl
import io.github.julystar.musicapp.core.domain.model.MetadataScanMode
import io.github.julystar.musicapp.core.domain.model.MetadataScanOptions
import io.github.julystar.musicapp.core.domain.model.toOptions
import io.github.julystar.musicapp.singleton.Bridge

import uniffi.app_backend.MetadataReadOptions
import uniffi.app_backend.RemoteMetadata
import uniffi.app_backend.RemoteMetadataRequest
import uniffi.app_backend.RemoteMetadataResult
import uniffi.app_backend.StorageEntry
import uniffi.app_backend.StorageEntryLoc
import uniffi.app_backend.ctReadRemoteMetadata
import uniffi.app_backend.ctReadRemoteMetadataBatch

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
