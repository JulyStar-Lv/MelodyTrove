package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.OneDriveDriveInfo
import com.github.tidetunes.core.domain.model.toStorageRouteIdOrNull
import com.github.tidetunes.core.domain.model.STORAGE_ACCOUNT_PREFIX
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.database.StorageDao
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.database.StorageEntity

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
import com.github.tidetunes.feature.sources.presentation.SourceConnectionTestStatus
import com.github.tidetunes.feature.sources.presentation.SourceEditorDraft
import com.github.tidetunes.feature.sources.presentation.SourceEditorType


class StorageRepositoryImpl(
    private val bridge: Bridge,
    private val scope: CoroutineScope,
    private val storageDao: StorageDao,
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
            storageDao.observeAll().collect { entities ->
                _storages.value = entities.map { entity ->
                    entity.toStorage(password = "")
                }
                _storageAccounts.value = _storages.value.map { storage ->
                    storage.toStorageAccountInfo()
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

    override suspend fun reload() {
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

    internal data class StorageEditorState(
        val accountId: SourceAccountId,
        val draft: SourceEditorDraft,
        val title: String,
        val musicCount: ULong,
        val isOneDrive: Boolean,
    )

    internal suspend fun loadEditorState(id: Long): StorageEditorState? {
        val storage = _storages.value.find { it.id.value == id } ?: return null
        val credential = loadCredential(StorageId(id))
        val accountId = SourceAccountId("$STORAGE_ACCOUNT_PREFIX$id")
        return StorageEditorState(
            accountId = accountId,
            draft = storage.copy(password = "").toSourceEditorDraft(),
            title = storage.displayNameForEditor(),
            musicCount = storage.musicCount,
            isOneDrive = storage.typ == StorageType.ONE_DRIVE,
        )
    }

    internal suspend fun testSource(draft: SourceEditorDraft): SourceConnectionTestStatus {
        return test(draft.toArgUpsertStorage()).toSourceConnectionTestStatus()
    }

    internal data class OneDriveDriveListResult(
        val drives: List<OneDriveDriveInfo>,
        val refreshedToken: String,
    )

    internal suspend fun listOneDriveDriveInfos(refreshToken: String): OneDriveDriveListResult {
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

    suspend fun updateOneDriveRefreshTokenByAccountId(accountId: SourceAccountId, refreshToken: String) {
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
        return value
            .takeIf { it.startsWith(STORAGE_ACCOUNT_PREFIX) }
            ?.removePrefix(STORAGE_ACCOUNT_PREFIX)
            ?.toLongOrNull()
            ?.let { StorageId(it) }
    }


    private fun Storage.displayNameForEditor(): String {
        return alias.ifBlank { addr }
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
        accountId = SourceAccountId("storage:${id.value}"),
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
    return SourceAccountId("$STORAGE_ACCOUNT_PREFIX$value")
}

