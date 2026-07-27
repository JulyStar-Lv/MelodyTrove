package com.github.tidetunes.source.smb

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
import com.github.tidetunes.source.api.SmbSourceConfiguration
import com.github.tidetunes.source.api.SourceAuthFailureReason
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchResult
import com.github.tidetunes.source.api.UnsupportedLegacyStorageSearchProvider
import com.github.tidetunes.source.api.resolveLegacyStoragePlayback

class SmbMusicSource(
    private val connectionTester: LegacyStorageConnectionTester,
    private val directoryLister: LegacyStorageDirectoryLister,
    private val playbackResolver: LegacyStoragePlaybackResolver,
    private val searchProvider: LegacyStorageSearchProvider = UnsupportedLegacyStorageSearchProvider,
) : MusicSource {
    override val descriptor = MusicSourceDescriptor(
        id = BuiltInSourceIds.Smb,
        displayName = "SMB",
    )

    override val capabilities = setOf(
        SourceCapability.Browse,
        SourceCapability.Search,
        SourceCapability.Stream,
        SourceCapability.Download,
    )

    override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
        if (configuration !is SmbSourceConfiguration) {
            return SourceAuthResult.Failure(SourceAuthFailureReason.UnsupportedConfiguration)
        }
        return connectionTester.test(configuration.toLegacyStorageConnectionRequest())
    }

    override suspend fun list(
        accountId: SourceAccountId,
        directoryId: String?,
    ): SourceListResult {
        return directoryLister.list(accountId, directoryId, LegacyStorageKind.Smb)
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
            expectedStorageKind = LegacyStorageKind.Smb,
            sourceId = descriptor.id,
        )
    }

    override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
        return mediaId.resolveLegacyStoragePlayback(
            expectedSourceId = descriptor.id,
            expectedStorageKind = LegacyStorageKind.Smb,
            playbackResolver = playbackResolver,
        )
    }
}

fun SmbSourceConfiguration.toSmbAddress(): String {
    require(host.isNotBlank()) { "SMB host cannot be blank" }
    require(port in 1..65535) { "SMB port must be between 1 and 65535" }
    val normalizedShare = share.normalizedSmbPath(required = true)
    val normalizedRoot = rootPath.normalizedSmbPath(required = false)
    val renderedHost = if (':' in host && !host.startsWith("[")) "[$host]" else host
    val path = buildString {
        append('/')
        append(normalizedShare.encodeUrlComponent())
        normalizedRoot.split('/').filter(String::isNotEmpty).forEach { segment ->
            append('/')
            append(segment.encodeUrlComponent())
        }
    }
    val query = buildList {
        domain?.trim()?.takeIf(String::isNotEmpty)?.let { add("domain=${it.encodeUrlComponent()}") }
        if (requireSigning) add("signing=true")
        if (requireEncryption) add("encryption=true")
    }.joinToString("&")
    return buildString {
        append("smb://")
        append(renderedHost)
        if (port != 445) append(":$port")
        append(path)
        if (query.isNotEmpty()) append("?$query")
    }
}

private fun SmbSourceConfiguration.toLegacyStorageConnectionRequest(): LegacyStorageConnectionRequest {
    return LegacyStorageConnectionRequest(
        alias = alias,
        address = toSmbAddress(),
        username = if (isGuest) "" else username,
        password = if (isGuest) "" else password,
        isAnonymous = isGuest,
        kind = LegacyStorageKind.Smb,
    )
}

private fun String.normalizedSmbPath(required: Boolean): String {
    require('\u0000' !in this) { "SMB paths cannot contain NUL" }
    val segments = replace('\\', '/')
        .split('/')
        .filter { it.isNotEmpty() && it != "." }
    require(".." !in segments) { "SMB path traversal is not allowed" }
    val normalized = segments.joinToString("/")
    if (required) require(normalized.isNotEmpty()) { "SMB share cannot be blank" }
    return normalized
}

private fun String.encodeUrlComponent(): String {
    return buildString {
        this@encodeUrlComponent.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and 0xff
            val character = value.toChar()
            if (
                character in 'A'..'Z' ||
                character in 'a'..'z' ||
                character in '0'..'9' ||
                character == '-' ||
                character == '_' ||
                character == '.' ||
                character == '~'
            ) {
                append(character)
            } else {
                append('%')
                append(value.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
}
