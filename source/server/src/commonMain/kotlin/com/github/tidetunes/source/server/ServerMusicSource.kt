package com.github.tidetunes.source.server

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.MusicSourceDescriptor
import com.github.tidetunes.source.api.RemoteServerGateway
import com.github.tidetunes.source.api.RemoteServerKind
import com.github.tidetunes.source.api.RemoteServerSourceConfiguration
import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListFailureReason
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourceMediaItem
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeType
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchFailureReason
import com.github.tidetunes.source.api.SourceSearchResult

class ServerMusicSource(
    private val kind: RemoteServerKind,
    private val gateway: RemoteServerGateway,
) : MusicSource {
    override val descriptor = MusicSourceDescriptor(
        id = when (kind) {
            RemoteServerKind.Navidrome -> BuiltInSourceIds.Navidrome
            RemoteServerKind.OpenSubsonic -> BuiltInSourceIds.OpenSubsonic
            RemoteServerKind.Emby -> BuiltInSourceIds.Emby
        },
        displayName = when (kind) {
            RemoteServerKind.Navidrome -> "Navidrome"
            RemoteServerKind.OpenSubsonic -> "OpenSubsonic"
            RemoteServerKind.Emby -> "Emby"
        },
    )

    override val capabilities = setOf(
        SourceCapability.Browse,
        SourceCapability.Search,
        SourceCapability.Stream,
        SourceCapability.Download,
        SourceCapability.Lyrics,
    )

    override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
        val server = configuration as? RemoteServerSourceConfiguration
            ?: return SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration)
        if (server.kind != kind) {
            return SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration)
        }
        return gateway.authenticate(server)
    }

    override suspend fun list(
        accountId: SourceAccountId,
        directoryId: String?,
    ): SourceListResult = gateway.tracks(
        kind = kind,
        accountId = accountId,
        limit = SERVER_BROWSE_LIMIT,
    ).fold(
        onSuccess = { tracks ->
            SourceListResult.Success(
                tracks.map { track ->
                    SourceNode(
                        accountId = accountId,
                        nodeId = track.encodedId(),
                        remoteId = track.encodedId(),
                        name = buildString {
                            append(track.title)
                            track.artist?.let { append(" — ").append(it) }
                        },
                        path = "/${track.remoteId}",
                        type = SourceNodeType.Track,
                        mimeType = track.mimeType,
                    )
                }
            )
        },
        onFailure = { SourceListResult.Failure(SourceListFailureReason.Unavailable) },
    )

    override suspend fun search(
        accountId: SourceAccountId,
        query: String,
        limit: Int,
    ): SourceSearchResult = gateway.tracks(kind, accountId, query, limit).fold(
        onSuccess = { tracks ->
            SourceSearchResult.Success(
                tracks.map { track ->
                    SourceMediaItem(
                        mediaId = MediaId(descriptor.id, MediaType.Track, track.encodedId()),
                        accountId = accountId,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        durationMs = track.durationMs,
                        path = track.streamUrl,
                    )
                }
            )
        },
        onFailure = { SourceSearchResult.Failure(SourceSearchFailureReason.Unavailable) },
    )

    override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
        if (mediaId.sourceId != descriptor.id) {
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaId)
        }
        if (mediaId.mediaType != MediaType.Track) {
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaType)
        }
        return gateway.playback(kind, mediaId.remoteId)
    }
}

private fun com.github.tidetunes.source.api.RemoteServerTrack.encodedId(): String =
    "${accountId.value}|$remoteId"

private const val SERVER_BROWSE_LIMIT = 10_000
