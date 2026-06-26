package com.github.tidetune.singleton

import uniffi.tidetune_core.ListStorageEntryChildrenResp
import uniffi.tidetune_core.OneDriveDeltaPageResult
import uniffi.tidetune_core.OneDriveDeltaRequest
import uniffi.tidetune_core.RemoteMusicScanSession
import uniffi.tidetune_core.StorageEntryLoc
import uniffi.tidetune_core.StorageId
import uniffi.tidetune_core.ctListStorageEntryChildren
import uniffi.tidetune_core.ctGetOnedriveDeltaPage
import uniffi.tidetune_core.ctScanStorageMusicFolder
import uniffi.tidetune_core.ctStartStorageMusicScan

class RemoteScannerRepository(
    private val bridge: Bridge,
    private val storageRepository: StorageRepository,
) {
    suspend fun listDirectory(
        storageId: StorageId,
        path: String,
    ): ListStorageEntryChildrenResp {
        val storage = storageRepository.storageForRust(storageId)
            ?: return ListStorageEntryChildrenResp.Unknown
        return bridge.runRaw {
            ctListStorageEntryChildren(
                it,
                storage,
                StorageEntryLoc(
                    storageId = storageId,
                    path = path,
                ),
            )
        }
    }

    suspend fun scanMusicFolder(
        storageId: StorageId,
        path: String,
    ): ListStorageEntryChildrenResp {
        val storage = storageRepository.storageForRust(storageId)
            ?: return ListStorageEntryChildrenResp.Unknown
        return bridge.runRaw {
            ctScanStorageMusicFolder(
                it,
                storage,
                StorageEntryLoc(
                    storageId = storageId,
                    path = path,
                ),
            )
        }
    }

    suspend fun startMusicFolderScan(
        storageId: StorageId,
        path: String,
    ): RemoteMusicScanSession {
        val storage = storageRepository.storageForRust(storageId)
            ?: error("Storage ${storageId.value} is no longer available")
        return bridge.runRaw {
            ctStartStorageMusicScan(
                it,
                storage,
                StorageEntryLoc(
                    storageId = storageId,
                    path = path,
                ),
            )
        }
    }

    suspend fun getOneDriveDeltaPage(
        storageId: StorageId,
        rootRemoteId: String,
        cursor: String?,
        latestOnly: Boolean,
    ): OneDriveDeltaPageResult {
        val storage = storageRepository.storageForRust(storageId)
            ?: return OneDriveDeltaPageResult.ResyncRequired
        return bridge.runRaw {
            ctGetOnedriveDeltaPage(
                it,
                storage,
                OneDriveDeltaRequest(
                    storageId = storageId,
                    rootRemoteId = rootRemoteId,
                    cursor = cursor,
                    latestOnly = latestOnly,
                ),
            )
        }
    }
}
