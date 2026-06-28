package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.StoredCredential
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface StorageRepository {
    val storageAccounts: StateFlow<List<StorageAccountInfo>>
    val onRemoveStorageEvent: SharedFlow<Unit>
    val oauthRefreshToken: StateFlow<String>
    suspend fun reload()
    fun findStorageAccountByAccountId(accountId: SourceAccountId): StorageAccountInfo?
    suspend fun loadCredentialByAccountId(accountId: SourceAccountId): StoredCredential?
    suspend fun removeByAccountId(accountId: SourceAccountId)
}
