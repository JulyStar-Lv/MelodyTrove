package io.github.julystar.musicapp.source.storage

import io.github.julystar.musicapp.source.api.LegacyStorageConnectionRequest
import io.github.julystar.musicapp.source.api.SourceAuthFailureReason
import io.github.julystar.musicapp.source.api.SourceAuthResult
import uniffi.app_backend.ArgUpsertStorage
import uniffi.app_backend.StorageConnectionTestResult

internal fun LegacyStorageConnectionRequest.toArgUpsertStorage(): ArgUpsertStorage {
    return ArgUpsertStorage(
        id = null,
        addr = address,
        alias = alias,
        username = username,
        password = password,
        isAnonymous = isAnonymous,
        typ = kind.toStorageType(),
    )
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
        StorageConnectionTestResult.PERMISSION_DENIED -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.PermissionDenied)
        }
        StorageConnectionTestResult.NOT_FOUND -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.NotFound)
        }
        StorageConnectionTestResult.INVALID_ADDRESS -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.InvalidAddress)
        }
        StorageConnectionTestResult.UNAVAILABLE -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.Unavailable)
        }
        StorageConnectionTestResult.UNSUPPORTED -> {
            SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedSecurityPolicy)
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
