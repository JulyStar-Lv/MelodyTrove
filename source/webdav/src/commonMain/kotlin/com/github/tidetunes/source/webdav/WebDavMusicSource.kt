package com.github.tidetunes.source.webdav

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.LegacyStorageConnectionRequest
import com.github.tidetunes.source.api.LegacyStorageConnectionTester
import com.github.tidetunes.source.api.LegacyStorageDirectoryLister
import com.github.tidetunes.source.api.LegacyStorageKind
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.api.LegacyStorageSearchProvider
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.MusicSourceDescriptor
import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchResult
import com.github.tidetunes.source.api.UnsupportedLegacyStorageSearchProvider
import com.github.tidetunes.source.api.WebDavSourceConfiguration
import com.github.tidetunes.source.api.resolveLegacyStoragePlayback

class WebDavMusicSource(
    private val connectionTester: LegacyStorageConnectionTester,
    private val directoryLister: LegacyStorageDirectoryLister,
    private val playbackResolver: LegacyStoragePlaybackResolver,
    private val searchProvider: LegacyStorageSearchProvider = UnsupportedLegacyStorageSearchProvider,
) : MusicSource {
    override val descriptor = MusicSourceDescriptor(
        id = BuiltInSourceIds.WebDav,
        displayName = "WebDAV",
    )

    override val capabilities = setOf(
        SourceCapability.Browse,
        SourceCapability.Search,
        SourceCapability.Stream,
        SourceCapability.Download,
    )

    override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
        if (configuration !is WebDavSourceConfiguration) {
            return SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration)
        }
        return connectionTester.test(configuration.toLegacyStorageConnectionRequest())
    }

    override suspend fun list(
        accountId: SourceAccountId,
        directoryId: String?,
    ): SourceListResult {
        return directoryLister.list(accountId, directoryId, LegacyStorageKind.WebDav)
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
            expectedStorageKind = LegacyStorageKind.WebDav,
            sourceId = descriptor.id,
        )
    }

    override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
        return mediaId.resolveLegacyStoragePlayback(
            expectedSourceId = descriptor.id,
            expectedStorageKind = LegacyStorageKind.WebDav,
            playbackResolver = playbackResolver,
        )
    }
}

private fun WebDavSourceConfiguration.toLegacyStorageConnectionRequest(): LegacyStorageConnectionRequest {
    return LegacyStorageConnectionRequest(
        alias = alias,
        address = address,
        username = username,
        password = password,
        isAnonymous = isAnonymous,
        kind = LegacyStorageKind.WebDav,
    )
}
