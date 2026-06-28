package com.github.tidetunes.source.storage

import uniffi.tidetunes_core.ListStorageEntryChildrenResp
import uniffi.tidetunes_core.OneDriveDeltaPageResult
import uniffi.tidetunes_core.OneDriveDeltaRequest
import uniffi.tidetunes_core.RemoteMusicScanSession
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.ctListStorageEntryChildren
import uniffi.tidetunes_core.ctGetOnedriveDeltaPage
import uniffi.tidetunes_core.ctScanStorageMusicFolder
import uniffi.tidetunes_core.ctStartStorageMusicScan
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.core.data.StorageRepositoryImpl

class RemoteScannerRepository(
    private val bridge: Bridge,
    private val storageRepository: StorageRepositoryImpl,
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
