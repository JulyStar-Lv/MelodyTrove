package io.github.julystar.musicapp.core.data.security

import cnames.structs.__CFData
import kotlinx.cinterop.ExperimentalForeignApi
import io.github.julystar.musicapp.core.domain.model.StoredCredential
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.*
import platform.Security.*
import io.github.julystar.musicapp.migration.AppIdentifiers
import io.github.julystar.musicapp.migration.LegacyCredentialIds

actual fun createCredentialStore(): CredentialStore = IosCredentialStore()

@OptIn(ExperimentalForeignApi::class)
private class IosCredentialStore : CredentialStore {
    private val service = AppIdentifiers.CREDENTIAL_SERVICE

    override suspend fun load(storageId: Long): StoredCredential? {
        loadFromService(storageId, service)?.let { return it }
        val credential = loadFromService(storageId, LegacyCredentialIds.SERVICE) ?: return null
        saveToService(storageId, credential, service)
        check(loadFromService(storageId, service) == credential) {
            "Unable to verify migrated iOS credential"
        }
        deleteFromService(storageId, LegacyCredentialIds.SERVICE)
        return credential
    }

    private fun loadFromService(
        storageId: Long,
        credentialService: String,
    ): StoredCredential? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(storageId, credentialService, includeResult = true) { query ->
            SecItemCopyMatching(query, result.ptr)
        }
        if (status == errSecItemNotFound) return@memScoped null
        check(status == errSecSuccess) { "Keychain read failed: $status" }
        val value = result.value ?: return@memScoped null
        val data = value.reinterpret<__CFData>()
        val bytes = CFDataGetBytePtr(data)?.readBytes(CFDataGetLength(data).toInt())
            ?: return@memScoped null
        CFRelease(value)
        decodeCredential(bytes.decodeToString())
    }

    override suspend fun save(storageId: Long, credential: StoredCredential) {
        saveToService(storageId, credential, service)
    }

    private fun saveToService(
        storageId: Long,
        credential: StoredCredential,
        credentialService: String,
    ) {
        deleteFromService(storageId, credentialService)
        val bytes = encodeCredential(credential).encodeToByteArray()
        val status = bytes.usePinned { pinned ->
            val data = CFDataCreate(
                null,
                pinned.addressOf(0).reinterpret(),
                bytes.size.toLong(),
            ) ?: error("Unable to allocate Keychain value")
            try {
                withQuery(storageId, credentialService, includeResult = false) { query ->
                    CFDictionarySetValue(query, kSecValueData, data)
                    CFDictionarySetValue(
                        query,
                        kSecAttrAccessible,
                        kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    )
                    SecItemAdd(query, null)
                }
            } finally {
                CFRelease(data)
            }
        }
        check(status == errSecSuccess) { "Keychain write failed: $status" }
    }

    override suspend fun delete(storageId: Long) {
        deleteFromService(storageId, service)
        deleteFromService(storageId, LegacyCredentialIds.SERVICE)
    }

    private fun deleteFromService(storageId: Long, credentialService: String) {
        val status = withQuery(
            storageId,
            credentialService,
            includeResult = false,
            block = ::SecItemDelete,
        )
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "Keychain delete failed: $status"
        }
    }

    override suspend fun clear() {
        clearService(service)
        clearService(LegacyCredentialIds.SERVICE)
    }

    private fun clearService(credentialService: String) {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
            ?: error("Unable to allocate Keychain query")
        val serviceValue = CFStringCreateWithCString(
            null,
            credentialService,
            kCFStringEncodingUTF8,
        )
            ?: error("Unable to allocate Keychain service")
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceValue)
            val status = SecItemDelete(query)
            check(status == errSecSuccess || status == errSecItemNotFound) {
                "Keychain clear failed: $status"
            }
        } finally {
            CFRelease(serviceValue)
            CFRelease(query)
        }
    }

    private inline fun <T> withQuery(
        storageId: Long,
        credentialService: String,
        includeResult: Boolean,
        block: (CFMutableDictionaryRef) -> T,
    ): T {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
            ?: error("Unable to allocate Keychain query")
        val serviceValue = CFStringCreateWithCString(
            null,
            credentialService,
            kCFStringEncodingUTF8,
        )
            ?: error("Unable to allocate Keychain service")
        val accountValue = CFStringCreateWithCString(
            null,
            "storage-$storageId",
            kCFStringEncodingUTF8,
        ) ?: error("Unable to allocate Keychain account")
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceValue)
            CFDictionarySetValue(query, kSecAttrAccount, accountValue)
            if (includeResult) {
                CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
                CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            }
            return block(query)
        } finally {
            CFRelease(accountValue)
            CFRelease(serviceValue)
            CFRelease(query)
        }
    }
}

private fun encodeCredential(value: StoredCredential): String {
    return listOf(value.username, value.secret, value.isAnonymous.toString()).joinToString("\u0000")
}

private fun decodeCredential(value: String): StoredCredential {
    val fields = value.split('\u0000', limit = 3)
    require(fields.size == 3)
    return StoredCredential(fields[0], fields[1], fields[2].toBooleanStrict())
}
