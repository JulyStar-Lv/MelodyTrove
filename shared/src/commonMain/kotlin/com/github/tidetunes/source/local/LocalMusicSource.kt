package com.github.tidetunes.source.local

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.LocalSourceConfiguration
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.MusicSourceDescriptor
import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.storage.LegacyStorageDirectoryLister
import com.github.tidetunes.source.storage.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchResult
import com.github.tidetunes.source.storage.LegacyStorageSearchProvider
import com.github.tidetunes.source.storage.UnsupportedLegacyStorageSearchProvider
import com.github.tidetunes.source.storage.resolveLegacyStoragePlayback
import uniffi.tidetunes_core.StorageType

class LocalMusicSource(
    private val directoryLister: LegacyStorageDirectoryLister,
    private val playbackResolver: LegacyStoragePlaybackResolver,
    private val searchProvider: LegacyStorageSearchProvider = UnsupportedLegacyStorageSearchProvider,
) : MusicSource {
    override val descriptor = MusicSourceDescriptor(
        id = BuiltInSourceIds.Local,
        displayName = "Local",
    )

    override val capabilities = setOf(
        SourceCapability.Browse,
        SourceCapability.Search,
        SourceCapability.Stream,
        SourceCapability.Download,
    )

    override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
        return if (configuration is LocalSourceConfiguration) {
            SourceAuthResult.Success
        } else {
            SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration)
        }
    }

    override suspend fun list(
        accountId: SourceAccountId,
        directoryId: String?,
    ): SourceListResult {
        return directoryLister.list(accountId, directoryId, StorageType.LOCAL)
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
            expectedStorageType = StorageType.LOCAL,
            sourceId = descriptor.id,
        )
    }

    override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
        return mediaId.resolveLegacyStoragePlayback(
            expectedSourceId = descriptor.id,
            expectedStorageType = StorageType.LOCAL,
            playbackResolver = playbackResolver,
        )
    }
}
