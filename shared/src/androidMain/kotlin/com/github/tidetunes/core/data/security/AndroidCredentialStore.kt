package com.github.tidetunes.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.github.tidetunes.core.domain.model.StoredCredential
import android.util.Base64
import com.github.tidetunes.platform.appContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

private const val KEY_ALIAS = "TideTunesCredentialKey"
private const val PREFERENCES = "tidetunes_secure_credentials"

actual fun createCredentialStore(): CredentialStore = AndroidCredentialStore()

private class AndroidCredentialStore : CredentialStore {
    private val preferences = appContext.getSharedPreferences(PREFERENCES, 0)

    override suspend fun load(storageId: Long): StoredCredential? {
        val encoded = preferences.getString(storageId.toString(), null) ?: return null
        val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
        require(encrypted.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, encrypted.copyOfRange(0, 12)),
        )
        return decodeCredential(
            cipher.doFinal(encrypted.copyOfRange(12, encrypted.size))
                .toString(StandardCharsets.UTF_8)
        )
    }

    override suspend fun save(storageId: Long, credential: StoredCredential) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.iv + cipher.doFinal(
            encodeCredential(credential).toByteArray(StandardCharsets.UTF_8)
        )
        preferences.edit {
            putString(storageId.toString(), Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
    }

    override suspend fun delete(storageId: Long) {
        preferences.edit { remove(storageId.toString()) }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}

private fun encodeCredential(value: StoredCredential): String {
    return listOf(value.username, value.secret, value.isAnonymous.toString())
        .joinToString("\u0000")
}

private fun decodeCredential(value: String): StoredCredential {
    val fields = value.split('\u0000', limit = 3)
    require(fields.size == 3)
    return StoredCredential(fields[0], fields[1], fields[2].toBooleanStrict())
}
