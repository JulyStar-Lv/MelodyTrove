package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.StorageUsage

interface StorageUsageRepository {
    suspend fun loadUsage(): StorageUsage
    suspend fun clearAudioCache()
    suspend fun clearImageCache()
    suspend fun clearAllCaches()
    suspend fun enforceCacheLimits(audioLimitBytes: Long, imageLimitBytes: Long)
}
