package io.github.julystar.musicapp.source.local

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.source.api.BuiltInSourceIds
import io.github.julystar.musicapp.source.api.LegacyStorageDirectoryLister
import io.github.julystar.musicapp.source.api.LegacyStorageKind
import io.github.julystar.musicapp.source.api.LegacyStoragePlaybackResolver
import io.github.julystar.musicapp.source.api.LegacyStorageSearchProvider
import io.github.julystar.musicapp.source.api.LocalSourceConfiguration
import io.github.julystar.musicapp.source.api.MusicSource
import io.github.julystar.musicapp.source.api.MusicSourceDescriptor
import io.github.julystar.musicapp.source.api.SourceAuthFailureReason
import io.github.julystar.musicapp.source.api.SourceAuthResult
import io.github.julystar.musicapp.source.api.SourceCapability
import io.github.julystar.musicapp.source.api.SourceConfiguration
import io.github.julystar.musicapp.source.api.SourceListResult
import io.github.julystar.musicapp.source.api.SourcePlaybackResult
import io.github.julystar.musicapp.source.api.SourceSearchResult
import io.github.julystar.musicapp.source.api.UnsupportedLegacyStorageSearchProvider
import io.github.julystar.musicapp.source.api.resolveLegacyStoragePlayback

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
        return directoryLister.list(accountId, directoryId, LegacyStorageKind.Local)
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
            expectedStorageKind = LegacyStorageKind.Local,
            sourceId = descriptor.id,
        )
    }

    override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
        return mediaId.resolveLegacyStoragePlayback(
            expectedSourceId = descriptor.id,
            expectedStorageKind = LegacyStorageKind.Local,
            playbackResolver = playbackResolver,
        )
    }
}
