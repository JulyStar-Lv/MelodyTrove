package com.github.tidetune.singleton

import com.github.tidetune.database.StorageDao
import com.github.tidetune.database.StorageEntity
import com.github.tidetune.platform.currentTimeMillis
import com.github.tidetune.security.CredentialStore
import com.github.tidetune.security.StoredCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.tidetune_core.ArgUpsertStorage
import uniffi.tidetune_core.Storage
import uniffi.tidetune_core.StorageConnectionTestResult
import uniffi.tidetune_core.OneDriveDriveList
import uniffi.tidetune_core.ctExchangeOnedriveCode
import uniffi.tidetune_core.ctListOnedriveDrives
import uniffi.tidetune_core.ctStartOnedriveOauth
import uniffi.tidetune_core.ctTestStorage
import uniffi.tidetune_core.StorageId
import uniffi.tidetune_core.StorageType


class StorageRepository(
    private val bridge: Bridge,
    private val scope: CoroutineScope,
    private val storageDao: StorageDao,
    private val credentialStore: CredentialStore,
) {
    private val _oauthRefreshToken = MutableStateFlow("")
    private val _storages = MutableStateFlow(listOf<Storage>())
    private val _preRemoveStorageEvent = MutableSharedFlow<StorageId>()
    private val _onRemoveStorageEvent = MutableSharedFlow<Unit>()

    val oauthRefreshToken = _oauthRefreshToken.asStateFlow()
    val storages = _storages.asStateFlow()
    val preRemoveStorageEvent = _preRemoveStorageEvent.asSharedFlow()
    val onRemoveStorageEvent = _onRemoveStorageEvent.asSharedFlow()

    init {
        scope.launch {
            ensureLocalStorage()
        }
        scope.launch {
            storageDao.observeAll().collect { entities ->
                _storages.value = entities.map { entity ->
                    entity.toStorage(password = "")
                }
            }
        }
    }

    suspend fun startOneDriveOAuth(): String {
        val session = ctStartOnedriveOauth()
        credentialStore.save(
            PENDING_ONEDRIVE_OAUTH_CREDENTIAL_ID,
            StoredCredential(
                username = session.state,
                secret = session.codeVerifier,
                isAnonymous = false,
            )
        )
        return session.authorizationUrl
    }

    suspend fun updateRefreshToken(code: String, state: String): Boolean {
        val pending = credentialStore.load(PENDING_ONEDRIVE_OAUTH_CREDENTIAL_ID)
            ?: return false
        if (pending.username != state) return false

        credentialStore.delete(PENDING_ONEDRIVE_OAUTH_CREDENTIAL_ID)
        val token = bridge.run {
            ctExchangeOnedriveCode(it, code, pending.secret)
        } ?: return false
        _oauthRefreshToken.value = token
        return true
    }

    suspend fun test(arg: ArgUpsertStorage): StorageConnectionTestResult {
        return bridge.runRaw { ctTestStorage(it, arg) }
    }

    suspend fun listOneDriveDrives(refreshToken: String): OneDriveDriveList {
        return bridge.runRaw { ctListOnedriveDrives(refreshToken) }
    }

    suspend fun updateOneDriveRefreshToken(storageId: StorageId, refreshToken: String) {
        val current = credentialStore.load(storageId.value) ?: return
        if (current.secret == refreshToken) return
        val rotated = current.copy(secret = refreshToken)
        credentialStore.save(storageId.value, rotated)
    }

    fun receiveOneDriveOAuthRedirect(code: String, state: String) {
        scope.launch {
            updateRefreshToken(code, state)
        }
    }

    suspend fun upsertStorage(arg: ArgUpsertStorage) {
        val normalized = arg.normalized()
        val id = normalized.id ?: StorageId((storageDao.maxId() ?: 0L) + 1L)
        val now = currentTimeMillis()
        val previous = storageDao.get(id.value)
        val credential = StoredCredential(
            username = normalized.username,
            secret = normalized.password,
            isAnonymous = normalized.isAnonymous,
        )
        credentialStore.save(id.value, credential)
        storageDao.upsert(
            StorageEntity(
                id = id.value,
                type = normalized.typ.name,
                displayName = normalized.alias.ifBlank {
                    if (normalized.typ == StorageType.LOCAL) "Local" else normalized.addr
                },
                baseUrl = if (normalized.typ == StorageType.ONE_DRIVE) "" else normalized.addr,
                driveId = if (normalized.typ == StorageType.ONE_DRIVE) normalized.addr.ifBlank { null } else null,
                credentialRef = previous?.credentialRef ?: "storage-${id.value}",
                username = normalized.username,
                isAnonymous = normalized.isAnonymous,
                musicCount = previous?.musicCount ?: 0L,
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
            )
        )
    }

    suspend fun remove(id: StorageId) {
        _preRemoveStorageEvent.emit(id)
        credentialStore.delete(id.value)
        storageDao.delete(id.value)
        _onRemoveStorageEvent.emit(Unit)
    }

    suspend fun reload() {
        ensureLocalStorage()
    }

    suspend fun loadCredential(id: StorageId): StoredCredential? {
        if (storageDao.get(id.value)?.type == StorageType.LOCAL.name) return null
        return credentialStore.load(id.value)
    }

    suspend fun storageForRust(id: StorageId): Storage? {
        val entity = storageDao.get(id.value) ?: return null
        val credential = loadCredential(id)
        return entity.toStorage(password = credential?.secret.orEmpty())
            .copyCredential(credential)
    }

    private suspend fun ensureLocalStorage() {
        if (storageDao.get(LOCAL_STORAGE_ID) != null) return
        val now = currentTimeMillis()
        storageDao.upsert(
            StorageEntity(
                id = LOCAL_STORAGE_ID,
                type = StorageType.LOCAL.name,
                displayName = "Local",
                baseUrl = "",
                driveId = null,
                credentialRef = "storage-$LOCAL_STORAGE_ID",
                username = "",
                isAnonymous = true,
                musicCount = 0,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    private fun StorageEntity.toStorage(password: String): Storage {
        return Storage(
            id = StorageId(id),
            addr = driveId ?: baseUrl,
            alias = displayName,
            username = username,
            password = password,
            isAnonymous = isAnonymous,
            typ = StorageType.valueOf(type),
            musicCount = musicCount.coerceAtLeast(0).toULong(),
        )
    }

    private fun ArgUpsertStorage.normalized(): ArgUpsertStorage {
        return if (isAnonymous) {
            copy(username = "", password = "")
        } else {
            this
        }
    }

    private fun Storage.copyCredential(credential: StoredCredential?): Storage {
        return if (credential == null) {
            this
        } else {
            copy(
                username = credential.username,
                password = credential.secret,
                isAnonymous = credential.isAnonymous,
            )
        }
    }

}

private const val PENDING_ONEDRIVE_OAUTH_CREDENTIAL_ID = Long.MIN_VALUE
private const val LOCAL_STORAGE_ID = 1L
