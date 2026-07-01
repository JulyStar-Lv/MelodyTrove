package com.github.tidetunes.service.librarysync.domain

/**
 * Default duplicate matcher that compares title, artist, album, and duration.
 * Two tracks are considered duplicates when:
 * - Titles match after case-insensitive whitespace normalization
 * - Artists match (or both null) with same normalization
 * - Albums match (or both null) with same normalization
 * - Duration is within the bucketing tolerance
 */
object DefaultDuplicateMatcher : DuplicateMatcher {
    private const val DURATION_BUCKET_MS = 2_000L

    override fun match(
        normalized: NormalizedMetadataItem,
        existingTracks: List<NormalizedMetadataItem>,
    ): DuplicateMatcher.Match {
        val normalizedKey = normalized.normalizedTitle()
        val normalizedArtist = normalized.artist.normalizeSearchKey()
        val normalizedAlbum = normalized.album.normalizeSearchKey()

        for (existing in existingTracks) {
            if (existing.sourcePath == normalized.sourcePath &&
                existing.accountId == normalized.accountId
            ) {
                return DuplicateMatcher.Match(MatchResult.Update)
            }
        }

        for (existing in existingTracks) {
            if (existing.normalizedTitle() == normalizedKey &&
                existing.artist.normalizeSearchKey() == normalizedArtist &&
                existing.album.normalizeSearchKey() == normalizedAlbum &&
                durationsMatch(normalized.durationMs, existing.durationMs)
            ) {
                return DuplicateMatcher.Match(
                    result = MatchResult.Duplicate,
                    existingTrackId = existing.hashCode().toLong(),
                )
            }
        }

        return DuplicateMatcher.Match(MatchResult.New)
    }

    private fun durationsMatch(a: Long?, b: Long?): Boolean {
        if (a == null || b == null) return true
        return kotlin.math.abs(a - b) <= DURATION_BUCKET_MS
    }
}

/**
 * Normalizer that passes through metadata, deriving title from file name when missing.
 */
object DefaultMetadataNormalizer : MetadataNormalizer {
    override fun normalize(raw: RawMetadataItem): NormalizedMetadataItem {
        return NormalizedMetadataItem(
            accountId = raw.accountId,
            sourcePath = raw.path,
            title = raw.trackTitle?.takeIf { it.isNotBlank() }
                ?: raw.name.stripMusicExtension(),
            trackNumber = raw.trackNumber,
            discNumber = raw.discNumber,
            artist = raw.artist?.takeIf { it.isNotBlank() },
            albumArtist = raw.albumArtist?.takeIf { it.isNotBlank() },
            album = raw.album?.takeIf { it.isNotBlank() },
            genre = raw.genre?.takeIf { it.isNotBlank() },
            year = raw.year,
            durationMs = raw.durationMs,
            bitRateKbps = raw.bitRateKbps,
            sampleRateHz = raw.sampleRateHz,
            bitDepth = raw.bitDepth,
            channels = raw.channels,
            composer = raw.composer?.takeIf { it.isNotBlank() },
            bpm = raw.bpm,
            coverHash = raw.coverHash,
            replayGainTrackDb = raw.replayGainTrackDb,
            replayGainAlbumDb = raw.replayGainAlbumDb,
            lyricRaw = raw.lyricRaw,
            lyricFormat = raw.lyricFormat,
            sourceModifiedAtEpochMs = raw.modifiedAtEpochMs,
        )
    }
}

private fun String.stripMusicExtension(): String {
    val lastDot = lastIndexOf('.')
    return if (lastDot > 0) substring(0, lastDot) else this
}

private fun String?.normalizeSearchKey(): String {
    return this?.trim()?.lowercase()?.replace(Regex("\\s+"), " ") ?: ""
}

private fun NormalizedMetadataItem.normalizedTitle(): String {
    return title.trim().lowercase().replace(Regex("\\s+"), " ")
}
