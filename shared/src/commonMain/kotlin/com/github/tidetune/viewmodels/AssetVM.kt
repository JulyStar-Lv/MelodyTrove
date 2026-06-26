package com.github.tidetune.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import com.github.tidetune.singleton.AssetRepository
import uniffi.tidetune_core.DataSourceKey


class AssetVM constructor(
    private val assetRepository: AssetRepository
) : ViewModel() {

    suspend fun load(key: DataSourceKey): ByteArray? {
        return assetRepository.load(key)
    }
    suspend fun loadBitmap(key: DataSourceKey): ImageBitmap? {
        return assetRepository.loadBitmap(key)
    }
    fun get(key: DataSourceKey): ByteArray? {
        return assetRepository.get(key)
    }
    fun getBitmap(key: DataSourceKey): ImageBitmap? {
        return assetRepository.getBitmap(key)
    }
}
