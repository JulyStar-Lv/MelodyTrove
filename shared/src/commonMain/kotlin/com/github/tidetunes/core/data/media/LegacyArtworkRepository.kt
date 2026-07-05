package com.github.tidetunes.core.data.media

import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.ArtworkCacheKey
import com.github.tidetunes.core.domain.repository.ArtworkRepository
import com.github.tidetunes.database.ArtworkEntity
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.source.api.toLegacyStorageArtworkTarget
import com.github.tidetunes.source.storage.toLegacyStorageIdOrNull
import okio.FileSystem
import okio.Path.Companion.toPath
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.ctGetAsset

class LegacyArtworkRepository(
    private val bridge: Bridge,
    private val storageRepository: StorageRepositoryImpl,
    private val roomLibraryStore: RoomLibraryStore,
    private val trackDao: TrackDao,
    private val metadataDao: MetadataDao,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : ArtworkRepository {
    private val cache = HashMap<Artwork, ByteArray>()

    override fun cached(artwork: Artwork): ByteArray? {
        return cache[artwork]
    }

    override suspend fun cacheKey(artwork: Artwork): ArtworkCacheKey? {
        return artwork.resolveRoomArtworkCacheKey(
            findTrack = trackDao::get,
            findTrackArtwork = metadataDao::getArtworkForTrack,
            findAlbumArtwork = metadataDao::getArtworkForAlbum,
        )
    }

    override suspend fun load(artwork: Artwork): ByteArray? {
        cache[artwork]?.let { return it }

        cacheKey(artwork)?.readLocalArtworkBytes(fileSystem)?.let { bytes ->
            cache[artwork] = bytes
            return bytes
        }

        val loc = artwork.resolveLegacyStorageEntryLoc { trackId ->
            roomLibraryStore.resolveTrackLoc(MusicId(trackId))
        } ?: return null
        val storage = storageRepository.storageForRust(loc.storageId) ?: return null
        val bytes = bridge.run { backend ->
            ctGetAsset(backend, storage, loc)
        } ?: return null
        cache[artwork] = bytes
        return bytes
    }
}

internal fun ArtworkCacheKey.readLocalArtworkBytes(fileSystem: FileSystem): ByteArray? {
    return readRegularFile(fileSystem, localPath)
        ?: thumbnailPath?.let { readRegularFile(fileSystem, it) }
}

private fun readRegularFile(fileSystem: FileSystem, path: String): ByteArray? {
    val okioPath = path.toPath()
    val metadata = fileSystem.metadataOrNull(okioPath) ?: return null
    if (!metadata.isRegularFile) return null
    return try {
        fileSystem.read(okioPath) {
            readByteArray()
        }
    } catch (_: Exception) {
        null
    }
}

internal suspend fun Artwork.resolveRoomArtworkCacheKey(
    findTrack: suspend (trackId: Long) -> TrackEntity?,
    findTrackArtwork: suspend (trackId: Long) -> ArtworkEntity?,
    findAlbumArtwork: suspend (albumId: Long) -> ArtworkEntity?,
): ArtworkCacheKey? {
    val entity = when (this) {
        is Artwork.LibraryTrack -> findTrackArtwork(trackId)
            ?: findTrack(trackId)?.albumId?.let { albumId -> findAlbumArtwork(albumId) }
        is Artwork.LibraryCover -> findTrackArtwork(trackId)
            ?: findTrack(trackId)?.albumId?.let { albumId -> findAlbumArtwork(albumId) }
        is Artwork.SourceMedia -> null
        is Artwork.LegacyStorageEntry -> null
    }
    return entity?.toArtworkCacheKey()
}

internal fun ArtworkEntity.toArtworkCacheKey(): ArtworkCacheKey {
    return ArtworkCacheKey(
        contentHash = contentHash,
        localPath = localPath,
        thumbnailPath = thumbnailPath,
        width = width,
        height = height,
        mimeType = mimeType,
        pictureType = pictureType,
    )
}

internal suspend fun Artwork.resolveLegacyStorageEntryLoc(
    resolveTrackLoc: suspend (trackId: Long) -> StorageEntryLoc?,
): StorageEntryLoc? {
    return when (this) {
        is Artwork.LibraryTrack -> resolveTrackLoc(trackId)
        is Artwork.LibraryCover -> null
        is Artwork.SourceMedia -> {
            val target = mediaId.toLegacyStorageArtworkTarget() ?: return null
            val storageId = target.accountId.toLegacyStorageIdOrNull() ?: return null
            StorageEntryLoc(
                storageId = storageId,
                path = target.path,
            )
        }
        is Artwork.LegacyStorageEntry -> StorageEntryLoc(
            storageId = StorageId(storageId),
            path = path,
        )
    }
}
