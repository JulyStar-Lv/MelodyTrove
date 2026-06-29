package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.SourceEditorDraft
import com.github.tidetunes.core.domain.model.SourceEditorType
import uniffi.tidetunes_core.ArgUpsertStorage
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType

internal fun SourceEditorDraft.toArgUpsertStorage(): ArgUpsertStorage {
    return ArgUpsertStorage(
        id = id?.let { StorageId(it) },
        addr = address,
        alias = alias,
        username = username,
        password = secret,
        isAnonymous = isAnonymous,
        typ = storageType.toStorageType(),
    )
}

internal fun Storage.toSourceEditorDraft(): SourceEditorDraft {
    return SourceEditorDraft(
        id = id.value,
        address = addr,
        alias = alias,
        username = username,
        secret = password,
        isAnonymous = isAnonymous,
        storageType = typ.toSourceEditorType(),
    )
}

internal fun SourceEditorType.toStorageType(): StorageType {
    return when (this) {
        SourceEditorType.WebDav -> StorageType.WEBDAV
        SourceEditorType.OneDrive -> StorageType.ONE_DRIVE
    }
}

internal fun StorageType.toSourceEditorType(): SourceEditorType {
    return when (this) {
        StorageType.ONE_DRIVE -> SourceEditorType.OneDrive
        else -> SourceEditorType.WebDav
    }
}
