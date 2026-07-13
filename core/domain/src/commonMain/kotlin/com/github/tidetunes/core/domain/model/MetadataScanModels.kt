package com.github.tidetunes.core.domain.model

enum class MetadataScanMode {
    Fast,
    Standard,
    Full,
}

data class MetadataScanOptions(
    val readArtwork: Boolean,
    val readLyrics: Boolean,
    val readRawMetadata: Boolean,
)

fun MetadataScanMode.toOptions(): MetadataScanOptions = when (this) {
    MetadataScanMode.Fast -> MetadataScanOptions(
        readArtwork = false,
        readLyrics = false,
        readRawMetadata = false,
    )
    MetadataScanMode.Standard -> MetadataScanOptions(
        readArtwork = false,
        readLyrics = true,
        readRawMetadata = false,
    )
    MetadataScanMode.Full -> MetadataScanOptions(
        readArtwork = true,
        readLyrics = true,
        readRawMetadata = true,
    )
}

enum class MetadataRefreshTarget {
    Artwork,
    Lyrics,
    RawMetadata,
    All,
}

fun MetadataRefreshTarget.toOptions(): MetadataScanOptions = when (this) {
    MetadataRefreshTarget.Artwork -> MetadataScanOptions(
        readArtwork = true,
        readLyrics = false,
        readRawMetadata = false,
    )
    MetadataRefreshTarget.Lyrics -> MetadataScanOptions(
        readArtwork = false,
        readLyrics = true,
        readRawMetadata = false,
    )
    MetadataRefreshTarget.RawMetadata -> MetadataScanOptions(
        readArtwork = false,
        readLyrics = false,
        readRawMetadata = true,
    )
    MetadataRefreshTarget.All -> MetadataScanOptions(
        readArtwork = true,
        readLyrics = true,
        readRawMetadata = true,
    )
}
