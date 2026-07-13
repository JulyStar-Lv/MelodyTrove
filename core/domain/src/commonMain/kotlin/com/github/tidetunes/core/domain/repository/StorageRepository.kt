package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceConnectionTestStatus
import com.github.tidetunes.core.domain.model.SourceEditorDraft
import com.github.tidetunes.core.domain.model.SourceEditorStorageState
import com.github.tidetunes.core.domain.model.OneDriveDriveListResult
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.StoredCredential
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface StorageRepository {
    val storageAccounts: StateFlow<List<StorageAccountInfo>>
    val onRemoveStorageEvent: SharedFlow<Unit>
    val oauthRefreshToken: StateFlow<String>
    suspend fun reload()
    suspend fun startOneDriveOAuth(): String
    suspend fun upsertSource(draft: SourceEditorDraft): SourceAccountId
    suspend fun loadEditorState(id: Long): SourceEditorStorageState?
    suspend fun testSource(draft: SourceEditorDraft): SourceConnectionTestStatus
    suspend fun listOneDriveDriveInfos(refreshToken: String): OneDriveDriveListResult
    suspend fun updateOneDriveRefreshTokenByAccountId(accountId: SourceAccountId, refreshToken: String)
    fun findStorageAccountByAccountId(accountId: SourceAccountId): StorageAccountInfo?
    suspend fun loadCredentialByAccountId(accountId: SourceAccountId): StoredCredential?
    suspend fun setAccountRootPath(accountId: SourceAccountId, rootPath: String)
    suspend fun removeByAccountId(accountId: SourceAccountId)
}
