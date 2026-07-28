package io.github.julystar.musicapp.core.data.security

import io.github.julystar.musicapp.core.domain.model.StoredCredential

interface CredentialStore {
    suspend fun load(storageId: Long): StoredCredential?
    suspend fun save(storageId: Long, credential: StoredCredential)
    suspend fun delete(storageId: Long)
    suspend fun clear()
}

expect fun createCredentialStore(): CredentialStore
