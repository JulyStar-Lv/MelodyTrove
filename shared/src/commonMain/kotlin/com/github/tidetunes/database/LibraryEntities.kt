package com.github.tidetunes.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "storage",
    indices = [
        Index(value = ["type"]),
        Index(value = ["credentialRef"], unique = true),
    ],
)
data class StorageEntity(
    val type: String,
    val displayName: String,
    val baseUrl: String,
    val driveId: String? = null,
    val credentialRef: String,
    val username: String = "",
    val isAnonymous: Boolean,
    val musicCount: Long,
    val createdAt: Long,
    val updatedAt: Long,
    @androidx.room.PrimaryKey val id: Long,
)

@Entity(
    tableName = "selected_folder",
    foreignKeys = [
        ForeignKey(
            entity = StorageEntity::class,
            parentColumns = ["id"],
            childColumns = ["storageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["storageId"]),
        Index(value = ["storageId", "remoteId"], unique = true),
        Index(value = ["storageId", "canonicalPath"], unique = true),
    ],
)
data class SelectedFolderEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storageId: Long,
    val remoteId: String?,
    val canonicalPath: String,
    val displayPath: String,
    val deltaLink: String?,
    val lastSyncAt: Long?,
    val syncStatus: String,
)

@Entity(
    tableName = "remote_file",
    foreignKeys = [
        ForeignKey(
            entity = StorageEntity::class,
            parentColumns = ["id"],
            childColumns = ["storageId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SelectedFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["selectedFolderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["storageId"]),
        Index(value = ["selectedFolderId"]),
        Index(value = ["storageId", "remoteId"], unique = true),
        Index(value = ["storageId", "canonicalPath"], unique = true),
        Index(value = ["parentRemoteId"]),
        Index(value = ["isDeleted"]),
        Index(value = ["lastSeenScanId"]),
    ],
)
data class RemoteFileEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storageId: Long,
    val selectedFolderId: Long,
    val remoteId: String?,
    val parentRemoteId: String?,
    val canonicalPath: String,
    val displayPath: String,
    val fileName: String,
    val extension: String,
    val mimeType: String?,
    val size: Long?,
    val etag: String?,
    val ctag: String?,
    val createdAt: Long?,
    val modifiedAt: Long?,
    val contentHash: String?,
    val isDeleted: Boolean,
    val lastSeenScanId: String,
)

@Entity(
    tableName = "album",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class AlbumEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val sortName: String?,
    val year: Int?,
    val artworkId: Long?,
)

@Entity(
    tableName = "artist",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class ArtistEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val sortName: String?,
)

@Entity(
    tableName = "genre",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class GenreEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
)

@Entity(
    tableName = "track",
    foreignKeys = [
        ForeignKey(
            entity = RemoteFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["remoteFileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["remoteFileId"], unique = true),
        Index(value = ["albumId"]),
        Index(value = ["title"]),
    ],
)
data class TrackEntity(
    @androidx.room.PrimaryKey val id: Long,
    val remoteFileId: Long?,
    val sourceStorageId: Long? = null,
    val sourcePath: String? = null,
    val title: String,
    val sortTitle: String?,
    val albumId: Long?,
    val albumArtist: String?,
    val composer: String?,
    val comment: String?,
    val grouping: String?,
    val durationMs: Long?,
    val discNumber: Int?,
    val discTotal: Int?,
    val trackNumber: Int?,
    val trackTotal: Int?,
    val year: Int?,
    val date: String?,
    val sampleRate: Int?,
    val bitRate: Int?,
    val bitsPerSample: Int?,
    val channels: Int?,
    val channelLayout: String?,
    val codec: String?,
    val container: String?,
    val lossless: Boolean?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastPlayedAt: Long? = null,
    val artist: String? = null,
    val lyricist: String? = null,
    val conductor: String? = null,
    val copyright: String? = null,
    val publisher: String? = null,
    val originalReleaseDate: String? = null,
    val bpm: Double? = null,
    val musicalKey: String? = null,
    val isrc: String? = null,
    val musicBrainzRecordingId: String? = null,
    val musicBrainzTrackId: String? = null,
    val musicBrainzReleaseId: String? = null,
    val musicBrainzReleaseGroupId: String? = null,
    val musicBrainzArtistId: String? = null,
    val musicBrainzReleaseArtistId: String? = null,
    val musicBrainzWorkId: String? = null,
    val replayGainTrackGain: Double? = null,
    val replayGainTrackPeak: Double? = null,
    val replayGainAlbumGain: Double? = null,
    val replayGainAlbumPeak: Double? = null,
)

@Entity(
    tableName = "track_artist",
    primaryKeys = ["trackId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["artistId"])],
)
data class TrackArtistCrossRef(
    val trackId: Long,
    val artistId: Long,
    val position: Int,
)

@Entity(
    tableName = "album_artist",
    primaryKeys = ["albumId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["artistId"])],
)
data class AlbumArtistCrossRef(
    val albumId: Long,
    val artistId: Long,
    val position: Int,
)

@Entity(
    tableName = "track_genre",
    primaryKeys = ["trackId", "genreId"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GenreEntity::class,
            parentColumns = ["id"],
            childColumns = ["genreId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["genreId"])],
)
data class TrackGenreCrossRef(
    val trackId: Long,
    val genreId: Long,
)
