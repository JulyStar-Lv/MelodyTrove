package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.StorageUsage

interface StorageUsageRepository {
    suspend fun loadUsage(): StorageUsage
    suspend fun clearAudioCache()
    suspend fun clearImageCache()
}
