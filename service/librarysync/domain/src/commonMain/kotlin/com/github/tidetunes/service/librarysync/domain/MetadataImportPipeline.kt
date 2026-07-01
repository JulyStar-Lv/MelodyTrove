package com.github.tidetunes.service.librarysync.domain

import com.github.tidetunes.core.domain.model.SourceAccountId

/**
 * Domain model for a single metadata import item before normalization.
 * Represents an audio file discovered during sync scanning.
 */
data class RawMetadataItem(
    val accountId: SourceAccountId,
    val remoteId: String,
    val path: String,
    val name: String,
    val sizeBytes: ULong,
    val mimeType: String?,
    val modifiedAtEpochMs: Long?,
    val trackTitle: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val genre: String?,
    val year: Int?,
    val durationMs: Long?,
    val bitRateKbps: Int?,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val channels: Int?,
    val coverHash: String?,
    val composer: String?,
    val bpm: Int?,
    val replayGainTrackDb: Float?,
    val replayGainAlbumDb: Float?,
    val lyricRaw: String?,
    val lyricFormat: String?,
) {
    init {
        require(remoteId.isNotBlank()) { "remoteId cannot be blank" }
        require(path.isNotBlank()) { "path cannot be blank" }
        require(name.isNotBlank()) { "name cannot be blank" }
    }
}

/**
 * Normalized metadata ready for Room import.
 * All optional fields use null for "not present" semantics.
 */
data class NormalizedMetadataItem(
    val accountId: SourceAccountId,
    val sourcePath: String,
    val title: String,
    val trackNumber: Int?,
    val discNumber: Int?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val genre: String?,
    val year: Int?,
    val durationMs: Long?,
    val bitRateKbps: Int?,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val channels: Int?,
    val composer: String?,
    val bpm: Int?,
    val coverHash: String?,
    val replayGainTrackDb: Float?,
    val replayGainAlbumDb: Float?,
    val lyricRaw: String?,
    val lyricFormat: String?,
    val sourceModifiedAtEpochMs: Long?,
) {
    init {
        require(title.isNotBlank()) { "title cannot be blank" }
        require(sourcePath.isNotBlank()) { "sourcePath cannot be blank" }
    }
}

/**
 * Result of matching a normalized item against the existing library.
 */
enum class MatchResult {
    /** New track with no existing match */
    New,
    /** Exact match by source path and storage account */
    Update,
    /** Potential duplicate by title + artist + album + duration */
    Duplicate,
    /** Skipped - excluded by sync policy */
    Skipped,
}

/**
 * A single item in the import pipeline with its match status.
 */
data class ImportPipelineItem(
    val normalized: NormalizedMetadataItem,
    val matchResult: MatchResult,
    val matchedExistingTrackId: Long? = null,
)

/**
 * Top-level result from a single import batch transaction.
 */
data class ImportBatchResult(
    val batchId: String,
    val insertedCount: Int,
    val updatedCount: Int,
    val duplicateCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
) {
    val totalProcessed: Int
        get() = insertedCount + updatedCount + duplicateCount + skippedCount + failedCount
}

/**
 * Contract for normalizing raw metadata into the canonical form.
 */
fun interface MetadataNormalizer {
    fun normalize(raw: RawMetadataItem): NormalizedMetadataItem
}

/**
 * Contract for matching a normalized item against the existing library.
 * Returns the match result and optional matched track ID.
 */
fun interface DuplicateMatcher {
    data class Match(
        val result: MatchResult,
        val existingTrackId: Long? = null,
    )

    fun match(normalized: NormalizedMetadataItem, existingTracks: List<NormalizedMetadataItem>): Match
}

/**
 * Contract for writing a batch of pipeline items to Room in a single transaction.
 */
fun interface SyncTransactionWriter {
    suspend fun writeBatch(items: List<ImportPipelineItem>): ImportBatchResult
}
