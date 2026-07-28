package io.github.julystar.musicapp.core.data.security

import java.nio.charset.StandardCharsets
import java.io.File
import java.util.Base64
import io.github.julystar.musicapp.core.domain.model.StoredCredential
import io.github.julystar.musicapp.migration.AppIdentifiers
import io.github.julystar.musicapp.migration.LegacyCredentialIds
import io.github.julystar.musicapp.migration.LegacyPaths
import io.github.julystar.musicapp.platform.getAppDataDirectory

actual fun createCredentialStore(): CredentialStore = DesktopCredentialStore()

private class DesktopCredentialStore : CredentialStore {
    private val service = AppIdentifiers.CREDENTIAL_SERVICE

    override suspend fun load(storageId: Long): StoredCredential? {
        val account = account(storageId)
        loadEncoded(account, service, legacy = false)?.let(::decodeCredential)?.let { return it }
        val encoded = loadEncoded(
            account,
            LegacyCredentialIds.SERVICE,
            legacy = true,
        ) ?: return null
        val credential = decodeCredential(encoded)
        save(storageId, credential)
        check(
            loadEncoded(account, service, legacy = false)
                ?.let(::decodeCredential) == credential
        ) { "Unable to verify migrated Desktop credential" }
        deleteEncoded(account, LegacyCredentialIds.SERVICE, legacy = true)
        return credential
    }

    private fun loadEncoded(
        account: String,
        credentialService: String,
        legacy: Boolean,
    ): String? {
        return when {
            isMac() -> run(
                listOf(
                    "/usr/bin/security",
                    "find-generic-password",
                    "-s",
                    credentialService,
                    "-a",
                    account,
                    "-w",
                )
            )
                .takeIf { it.exitCode == 0 }?.stdout?.trim()
            isLinux() -> run(
                listOf("secret-tool", "lookup", "service", credentialService, "account", account)
            )
                .takeIf { it.exitCode == 0 }?.stdout?.trim()
            isWindows() -> loadWindows(account, legacy)
            else -> null
        }
    }

    override suspend fun save(storageId: Long, credential: StoredCredential) {
        val account = account(storageId)
        val encoded = encodeCredential(credential)
        val result = when {
            isMac() -> run(
                listOf(
                    "/usr/bin/security", "add-generic-password", "-U",
                    "-s", service, "-a", account, "-w", encoded,
                )
            )
            isLinux() -> run(
                listOf(
                    "secret-tool",
                    "store",
                    "--label=${AppIdentifiers.BRAND_NAME}",
                    "service",
                    service,
                    "account",
                    account,
                ),
                stdin = encoded,
            )
            isWindows() -> saveWindows(account, encoded, legacy = false)
            else -> error("No supported secure credential provider on ${System.getProperty("os.name")}")
        }
        check(result.exitCode == 0) { result.stderr.ifBlank { "Credential store failed" } }
    }

    override suspend fun delete(storageId: Long) {
        val account = account(storageId)
        deleteEncoded(account, service, legacy = false)
        deleteEncoded(account, LegacyCredentialIds.SERVICE, legacy = true)
    }

    override suspend fun clear() {
        when {
            isMac() -> {
                listOf(service, LegacyCredentialIds.SERVICE).forEach { credentialService ->
                    while (
                        run(
                            listOf(
                                "/usr/bin/security",
                                "delete-generic-password",
                                "-s",
                                credentialService,
                            )
                        ).exitCode == 0
                    ) {
                        // Remove every item for this application service.
                    }
                }
            }
            isLinux() -> {
                run(listOf("secret-tool", "clear", "service", service))
                run(listOf("secret-tool", "clear", "service", LegacyCredentialIds.SERVICE))
            }
            isWindows() -> {
                credentialDirectory(legacy = false).deleteRecursively()
                credentialDirectory(legacy = true).deleteRecursively()
            }
        }
    }

    private fun deleteEncoded(account: String, credentialService: String, legacy: Boolean) {
        when {
            isMac() -> run(
                listOf(
                    "/usr/bin/security",
                    "delete-generic-password",
                    "-s",
                    credentialService,
                    "-a",
                    account,
                )
            )
            isLinux() -> run(
                listOf("secret-tool", "clear", "service", credentialService, "account", account)
            )
            isWindows() -> credentialFile(account, legacy).delete()
        }
    }

    private fun account(storageId: Long) = "storage-$storageId"
    private fun isMac() = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
    private fun isLinux() = System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
    private fun isWindows() = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private fun loadWindows(account: String, legacy: Boolean): String? {
        val file = credentialFile(account, legacy)
        if (!file.isFile) return null
        val result = run(
            listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", WINDOWS_UNPROTECT_COMMAND),
            stdin = file.readText(StandardCharsets.UTF_8),
        )
        check(result.exitCode == 0) { result.stderr.ifBlank { "Credential store failed" } }
        return result.stdout.trim().takeIf { it.isNotBlank() }
    }

    private fun saveWindows(account: String, encoded: String, legacy: Boolean): CommandResult {
        val result = run(
            listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", WINDOWS_PROTECT_COMMAND),
            stdin = encoded,
        )
        if (result.exitCode == 0) {
            val directory = credentialDirectory(legacy)
            directory.mkdirs()
            credentialFile(account, legacy).writeText(result.stdout.trim(), StandardCharsets.UTF_8)
        }
        return result
    }

    private fun credentialFile(account: String, legacy: Boolean): File {
        return File(credentialDirectory(legacy), "$account.dpapi")
    }

    private fun credentialDirectory(legacy: Boolean): File {
        val root = if (legacy) {
            File(System.getProperty("user.home"), LegacyPaths.DESKTOP_DATA_DIRECTORY)
        } else {
            File(getAppDataDirectory())
        }
        return File(root, "credentials")
    }
}

private data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)

private fun run(command: List<String>, stdin: String? = null): CommandResult {
    val process = ProcessBuilder(command).start()
    if (stdin != null) {
        process.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(stdin) }
    } else {
        process.outputStream.close()
    }
    val stdout = process.inputStream.bufferedReader().use { it.readText() }
    val stderr = process.errorStream.bufferedReader().use { it.readText() }
    return CommandResult(process.waitFor(), stdout, stderr)
}

private fun encodeCredential(value: StoredCredential): String {
    val raw = listOf(value.username, value.secret, value.isAnonymous.toString()).joinToString("\u0000")
    return Base64.getEncoder().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
}

private fun decodeCredential(encoded: String): StoredCredential {
    val raw = String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)
    val fields = raw.split('\u0000', limit = 3)
    require(fields.size == 3)
    return StoredCredential(fields[0], fields[1], fields[2].toBooleanStrict())
}

private const val WINDOWS_PROTECT_COMMAND = """
try {
    Add-Type -AssemblyName System.Security
    ${'$'}encoded = [Console]::In.ReadToEnd().Trim()
    ${'$'}plain = [Convert]::FromBase64String(${'$'}encoded)
    ${'$'}protected = [System.Security.Cryptography.ProtectedData]::Protect(
        ${'$'}plain,
        ${'$'}null,
        [System.Security.Cryptography.DataProtectionScope]::CurrentUser
    )
    [Console]::Out.Write([Convert]::ToBase64String(${'$'}protected))
    exit 0
} catch {
    [Console]::Error.Write(${'$'}_.Exception.Message)
    exit 1
}
"""

private const val WINDOWS_UNPROTECT_COMMAND = """
try {
    Add-Type -AssemblyName System.Security
    ${'$'}encoded = [Console]::In.ReadToEnd().Trim()
    ${'$'}protected = [Convert]::FromBase64String(${'$'}encoded)
    ${'$'}plain = [System.Security.Cryptography.ProtectedData]::Unprotect(
        ${'$'}protected,
        ${'$'}null,
        [System.Security.Cryptography.DataProtectionScope]::CurrentUser
    )
    [Console]::Out.Write([Convert]::ToBase64String(${'$'}plain))
    exit 0
} catch {
    [Console]::Error.Write(${'$'}_.Exception.Message)
    exit 1
}
"""
