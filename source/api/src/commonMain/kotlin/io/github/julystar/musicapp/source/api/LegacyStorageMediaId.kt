package io.github.julystar.musicapp.source.api

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.MediaType
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId

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

fun MediaId.toLegacyStoragePlaybackTarget(): LegacyStoragePlaybackTarget? {
    return decodeLegacyStorageTarget(LEGACY_STORAGE_TRACK_PREFIX)
}

fun MediaId.toLegacyStorageArtworkTarget(): LegacyStoragePlaybackTarget? {
    return decodeLegacyStorageTarget(LEGACY_STORAGE_ARTWORK_PREFIX)
}

suspend fun MediaId.resolveLegacyStoragePlayback(
    expectedSourceId: SourceId,
    expectedStorageKind: LegacyStorageKind,
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
        expectedStorageKind = expectedStorageKind,
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

private fun decodeUrlComponent(value: String): String {
    val decoded = StringBuilder(value.length)
    var index = 0

    while (index < value.length) {
        if (value[index] != '%' || index + 2 >= value.length) {
            decoded.append(value[index])
            index += 1
            continue
        }

        val bytes = mutableListOf<Byte>()
        while (index + 2 < value.length && value[index] == '%') {
            val byte = value.substring(index + 1, index + 3).toIntOrNull(16) ?: break
            bytes += byte.toByte()
            index += 3
        }

        if (bytes.isEmpty()) {
            decoded.append('%')
            index += 1
        } else {
            decoded.append(bytes.toByteArray().decodeToString())
        }
    }

    return decoded.toString()
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
