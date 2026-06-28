package com.github.tidetunes.source.storage

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.core.utils.decodeUrlComponent

data class LegacyStoragePlaybackTarget(
    val accountId: SourceAccountId,
    val path: String,
)

fun legacyStorageArtworkMediaId(
    sourceId: SourceId,
    accountId: SourceAccountId,
    path: String,
): MediaId {
    require(path.isNotBlank()) { "Legacy storage artwork path cannot be blank" }
    return legacyStorageMediaId(
        sourceId = sourceId,
        mediaType = MediaType.Image,
        prefix = LEGACY_STORAGE_ARTWORK_PREFIX,
        accountId = accountId,
        path = path,
    )
}

fun legacyStorageTrackMediaId(
    sourceId: SourceId,
    accountId: SourceAccountId,
    path: String,
): MediaId {
    require(path.isNotBlank()) { "Legacy storage playback path cannot be blank" }
    return legacyStorageMediaId(
        sourceId = sourceId,
        mediaType = MediaType.Track,
        prefix = LEGACY_STORAGE_TRACK_PREFIX,
        accountId = accountId,
        path = path,
    )
}

private fun legacyStorageMediaId(
    sourceId: SourceId,
    mediaType: MediaType,
    prefix: String,
    accountId: SourceAccountId,
    path: String,
): MediaId {
    return MediaId(
        sourceId = sourceId,
        mediaType = mediaType,
        remoteId = "$prefix:" +
            "${encodeUrlComponent(accountId.value)}:${encodeUrlComponent(path)}",
    )
}

internal suspend fun MediaId.resolveLegacyStoragePlayback(
    expectedSourceId: SourceId,
    expectedStorageType: uniffi.tidetunes_core.StorageType,
    playbackResolver: LegacyStoragePlaybackResolver,
): SourcePlaybackResult {
    if (sourceId != expectedSourceId) {
        return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaId)
    }
    if (mediaType != MediaType.Track) {
        return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaType)
    }
    val target = toLegacyStoragePlaybackTarget()
        ?: return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedMediaId)

    return playbackResolver.resolve(
        accountId = target.accountId,
        path = target.path,
        expectedStorageType = expectedStorageType,
    )
}

internal fun MediaId.toLegacyStoragePlaybackTarget(): LegacyStoragePlaybackTarget? {
    return decodeLegacyStorageTarget(LEGACY_STORAGE_TRACK_PREFIX)
}

internal fun MediaId.toLegacyStorageArtworkTarget(): LegacyStoragePlaybackTarget? {
    return decodeLegacyStorageTarget(LEGACY_STORAGE_ARTWORK_PREFIX)
}

private fun MediaId.decodeLegacyStorageTarget(prefix: String): LegacyStoragePlaybackTarget? {
    val remotePrefix = "$prefix:"
    if (!remoteId.startsWith(remotePrefix)) return null

    val encoded = remoteId.removePrefix(remotePrefix)
    val separator = encoded.indexOf(':')
    if (separator <= 0 || separator == encoded.lastIndex) return null

    val accountId = decodeUrlComponent(encoded.substring(0, separator))
    val path = decodeUrlComponent(encoded.substring(separator + 1))
    if (accountId.isBlank() || path.isBlank()) return null

    return LegacyStoragePlaybackTarget(
        accountId = SourceAccountId(accountId),
        path = path,
    )
}

private fun encodeUrlComponent(value: String): String {
    val encoded = StringBuilder(value.length)
    for (byte in value.encodeToByteArray()) {
        val unsigned = byte.toInt() and 0xff
        val char = unsigned.toChar()
        if (char.isUnreservedUrlComponent()) {
            encoded.append(char)
        } else {
            encoded.append('%')
            encoded.append(unsigned.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return encoded.toString()
}

private fun Char.isUnreservedUrlComponent(): Boolean {
    return this in 'A'..'Z' ||
        this in 'a'..'z' ||
        this in '0'..'9' ||
        this == '-' ||
        this == '_' ||
        this == '.' ||
        this == '~'
}

private const val LEGACY_STORAGE_TRACK_PREFIX = "legacy-storage-track"
private const val LEGACY_STORAGE_ARTWORK_PREFIX = "legacy-storage-artwork"
