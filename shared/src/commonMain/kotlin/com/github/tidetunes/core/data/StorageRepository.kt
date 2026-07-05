package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.OneDriveDriveInfo
import com.github.tidetunes.core.domain.model.OneDriveDriveListResult
import com.github.tidetunes.core.domain.model.SourceConnectionTestStatus
import com.github.tidetunes.core.domain.model.SourceEditorDraft
import com.github.tidetunes.core.domain.model.SourceEditorStorageState
import com.github.tidetunes.core.domain.model.SourceEditorType
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.core.domain.model.toStorageRouteIdOrNull
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.SourceAccountDao
import com.github.tidetunes.database.SourceAccountEntity

import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.core.data.security.CredentialStore
import com.github.tidetunes.core.domain.model.StoredCredential
import com.github.tidetunes.source.api.BuiltInSourceIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uniffi.tidetunes_core.ArgUpsertStorage
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageConnectionTestResult
import uniffi.tidetunes_core.OneDriveDriveList
import uniffi.tidetunes_core.ctExchangeOnedriveCode
import uniffi.tidetunes_core.ctListOnedriveDrives
import uniffi.tidetunes_core.ctStartOnedriveOauth
import uniffi.tidetunes_core.ctTestStorage
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType


class StorageRepositoryImpl(
    private val bridge: Bridge,
    private val scope: CoroutineScope,
    private val sourceAccountDao: SourceAccountDao,
    private val credentialStore: CredentialStore,
) : StorageRepository {
    private val _oauthRefreshToken = MutableStateFlow("")
    private val _storages = MutableStateFlow(listOf<Storage>())
    private val _storageAccounts = MutableStateFlow(listOf<StorageAccountInfo>())
    private val _preRemoveStorageEvent = MutableSharedFlow<StorageId>()
    private val _onRemoveStorageEvent = MutableSharedFlow<Unit>()

    override val oauthRefreshToken = _oauthRefreshToken.asStateFlow()
    val storages = _storages.asStateFlow()
    override val storageAccounts = _storageAccounts.asStateFlow()
    val preRemoveStorageEvent = _preRemoveStorageEvent.asSharedFlow()
    override val onRemoveStorageEvent = _onRemoveStorageEvent.asSharedFlow()

    init {
        scope.launch {
            ensureLocalStorage()
        }
        scope.launch {
            sourceAccountDao.observeAll().collect { entities ->
                _storages.value = entities.map { entity ->
                    entity.toStorage(password = "")
                }
                _storageAccounts.value = _storages.value.map { storage ->
                    storage.toStorageAccountInfo()
                }
            }
        }
    }

    override suspend fun startOneDriveOAuth(): String {
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

    suspend fun upsertStorage(arg: ArgUpsertStorage): StorageId {
        val normalized = arg.normalized()
        val id = normalized.id ?: StorageId((sourceAccountDao.maxId() ?: 0L) + 1L)
        val now = currentTimeMillis()
        val previous = sourceAccountDao.get(id.value)
        val credential = StoredCredential(
            username = normalized.username,
            secret = normalized.password,
            isAnonymous = normalized.isAnonymous,
        )
        credentialStore.save(id.value, credential)
        sourceAccountDao.upsert(
            SourceAccountEntity(
                id = id.value,
                providerType = normalized.typ.toProviderType(),
                displayName = normalized.alias.ifBlank {
                    if (normalized.typ == StorageType.LOCAL) "Local" else normalized.addr
                },
                endpoint = normalized.addr.takeIf { it.isNotBlank() },
                externalAccountId = if (normalized.typ == StorageType.ONE_DRIVE) {
                    normalized.addr.ifBlank { null }
                } else {
                    null
                },
                credentialRef = previous?.credentialRef ?: "storage-${id.value}",
                priority = previous?.priority ?: 0,
                enabled = true,
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
            )
        )
        return id
    }

    override suspend fun upsertSource(draft: SourceEditorDraft): SourceAccountId {
        return storageSourceAccountId(upsertStorage(draft.toArgUpsertStorage()).value)
    }

    suspend fun remove(id: StorageId) {
        _preRemoveStorageEvent.emit(id)
        credentialStore.delete(id.value)
        sourceAccountDao.delete(id.value)
        _onRemoveStorageEvent.emit(Unit)
    }

    override suspend fun reload() {
        ensureLocalStorage()
    }

    suspend fun loadCredential(id: StorageId): StoredCredential? {
        if (sourceAccountDao.get(id.value)?.providerType == ProviderTypes.Local) return null
        return credentialStore.load(id.value)
    }

    suspend fun storageForRust(id: StorageId): Storage? {
        val entity = sourceAccountDao.get(id.value) ?: return null
        val credential = loadCredential(id)
        return entity.toStorage(password = credential?.secret.orEmpty())
            .copyCredential(credential)
    }

    private suspend fun ensureLocalStorage() {
        if (sourceAccountDao.get(LOCAL_STORAGE_ID) != null) return
        val now = currentTimeMillis()
        sourceAccountDao.upsert(
            SourceAccountEntity(
                id = LOCAL_STORAGE_ID,
                providerType = ProviderTypes.Local,
                displayName = "Local",
                endpoint = null,
                externalAccountId = null,
                credentialRef = "storage-$LOCAL_STORAGE_ID",
                priority = 0,
                enabled = true,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    private fun SourceAccountEntity.toStorage(password: String): Storage {
        return Storage(
            id = StorageId(id),
            addr = externalAccountId ?: endpoint.orEmpty(),
            alias = displayName,
            username = "",
            password = password,
            isAnonymous = providerType == ProviderTypes.Local,
            typ = providerType.toStorageType(),
            musicCount = 0u,
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

    override suspend fun loadEditorState(id: Long): SourceEditorStorageState? {
        val storage = _storages.value.find { it.id.value == id } ?: return null
        val credential = loadCredential(StorageId(id))
        val accountId = storageSourceAccountId(id)
        return SourceEditorStorageState(
            accountId = accountId,
            draft = storage.copy(password = "").toSourceEditorDraft(),
            title = storage.displayNameForEditor(),
            musicCount = storage.musicCount,
            isOneDrive = storage.typ == StorageType.ONE_DRIVE,
        )
    }

    override suspend fun testSource(draft: SourceEditorDraft): SourceConnectionTestStatus {
        return test(draft.toArgUpsertStorage()).toSourceConnectionTestStatus()
    }

    override suspend fun listOneDriveDriveInfos(refreshToken: String): OneDriveDriveListResult {
        val result = listOneDriveDrives(refreshToken)
        return OneDriveDriveListResult(
            drives = result.drives.map { drive ->
                OneDriveDriveInfo(
                    id = drive.id,
                    name = drive.name,
                )
            },
            refreshedToken = result.refreshToken,
        )
    }

    override suspend fun removeByAccountId(accountId: SourceAccountId) {
        val id = accountId.toStorageIdOrNull() ?: return
        remove(id)
    }

    override suspend fun updateOneDriveRefreshTokenByAccountId(accountId: SourceAccountId, refreshToken: String) {
        val id = accountId.toStorageIdOrNull() ?: return
        updateOneDriveRefreshToken(id, refreshToken)
    }

    override suspend fun loadCredentialByAccountId(accountId: SourceAccountId): StoredCredential? {
        val id = accountId.toStorageIdOrNull() ?: return null
        return loadCredential(id)
    }

    fun findStorageAccount(id: Long): StorageAccountInfo? {
        return _storageAccounts.value.find { it.accountId.toStorageRouteIdOrNull() == id }
    }

    override fun findStorageAccountByAccountId(accountId: SourceAccountId): StorageAccountInfo? {
        return _storageAccounts.value.find { it.accountId == accountId }
    }

    private fun SourceAccountId.toStorageIdOrNull(): StorageId? {
        return toStorageRouteIdOrNull()?.let { StorageId(it) }
    }


    private fun Storage.displayNameForEditor(): String {
        return alias.ifBlank { addr }
    }

}

private fun StorageType.toProviderType(): String {
    return when (this) {
        StorageType.LOCAL -> ProviderTypes.Local
        StorageType.WEBDAV -> ProviderTypes.WebDav
        StorageType.ONE_DRIVE -> ProviderTypes.OneDrive
    }
}

private fun String.toStorageType(): StorageType {
    return when (this) {
        ProviderTypes.Local -> StorageType.LOCAL
        ProviderTypes.WebDav -> StorageType.WEBDAV
        ProviderTypes.OneDrive -> StorageType.ONE_DRIVE
        else -> StorageType.WEBDAV
    }
}

private const val PENDING_ONEDRIVE_OAUTH_CREDENTIAL_ID = Long.MIN_VALUE
private const val LOCAL_STORAGE_ID = 1L

fun Storage.toStorageAccountInfo(): StorageAccountInfo {
    val sourceId = when (typ) {
        StorageType.LOCAL -> BuiltInSourceIds.Local
        StorageType.WEBDAV -> BuiltInSourceIds.WebDav
        StorageType.ONE_DRIVE -> BuiltInSourceIds.OneDrive
    }
    return StorageAccountInfo(
        accountId = storageSourceAccountId(id.value),
        sourceId = sourceId,
        isLocal = typ == StorageType.LOCAL,
        isOneDrive = typ == StorageType.ONE_DRIVE,
        title = alias.ifBlank { addr },
        subtitle = addr.ifBlank { alias },
        musicCount = musicCount.toLong(),
    )
}
fun SourceConnectionTestStatus.toStorageConnectionTestResult(): StorageConnectionTestResult {
    return when (this) {
        SourceConnectionTestStatus.None -> StorageConnectionTestResult.NONE
        SourceConnectionTestStatus.Testing -> StorageConnectionTestResult.TESTING
        SourceConnectionTestStatus.Success -> StorageConnectionTestResult.SUCCESS
        SourceConnectionTestStatus.Error -> StorageConnectionTestResult.OTHER_ERROR
    }
}

fun StorageConnectionTestResult.toSourceConnectionTestStatus(): SourceConnectionTestStatus {
    return when (this) {
        StorageConnectionTestResult.NONE -> SourceConnectionTestStatus.None
        StorageConnectionTestResult.TESTING -> SourceConnectionTestStatus.Testing
        StorageConnectionTestResult.SUCCESS -> SourceConnectionTestStatus.Success
        else -> SourceConnectionTestStatus.Error
    }
}

fun uniffi.tidetunes_core.StorageId.toSourceAccountId(): SourceAccountId {
    return storageSourceAccountId(value)
}
