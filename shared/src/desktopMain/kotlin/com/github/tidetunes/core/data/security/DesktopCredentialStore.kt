package com.github.tidetunes.core.data.security

import java.nio.charset.StandardCharsets
import java.io.File
import java.util.Base64
import com.github.tidetunes.core.domain.model.StoredCredential

actual fun createCredentialStore(): CredentialStore = DesktopCredentialStore()

private class DesktopCredentialStore : CredentialStore {
    private val service = "com.github.tidetunes"

    override suspend fun load(storageId: Long): StoredCredential? {
        val account = account(storageId)
        val encoded = when {
            isMac() -> run(listOf("/usr/bin/security", "find-generic-password", "-s", service, "-a", account, "-w"))
                .takeIf { it.exitCode == 0 }?.stdout?.trim()
            isLinux() -> run(listOf("secret-tool", "lookup", "service", service, "account", account))
                .takeIf { it.exitCode == 0 }?.stdout?.trim()
            isWindows() -> loadWindows(account)
            else -> null
        } ?: return null
        return decodeCredential(encoded)
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
                listOf("secret-tool", "store", "--label=TideTunes", "service", service, "account", account),
                stdin = encoded,
            )
            isWindows() -> saveWindows(account, encoded)
            else -> error("No supported secure credential provider on ${System.getProperty("os.name")}")
        }
        check(result.exitCode == 0) { result.stderr.ifBlank { "Credential store failed" } }
    }

    override suspend fun delete(storageId: Long) {
        val account = account(storageId)
        when {
            isMac() -> run(listOf("/usr/bin/security", "delete-generic-password", "-s", service, "-a", account))
            isLinux() -> run(listOf("secret-tool", "clear", "service", service, "account", account))
            isWindows() -> credentialFile(account).delete()
        }
    }

    private fun account(storageId: Long) = "storage-$storageId"
    private fun isMac() = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
    private fun isLinux() = System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
    private fun isWindows() = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private fun loadWindows(account: String): String? {
        val file = credentialFile(account)
        if (!file.isFile) return null
        val result = run(
            listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", WINDOWS_UNPROTECT_COMMAND),
            stdin = file.readText(StandardCharsets.UTF_8),
        )
        check(result.exitCode == 0) { result.stderr.ifBlank { "Credential store failed" } }
        return result.stdout.trim().takeIf { it.isNotBlank() }
    }

    private fun saveWindows(account: String, encoded: String): CommandResult {
        val result = run(
            listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", WINDOWS_PROTECT_COMMAND),
            stdin = encoded,
        )
        if (result.exitCode == 0) {
            val directory = credentialDirectory()
            directory.mkdirs()
            credentialFile(account).writeText(result.stdout.trim(), StandardCharsets.UTF_8)
        }
        return result
    }

    private fun credentialFile(account: String): File {
        return File(credentialDirectory(), "$account.dpapi")
    }

    private fun credentialDirectory(): File {
        return File(File(System.getProperty("user.home"), ".tidetunes"), "credentials")
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
