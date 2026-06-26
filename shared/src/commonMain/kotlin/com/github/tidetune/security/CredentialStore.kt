package com.github.tidetune.security

data class StoredCredential(
    val username: String,
    val secret: String,
    val isAnonymous: Boolean,
)

interface CredentialStore {
    suspend fun load(storageId: Long): StoredCredential?
    suspend fun save(storageId: Long, credential: StoredCredential)
    suspend fun delete(storageId: Long)
}

expect fun createCredentialStore(): CredentialStore
