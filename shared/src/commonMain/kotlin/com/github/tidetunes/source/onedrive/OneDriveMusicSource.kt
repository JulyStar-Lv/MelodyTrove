package com.github.tidetunes.source.onedrive

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.MusicSourceDescriptor
import com.github.tidetunes.source.api.OneDriveSourceConfiguration
import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchResult
import com.github.tidetunes.source.storage.LegacyStorageConnectionTester
import com.github.tidetunes.source.storage.LegacyStorageDirectoryLister
import com.github.tidetunes.source.storage.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.storage.LegacyStorageSearchProvider
import com.github.tidetunes.source.storage.UnsupportedLegacyStorageSearchProvider
import com.github.tidetunes.source.storage.resolveLegacyStoragePlayback
import com.github.tidetunes.source.storage.toSourceAuthResult
import uniffi.tidetunes_core.ArgUpsertStorage
import uniffi.tidetunes_core.StorageType

class OneDriveMusicSource(
    private val connectionTester: LegacyStorageConnectionTester,
    private val directoryLister: LegacyStorageDirectoryLister,
    private val playbackResolver: LegacyStoragePlaybackResolver,
    private val searchProvider: LegacyStorageSearchProvider = UnsupportedLegacyStorageSearchProvider,
) : MusicSource {
    override val descriptor = MusicSourceDescriptor(
        id = BuiltInSourceIds.OneDrive,
        displayName = "OneDrive",
    )

    override val capabilities = setOf(
        SourceCapability.Browse,
        SourceCapability.Search,
        SourceCapability.Stream,
        SourceCapability.Download,
        SourceCapability.IncrementalSync,
    )

    override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
        if (configuration !is OneDriveSourceConfiguration) {
            return SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration)
        }
        return connectionTester.test(configuration.toArgUpsertStorage()).toSourceAuthResult()
    }

    override suspend fun list(
        accountId: SourceAccountId,
        directoryId: String?,
    ): SourceListResult {
        return directoryLister.list(accountId, directoryId, StorageType.ONE_DRIVE)
    }

    override suspend fun search(
        accountId: SourceAccountId,
        query: String,
        limit: Int,
    ): SourceSearchResult {
        return searchProvider.search(
            accountId = accountId,
            query = query,
            limit = limit,
            expectedStorageType = StorageType.ONE_DRIVE,
            sourceId = descriptor.id,
        )
    }

    override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
        return mediaId.resolveLegacyStoragePlayback(
            expectedSourceId = descriptor.id,
            expectedStorageType = StorageType.ONE_DRIVE,
            playbackResolver = playbackResolver,
        )
    }
}

private fun OneDriveSourceConfiguration.toArgUpsertStorage(): ArgUpsertStorage {
    return ArgUpsertStorage(
        id = null,
        addr = driveId,
        alias = alias,
        username = "",
        password = refreshToken,
        isAnonymous = false,
        typ = StorageType.ONE_DRIVE,
    )
}
