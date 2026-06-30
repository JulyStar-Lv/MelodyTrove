package com.github.tidetunes.core.domain.model

data class StorageAccountInfo(
    val accountId: SourceAccountId,
    val sourceId: SourceId,
    val isLocal: Boolean,
    val isOneDrive: Boolean,
    val title: String,
    val subtitle: String,
    val musicCount: Long,
)

data class OneDriveDriveInfo(
    val id: String,
    val name: String,
)

fun storageSourceAccountId(storageId: Long): SourceAccountId {
    return SourceAccountId("$STORAGE_ACCOUNT_PREFIX$storageId")
}

fun SourceAccountId.toStorageRouteIdOrNull(): Long? {
    return value
        .takeIf { it.startsWith(STORAGE_ACCOUNT_PREFIX) }
        ?.removePrefix(STORAGE_ACCOUNT_PREFIX)
        ?.toLongOrNull()
}

const val STORAGE_ACCOUNT_PREFIX = "storage:"
