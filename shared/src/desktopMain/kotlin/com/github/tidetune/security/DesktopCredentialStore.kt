package com.github.tidetune.security

import java.nio.charset.StandardCharsets
import java.util.Base64

actual fun createCredentialStore(): CredentialStore = DesktopCredentialStore()

private class DesktopCredentialStore : CredentialStore {
    private val service = "com.github.tidetune"

    override suspend fun load(storageId: Long): StoredCredential? {
        val account = account(storageId)
        val encoded = when {
            isMac() -> run(listOf("/usr/bin/security", "find-generic-password", "-s", service, "-a", account, "-w"))
                .takeIf { it.exitCode == 0 }?.stdout?.trim()
            isLinux() -> run(listOf("secret-tool", "lookup", "service", service, "account", account))
                .takeIf { it.exitCode == 0 }?.stdout?.trim()
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
                listOf("secret-tool", "store", "--label=TideTune", "service", service, "account", account),
                stdin = encoded,
            )
            else -> error("No supported secure credential provider on ${System.getProperty("os.name")}")
        }
        check(result.exitCode == 0) { result.stderr.ifBlank { "Credential store failed" } }
    }

    override suspend fun delete(storageId: Long) {
        val account = account(storageId)
        when {
            isMac() -> run(listOf("/usr/bin/security", "delete-generic-password", "-s", service, "-a", account))
            isLinux() -> run(listOf("secret-tool", "clear", "service", service, "account", account))
        }
    }

    private fun account(storageId: Long) = "storage-$storageId"
    private fun isMac() = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
    private fun isLinux() = System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
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
