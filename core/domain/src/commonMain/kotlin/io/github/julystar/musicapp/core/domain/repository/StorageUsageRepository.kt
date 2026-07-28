package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.StorageUsage

interface StorageUsageRepository {
    suspend fun loadUsage(): StorageUsage
    suspend fun clearAudioCache()
    suspend fun clearImageCache()
    suspend fun clearAllCaches()
    suspend fun clearAllStoredFiles()
    suspend fun enforceCacheLimits(audioLimitBytes: Long, imageLimitBytes: Long)
}
