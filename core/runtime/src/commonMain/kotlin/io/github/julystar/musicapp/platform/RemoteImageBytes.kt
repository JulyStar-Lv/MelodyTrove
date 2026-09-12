package io.github.julystar.musicapp.platform

internal expect suspend fun fetchRemoteImageBytes(
    url: String,
    maxBytes: Long,
): ByteArray?
