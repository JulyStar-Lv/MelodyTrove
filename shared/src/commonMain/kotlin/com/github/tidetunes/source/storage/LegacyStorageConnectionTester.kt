package com.github.tidetunes.source.storage

import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import uniffi.tidetunes_core.ArgUpsertStorage
import uniffi.tidetunes_core.StorageConnectionTestResult

fun interface LegacyStorageConnectionTester {
    suspend fun test(arg: ArgUpsertStorage): StorageConnectionTestResult
}

internal fun StorageConnectionTestResult.toSourceAuthResult(): SourceAuthResult {
    return when (this) {
        StorageConnectionTestResult.SUCCESS -> SourceAuthResult.Success
        StorageConnectionTestResult.TIMEOUT -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.Timeout)
        }
        StorageConnectionTestResult.UNAUTHORIZED -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.Unauthorized)
        }
        StorageConnectionTestResult.NONE,
        StorageConnectionTestResult.TESTING -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.Unavailable)
        }
        StorageConnectionTestResult.OTHER_ERROR -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.Unknown)
        }
    }
}
