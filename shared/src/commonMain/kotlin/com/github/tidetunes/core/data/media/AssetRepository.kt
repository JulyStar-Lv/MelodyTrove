package com.github.tidetunes.core.data.media

import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.core.data.StorageRepositoryImpl

import androidx.compose.ui.graphics.ImageBitmap
import com.github.tidetunes.core.DataSourceKeyH
import com.github.tidetunes.platform.byteArrayToImageBitmap
import uniffi.tidetunes_core.AssetStream
import uniffi.tidetunes_core.ctGetAsset
import uniffi.tidetunes_core.ctGetAssetStream
import uniffi.tidetunes_core.tidetunesError
import uniffi.tidetunes_core.DataSourceKey
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.StorageEntryLoc

class AssetRepository(
    private val bridge: Bridge,
    private val storageRepository: StorageRepositoryImpl,
    private val roomLibraryStore: RoomLibraryStore,
) {
    private val bufCache = HashMap<DataSourceKeyH, ByteArray>()
    private val bitmapCache = HashMap<DataSourceKeyH, ImageBitmap>()

    suspend fun load(key: DataSourceKey): ByteArray? {
        val keyH = DataSourceKeyH(key)
        bufCache[keyH]?.let {
            return it
        }

        return try {
            val loc = resolveLoc(key) ?: return null
            val storage = storageRepository.storageForRust(loc.storageId) ?: return null
            val buf = bridge.run { ctGetAsset(it, storage, loc) }
            if (buf != null) {
                bufCache[keyH] = buf
            }
            buf
        } catch (e: Exception) {
            tidetunesError(e.toString())
            null
        }
    }

    suspend fun loadBitmap(key: DataSourceKey): ImageBitmap? {
        val keyH = DataSourceKeyH(key)
        bitmapCache[keyH]?.let {
            return it
        }

        val buf = load(key) ?: return null
        val bitmap = byteArrayToImageBitmap(buf) ?: return null
        bitmapCache[keyH] = bitmap
        return bitmap
    }

    fun get(key: DataSourceKey): ByteArray? {
        return bufCache[DataSourceKeyH(key)]
    }

    fun getBitmap(key: DataSourceKey): ImageBitmap? {
        return bitmapCache[DataSourceKeyH(key)]
    }

    suspend fun openMusicStream(id: MusicId, byteOffset: ULong): AssetStream? {
        val loc = roomLibraryStore.resolveTrackLoc(id) ?: return null
        val storage = storageRepository.storageForRust(loc.storageId) ?: return null
        return bridge.run {
            ctGetAssetStream(it, storage, loc, byteOffset)
        }
    }

    private suspend fun resolveLoc(key: DataSourceKey): StorageEntryLoc? {
        return when (key) {
            is DataSourceKey.AnyEntry -> key.entry
            is DataSourceKey.Music -> roomLibraryStore.resolveTrackLoc(key.id)
            is DataSourceKey.Cover -> null
        }
    }
}
