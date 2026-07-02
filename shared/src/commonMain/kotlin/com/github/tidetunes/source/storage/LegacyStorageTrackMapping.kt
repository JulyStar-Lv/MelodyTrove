package com.github.tidetunes.source.storage

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.legacyStorageTrackMediaId
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType

suspend fun TrackEntity.toLegacyStorageTrackMediaIdOrNull(
    storageLookup: LegacyStorageLookup,
): MediaId? {
    return null
}

suspend fun legacyStorageTrackMediaIdOrNull(
    storageLookup: LegacyStorageLookup,
    sourceStorageId: Long?,
    sourcePath: String?,
): MediaId? {
    val storageId = sourceStorageId ?: return null
    val path = sourcePath?.takeIf { it.isNotBlank() } ?: return null
    val storage = storageLookup.storageForPlayback(StorageId(storageId)) ?: return null
    return legacyStorageTrackMediaId(
        sourceId = storage.typ.toBuiltInSourceId(),
        accountId = storage.id.toLegacyStorageSourceAccountId(),
        path = path,
    )
}

fun StorageType.toBuiltInSourceId(): SourceId {
    return when (this) {
        StorageType.LOCAL -> BuiltInSourceIds.Local
        StorageType.WEBDAV -> BuiltInSourceIds.WebDav
        StorageType.ONE_DRIVE -> BuiltInSourceIds.OneDrive
    }
}
