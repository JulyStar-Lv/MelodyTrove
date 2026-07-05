package com.github.tidetunes.database

import androidx.room.Embedded
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceAccountDao {
    @Query("SELECT * FROM source_account ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<SourceAccountEntity>>

    @Query("SELECT * FROM source_account WHERE id = :id")
    suspend fun get(id: Long): SourceAccountEntity?

    @Query("SELECT MAX(id) FROM source_account")
    suspend fun maxId(): Long?

    @Query("SELECT * FROM source_account ORDER BY priority DESC, displayName COLLATE NOCASE")
    suspend fun listAll(): List<SourceAccountEntity>

    @Upsert
    suspend fun upsert(account: SourceAccountEntity): Long

    @Query(
        """
        UPDATE source_account
        SET enabled = :enabled,
            updatedAt = :updatedAt
        WHERE providerType = :providerType
        """
    )
    suspend fun setEnabledByProviderType(
        providerType: String,
        enabled: Boolean,
        updatedAt: Long,
    )

    @Query("DELETE FROM source_account WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface LibraryRootDao {
    @Query("SELECT * FROM library_root WHERE id = :id")
    suspend fun get(id: Long): LibraryRootEntity?

    @Query(
        """
        SELECT * FROM library_root
        WHERE sourceAccountId = :sourceAccountId
          AND canonicalPath = :canonicalPath
        LIMIT 1
        """
    )
    suspend fun findByPath(sourceAccountId: Long, canonicalPath: String): LibraryRootEntity?

    @Query(
        """
        SELECT * FROM library_root
        WHERE sourceAccountId = :sourceAccountId
          AND providerRootId = :providerRootId
        LIMIT 1
        """
    )
    suspend fun findByProviderRootId(sourceAccountId: Long, providerRootId: String): LibraryRootEntity?

    @Query(
        """
        SELECT * FROM library_root
        WHERE sourceAccountId = :sourceAccountId
        ORDER BY displayName COLLATE NOCASE
        """
    )
    fun observeBySourceAccount(sourceAccountId: Long): Flow<List<LibraryRootEntity>>

    @Upsert
    suspend fun upsert(root: LibraryRootEntity): Long

    @Query("DELETE FROM library_root WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SourceItemDao {
    @Query("SELECT * FROM source_item WHERE id = :id")
    suspend fun get(id: Long): SourceItemEntity?

    @Query("SELECT COUNT(*) FROM source_item WHERE libraryRootId = :libraryRootId")
    suspend fun countForLibraryRoot(libraryRootId: Long): Long

    @Query(
        """
        SELECT * FROM source_item
        WHERE sourceAccountId = :sourceAccountId
          AND canonicalPath = :canonicalPath
        LIMIT 1
        """
    )
    suspend fun findByPath(sourceAccountId: Long, canonicalPath: String): SourceItemEntity?

    @Query(
        """
        SELECT * FROM source_item
        WHERE sourceAccountId = :sourceAccountId
          AND canonicalPath IN (:canonicalPaths)
        """
    )
    suspend fun findByPaths(
        sourceAccountId: Long,
        canonicalPaths: List<String>,
    ): List<SourceItemEntity>

    @Query(
        """
        SELECT * FROM source_item
        WHERE sourceAccountId = :sourceAccountId
          AND providerItemId IN (:providerItemIds)
        """
    )
    suspend fun findByProviderItemIds(
        sourceAccountId: Long,
        providerItemIds: List<String>,
    ): List<SourceItemEntity>

    @Query("SELECT * FROM source_item WHERE contentHash = :contentHash AND contentHash IS NOT NULL")
    suspend fun findByContentHash(contentHash: String): List<SourceItemEntity>

    @Query(
        """
        SELECT * FROM source_item
        WHERE audioFingerprint = :audioFingerprint
          AND audioFingerprint IS NOT NULL
        """
    )
    suspend fun findByAudioFingerprint(audioFingerprint: String): List<SourceItemEntity>

    @Upsert
    suspend fun upsertAll(items: List<SourceItemEntity>): List<Long>

    @Query(
        """
        UPDATE source_item
        SET lastSeenScanId = :scanId,
            lastSyncedAt = :now,
            isDeleted = 0
        WHERE id IN (:ids)
        """
    )
    suspend fun markSeen(
        ids: List<Long>,
        scanId: String,
        now: Long,
    )

    @Query(
        """
        UPDATE source_item
        SET isDeleted = 1,
            lastSyncedAt = :now
        WHERE libraryRootId = :libraryRootId
          AND lastSeenScanId != :scanId
        """
    )
    suspend fun markMissingDeleted(
        libraryRootId: Long,
        scanId: String,
        now: Long,
    )

    @Query(
        """
        UPDATE source_item
        SET isDeleted = 1,
            lastSyncedAt = :now
        WHERE sourceAccountId = :sourceAccountId
          AND providerItemId IN (:providerItemIds)
        """
    )
    suspend fun markDeletedByProviderItemIds(
        sourceAccountId: Long,
        providerItemIds: List<String>,
        now: Long,
    ): Int

    @Transaction
    suspend fun applyScanBatch(
        changedItems: List<SourceItemEntity>,
        unchangedIds: List<Long>,
        scanId: String,
        now: Long,
    ) {
        if (changedItems.isNotEmpty()) {
            upsertAll(changedItems)
        }
        if (unchangedIds.isNotEmpty()) {
            markSeen(unchangedIds, scanId, now)
        }
    }
}

@Dao
interface TrackSourceRefDao {
    @Query("SELECT * FROM track_source_ref WHERE trackId = :trackId")
    suspend fun findByTrackId(trackId: Long): List<TrackSourceRefEntity>

    @Query("SELECT * FROM track_source_ref WHERE sourceItemId IN (:sourceItemIds)")
    suspend fun findBySourceItemIds(sourceItemIds: List<Long>): List<TrackSourceRefEntity>

    @Query("SELECT COUNT(*) FROM track_source_ref WHERE trackId = :trackId")
    suspend fun countForTrack(trackId: Long): Int

    @Upsert
    suspend fun upsertAll(refs: List<TrackSourceRefEntity>)

    @Query(
        """
        UPDATE track_source_ref
        SET isAvailable = 1,
            updatedAt = :now
        WHERE sourceItemId IN (:sourceItemIds)
        """
    )
    suspend fun markAvailableBySourceItemIds(sourceItemIds: List<Long>, now: Long)

    @Query(
        """
        UPDATE track_source_ref
        SET isAvailable = 0,
            updatedAt = :now
        WHERE sourceItemId IN (:sourceItemIds)
        """
    )
    suspend fun markUnavailableBySourceItemIds(sourceItemIds: List<Long>, now: Long)

    @Query(
        """
        UPDATE track_source_ref
        SET isAvailable = 0,
            updatedAt = :now
        WHERE sourceItemId IN (
            SELECT id FROM source_item
            WHERE libraryRootId = :libraryRootId
              AND isDeleted = 1
        )
        """
    )
    suspend fun markUnavailableForDeletedSourceItems(libraryRootId: Long, now: Long)

    @Query(
        """
        SELECT
            ref.trackId AS ref_trackId,
            ref.sourceItemId AS ref_sourceItemId,
            ref.role AS ref_role,
            ref.matchMethod AS ref_matchMethod,
            ref.matchConfidence AS ref_matchConfidence,
            ref.isPreferred AS ref_isPreferred,
            ref.isAvailable AS ref_isAvailable,
            ref.isDownloaded AS ref_isDownloaded,
            ref.playable AS ref_playable,
            ref.downloadable AS ref_downloadable,
            ref.codec AS ref_codec,
            ref.container AS ref_container,
            ref.bitRate AS ref_bitRate,
            ref.sampleRate AS ref_sampleRate,
            ref.bitsPerSample AS ref_bitsPerSample,
            ref.channels AS ref_channels,
            ref.lossless AS ref_lossless,
            ref.createdAt AS ref_createdAt,
            ref.updatedAt AS ref_updatedAt,
            item.id AS item_id,
            item.sourceAccountId AS item_sourceAccountId,
            item.libraryRootId AS item_libraryRootId,
            item.itemType AS item_itemType,
            item.providerItemId AS item_providerItemId,
            item.parentProviderItemId AS item_parentProviderItemId,
            item.canonicalPath AS item_canonicalPath,
            item.displayPath AS item_displayPath,
            item.displayName AS item_displayName,
            item.mimeType AS item_mimeType,
            item.sizeBytes AS item_sizeBytes,
            item.etag AS item_etag,
            item.revision AS item_revision,
            item.createdAtRemote AS item_createdAtRemote,
            item.modifiedAtRemote AS item_modifiedAtRemote,
            item.contentHash AS item_contentHash,
            item.audioFingerprint AS item_audioFingerprint,
            item.isDeleted AS item_isDeleted,
            item.firstSyncedAt AS item_firstSyncedAt,
            item.lastSyncedAt AS item_lastSyncedAt,
            item.lastSeenScanId AS item_lastSeenScanId,
            account.id AS account_id,
            account.providerType AS account_providerType,
            account.displayName AS account_displayName,
            account.endpoint AS account_endpoint,
            account.externalAccountId AS account_externalAccountId,
            account.credentialRef AS account_credentialRef,
            account.priority AS account_priority,
            account.enabled AS account_enabled,
            account.createdAt AS account_createdAt,
            account.updatedAt AS account_updatedAt
        FROM track_source_ref ref
        JOIN source_item item ON item.id = ref.sourceItemId
        JOIN source_account account ON account.id = item.sourceAccountId
        WHERE ref.trackId = :trackId
          AND ref.playable = 1
          AND ref.isAvailable = 1
          AND item.isDeleted = 0
          AND account.enabled = 1
        ORDER BY
          ref.isDownloaded DESC,
          CASE WHEN account.providerType = 'local' THEN 1 ELSE 0 END DESC,
          ref.isPreferred DESC,
          COALESCE(ref.lossless, 0) DESC,
          COALESCE(ref.bitsPerSample, 0) DESC,
          COALESCE(ref.sampleRate, 0) DESC,
          COALESCE(ref.bitRate, 0) DESC,
          account.priority DESC,
          ref.updatedAt DESC
        """
    )
    suspend fun playbackCandidates(trackId: Long): List<TrackSourcePlaybackCandidate>
}

data class TrackSourcePlaybackCandidate(
    @Embedded(prefix = "ref_") val ref: TrackSourceRefEntity,
    @Embedded(prefix = "item_") val item: SourceItemEntity,
    @Embedded(prefix = "account_") val account: SourceAccountEntity,
)

@Dao
interface SourceSyncCursorDao {
    @Upsert
    suspend fun upsert(cursor: SourceSyncCursorEntity)
}

@Dao
interface SourceErrorDao {
    @Upsert
    suspend fun upsert(error: SourceErrorEntity)

    @Insert
    suspend fun insertAll(errors: List<SourceErrorEntity>)

    @Query("SELECT * FROM source_error WHERE importJobId = :importJobId ORDER BY createdAt DESC, id DESC")
    fun observeByImportJob(importJobId: String): Flow<List<SourceErrorEntity>>

    @Query("DELETE FROM source_error WHERE importJobId = :importJobId")
    suspend fun deleteByImportJob(importJobId: String)
}
