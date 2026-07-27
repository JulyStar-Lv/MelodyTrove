package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.SourceEditorDraft
import com.github.tidetunes.core.domain.model.SourceEditorType
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.source.api.SmbSourceConfiguration
import com.github.tidetunes.source.smb.toSmbAddress
import uniffi.tidetunes_backend.ArgUpsertStorage
import uniffi.tidetunes_backend.Storage
import uniffi.tidetunes_backend.StorageId
import uniffi.tidetunes_backend.StorageType

internal fun SourceEditorDraft.toArgUpsertStorage(): ArgUpsertStorage {
    return ArgUpsertStorage(
        id = id?.let { StorageId(it) },
        addr = when (storageType) {
            SourceEditorType.Smb -> toSmbSourceConfiguration().toSmbAddress()
            else -> address
        },
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
        SourceEditorType.Smb -> StorageType.SMB
        SourceEditorType.Navidrome,
        SourceEditorType.OpenSubsonic,
        SourceEditorType.Emby -> StorageType.WEBDAV
    }
}

internal fun StorageType.toSourceEditorType(): SourceEditorType {
    return when (this) {
        StorageType.ONE_DRIVE -> SourceEditorType.OneDrive
        StorageType.SMB -> SourceEditorType.Smb
        else -> SourceEditorType.WebDav
    }
}

internal fun SourceEditorDraft.toSmbSourceConfiguration(): SmbSourceConfiguration {
    return SmbSourceConfiguration(
        accountId = id?.let(::storageSourceAccountId),
        alias = alias,
        host = smbHost,
        port = smbPort,
        share = smbShare,
        rootPath = smbRootPath,
        domain = smbDomain.ifBlank { null },
        username = username,
        password = secret,
        isGuest = isAnonymous,
        requireSigning = smbRequireSigning,
        requireEncryption = smbRequireEncryption,
    )
}
