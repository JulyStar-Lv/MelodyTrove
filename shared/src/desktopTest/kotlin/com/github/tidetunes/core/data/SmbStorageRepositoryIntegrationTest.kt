package com.github.tidetunes.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.core.data.security.CredentialStore
import com.github.tidetunes.core.domain.model.SourceEditorDraft
import com.github.tidetunes.core.domain.model.SourceEditorType
import com.github.tidetunes.core.domain.model.StoredCredential
import com.github.tidetunes.core.domain.model.toStorageRouteIdOrNull
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.singleton.Bridge
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import uniffi.tidetunes_backend.StorageType

class SmbStorageRepositoryIntegrationTest {
    @Test
    fun smbAccountPersistsStructuredConfigurationAndSecureCredential() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<TideTunesDatabase> {
            TideTunesDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val credentialStore = InMemoryCredentialStore()
        val tempDir = Files.createTempDirectory("tidetunes-smb-account").toFile()
        try {
            val repository = StorageRepositoryImpl(
                bridge = Bridge(
                    appDocumentDir = tempDir.absolutePath,
                    appCacheDir = tempDir.absolutePath,
                    toastRepository = ToastRepositoryImpl(scope),
                ),
                scope = scope,
                sourceAccountDao = database.sourceAccountDao(),
                credentialStore = credentialStore,
            )
            withTimeout(5_000) {
                while (database.sourceAccountDao().get(1) == null) delay(10)
            }
            val draft = SourceEditorDraft(
                alias = "Studio NAS",
                username = "alice",
                secret = "secret-password",
                storageType = SourceEditorType.Smb,
                smbHost = "nas.local",
                smbPort = 1445,
                smbShare = "Music Share",
                smbRootPath = "Library",
                smbDomain = "STUDIO",
                smbRequireSigning = true,
                smbRequireEncryption = true,
            )

            val accountId = repository.upsertSource(draft)
            val storageId = assertNotNull(accountId.toStorageRouteIdOrNull())
            val entity = assertNotNull(database.sourceAccountDao().get(storageId))
            val credential = assertNotNull(credentialStore.load(storageId))
            val rustStorage = assertNotNull(
                repository.storageForRust(uniffi.tidetunes_backend.StorageId(storageId))
            )
            val editor = assertNotNull(repository.loadEditorState(storageId))

            assertEquals(ProviderTypes.Smb, entity.providerType)
            assertEquals("nas.local", entity.endpoint)
            assertTrue(entity.providerConfig.orEmpty().contains("\"share\":\"Music Share\""))
            assertFalse(entity.providerConfig.orEmpty().contains("secret-password"))
            assertEquals("alice", credential.username)
            assertEquals("secret-password", credential.secret)
            assertEquals(StorageType.SMB, rustStorage.typ)
            assertEquals(
                "smb://nas.local:1445/Music%20Share/Library" +
                    "?domain=STUDIO&signing=true&encryption=true",
                rustStorage.addr,
            )
            assertFalse(rustStorage.addr.contains("secret-password"))
            assertEquals("", editor.draft.secret)
            assertEquals("Music Share", editor.draft.smbShare)

            repository.upsertSource(
                draft.copy(
                    id = storageId,
                    alias = "Renamed NAS",
                    secret = "",
                )
            )
            assertEquals("secret-password", credentialStore.load(storageId)?.secret)

            repository.removeByAccountId(accountId)
            assertNull(database.sourceAccountDao().get(storageId))
            assertNull(credentialStore.load(storageId))
        } finally {
            scope.cancel()
            database.close()
            tempDir.deleteRecursively()
        }
    }
}

private class InMemoryCredentialStore : CredentialStore {
    private val values = mutableMapOf<Long, StoredCredential>()

    override suspend fun load(storageId: Long): StoredCredential? = values[storageId]

    override suspend fun save(
        storageId: Long,
        credential: StoredCredential,
    ) {
        values[storageId] = credential
    }

    override suspend fun delete(storageId: Long) {
        values.remove(storageId)
    }

    override suspend fun clear() {
        values.clear()
    }
}
