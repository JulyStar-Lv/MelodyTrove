package com.github.tidetunes.database

import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "artwork",
    indices = [
        Index(value = ["contentHash"], unique = true),
        Index(value = ["trackId"]),
        Index(value = ["albumId"]),
    ],
)
data class ArtworkEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long?,
    val albumId: Long?,
    val contentHash: String,
    val localPath: String,
    val thumbnailPath: String?,
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
    val pictureType: String?,
)

@Entity(
    tableName = "lyrics",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["trackId"], unique = true)],
)
data class LyricsEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val format: String,
    val language: String?,
    val synchronized: Boolean,
    val content: String,
    val sourcePath: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "raw_metadata",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["trackId", "tagKey"]),
    ],
)
data class RawMetadataEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val tagKey: String,
    val value: String,
    val locale: String?,
    val description: String?,
)

@Entity(
    tableName = "import_job",
    indices = [
        Index(value = ["selectedFolderId"]),
        Index(value = ["status"]),
    ],
)
data class ImportJobEntity(
    @androidx.room.PrimaryKey val id: String,
    val selectedFolderId: Long,
    val status: String,
    val scannedCount: Long,
    val importedCount: Long,
    val skippedCount: Long,
    val failedCount: Long,
    val checkpoint: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ImportJobWithFolder(
    @Embedded val job: ImportJobEntity,
    val folderStorageId: Long,
    val folderRemoteId: String?,
    val folderCanonicalPath: String,
    val folderDisplayPath: String,
)

@Entity(
    tableName = "sync_cursor",
    indices = [Index(value = ["selectedFolderId"], unique = true)],
)
data class SyncCursorEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val selectedFolderId: Long,
    val deltaLink: String?,
    val continuationToken: String?,
    val lastScanId: String?,
    val lastSyncAt: Long?,
)

@Entity(
    tableName = "download_task",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updatedAt"]),
        Index(value = ["sourceId", "mediaType", "remoteId"], unique = true),
    ],
)
data class DownloadTaskEntity(
    @androidx.room.PrimaryKey val id: String,
    val sourceId: String,
    val mediaType: String,
    val remoteId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val localPath: String?,
    val mimeType: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "playlist",
    indices = [Index(value = ["sortOrder"])],
)
data class PlaylistEntity(
    @androidx.room.PrimaryKey val id: Long,
    val title: String,
    val artworkId: Long?,
    val coverStorageId: Long? = null,
    val coverPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Long,
)

@Entity(
    tableName = "playlist_track",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["playlistId", "sortOrder"]),
    ],
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val sortOrder: Long,
    val addedAt: Long,
)
