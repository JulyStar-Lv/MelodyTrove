package com.github.tidetunes.core.data.security

import com.github.tidetunes.core.domain.model.StoredCredential

interface CredentialStore {
    suspend fun load(storageId: Long): StoredCredential?
    suspend fun save(storageId: Long, credential: StoredCredential)
    suspend fun delete(storageId: Long)
}

expect fun createCredentialStore(): CredentialStore
