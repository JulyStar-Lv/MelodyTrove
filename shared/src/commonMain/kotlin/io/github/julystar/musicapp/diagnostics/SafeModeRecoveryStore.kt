package io.github.julystar.musicapp.diagnostics

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_FULLMUTEX
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import io.github.julystar.musicapp.platform.currentTimeMillis
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentDraft
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentSeverity
import io.github.julystar.musicapp.core.domain.model.DiagnosticIncidentType
import io.github.julystar.musicapp.migration.AppIdentifiers
import io.github.julystar.musicapp.platform.getAppCacheDir
import io.github.julystar.musicapp.platform.getAppDatabasePath
import io.github.julystar.musicapp.platform.getAppDataDirectory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

@Serializable
enum class SafeModeRecoveryOption {
    DisableThirdPartyPlugins,
    ClearPlaybackQueue,
    ResetAudio,
    DisableAutomaticScan,
    DisableRemoteSources,
    RebuildLibraryIndex,
    RestoreDefaultSettings,
}

@Serializable
private data class PersistedRecoveryPlan(
    val schemaVersion: Int = 1,
    val options: Set<SafeModeRecoveryOption> = emptySet(),
)

data class DatabaseCheckResult(
    val databaseExists: Boolean,
    val integrityResult: String?,
    val userVersion: Long?,
    val walExists: Boolean,
    val shmExists: Boolean,
)

/**
 * Minimal file-backed recovery state. It is intentionally usable before Koin,
 * Room, the player, plugins, sources, or the application backend exist.
 */
object SafeModeRecoveryStore {
    private val fileSystem = FileSystem.SYSTEM
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val planPath: Path
        get() = "${getAppDataDirectory()}/diagnostics/state/recovery-options.json".toPath()

    fun selectedOptions(): Set<SafeModeRecoveryOption> = runCatching {
        if (!fileSystem.exists(planPath)) return@runCatching emptySet()
        json.decodeFromString<PersistedRecoveryPlan>(
            fileSystem.read(planPath) { readUtf8() },
        ).options
    }.getOrDefault(emptySet())

    fun setOption(option: SafeModeRecoveryOption, enabled: Boolean): Set<SafeModeRecoveryOption> {
        val updated = selectedOptions().toMutableSet().apply {
            if (enabled) add(option) else remove(option)
        }
        persist(updated)
        return updated
    }

    fun disabledComponents(): Set<String> = selectedOptions()
        .mapNotNullTo(mutableSetOf()) { option ->
            when (option) {
                SafeModeRecoveryOption.DisableThirdPartyPlugins -> "third_party_plugins"
                SafeModeRecoveryOption.ClearPlaybackQueue -> "playback_restore"
                SafeModeRecoveryOption.ResetAudio -> "dsp_defaults"
                SafeModeRecoveryOption.DisableAutomaticScan -> "automatic_scan"
                SafeModeRecoveryOption.DisableRemoteSources -> "remote_sources"
                SafeModeRecoveryOption.RebuildLibraryIndex -> "rebuild_library_index"
                SafeModeRecoveryOption.RestoreDefaultSettings -> "settings_defaults"
            }
        }

    fun backupSettings(): String = backupFiles(
        prefix = "settings",
        paths = listOf("${getAppDataDirectory()}/${AppIdentifiers.PREFERENCES_FILE}".toPath()),
    )

    /**
     * The database snapshot contains playlists and favorites without opening a
     * possibly damaged database. It is a recovery backup, not a portable export.
     */
    fun backupPlaylists(): String {
        val database = getAppDatabasePath()?.toPath() ?: error("Database path is unavailable")
        return backupFiles(
            prefix = "library-database",
            paths = listOf(
                database,
                "${database}-wal".toPath(),
                "${database}-shm".toPath(),
            ),
        )
    }

    fun clearCache(): Long {
        val cache = getAppCacheDir().toPath()
        if (!fileSystem.exists(cache)) return 0
        var deleted = 0L
        fileSystem.list(cache).forEach { child ->
            deleted += pathBytes(child)
            fileSystem.deleteRecursively(child, mustExist = false)
        }
        return deleted
    }

    fun checkDatabase(): DatabaseCheckResult {
        return try {
            val path = getAppDatabasePath()?.toPath() ?: error("Database path is unavailable")
            if (!fileSystem.exists(path)) {
                recordDatabaseCheckFailure("The database file does not exist")
                return DatabaseCheckResult(
                    databaseExists = false,
                    integrityResult = null,
                    userVersion = null,
                    walExists = false,
                    shmExists = false,
                )
            }
            val connection = BundledSQLiteDriver().open(
                path.toString(),
                SQLITE_OPEN_READONLY or SQLITE_OPEN_FULLMUTEX,
            )
            connection.use {
                val integrity = it.prepare("PRAGMA integrity_check").use { statement ->
                    if (statement.step()) statement.getText(0) else null
                }
                val version = it.prepare("PRAGMA user_version").use { statement ->
                    if (statement.step()) statement.getLong(0) else null
                }
                DatabaseCheckResult(
                    databaseExists = true,
                    integrityResult = integrity,
                    userVersion = version,
                    walExists = fileSystem.exists("${path}-wal".toPath()),
                    shmExists = fileSystem.exists("${path}-shm".toPath()),
                ).also { result ->
                    if (!result.integrityResult.equals("ok", ignoreCase = true)) {
                        recordDatabaseCheckFailure(
                            "SQLite integrity_check returned ${result.integrityResult ?: "no result"}",
                        )
                    }
                }
            }
        } catch (error: Throwable) {
            recordDatabaseCheckFailure(error.stackTraceToString())
            throw error
        }
    }

    fun databaseCheckSummaryJson(): String {
        val result = runCatching(::checkDatabase)
        return buildJsonObject {
            result.fold(
                onSuccess = { check ->
                    put("databaseExists", check.databaseExists)
                    put("integrityResult", check.integrityResult ?: "unavailable")
                    put("userVersion", check.userVersion ?: -1)
                    put("walExists", check.walExists)
                    put("shmExists", check.shmExists)
                },
                onFailure = {
                    put("databaseExists", false)
                    put("integrityResult", "check failed; see database incident")
                    put("userVersion", -1)
                    put("walExists", false)
                    put("shmExists", false)
                },
            )
        }.toString()
    }

    fun clear() {
        fileSystem.delete(planPath, mustExist = false)
    }

    private fun persist(options: Set<SafeModeRecoveryOption>) {
        fileSystem.createDirectories(checkNotNull(planPath.parent))
        val temporary = "${planPath}.tmp".toPath()
        fileSystem.write(temporary) {
            writeUtf8(json.encodeToString(PersistedRecoveryPlan(options = options)))
            flush()
        }
        fileSystem.atomicMove(temporary, planPath)
    }

    private fun backupFiles(prefix: String, paths: List<Path>): String {
        val available = paths.filter(fileSystem::exists)
        check(available.isNotEmpty()) { "No $prefix data is available to back up" }
        val destination = (
            "${getAppDataDirectory()}/backups/" +
                "${AppIdentifiers.BRAND_NAME}-recovery-$prefix-${currentTimeMillis()}"
            ).toPath()
        fileSystem.createDirectories(destination)
        available.forEach { source ->
            fileSystem.copy(source, destination / source.name)
        }
        return destination.toString()
    }

    private fun pathBytes(path: Path): Long {
        val metadata = fileSystem.metadata(path)
        if (metadata.isRegularFile) return metadata.size ?: 0
        if (!metadata.isDirectory) return 0
        return fileSystem.list(path).sumOf(::pathBytes)
    }

    private fun recordDatabaseCheckFailure(detail: String) {
        runCatching {
            RustDiagnosticsRepository.recordIncident(
                DiagnosticIncidentDraft(
                    type = DiagnosticIncidentType.DatabaseOpenFailure,
                    severity = DiagnosticIncidentSeverity.Error,
                    summary = "Safe-mode database integrity check failed",
                    detail = detail,
                    fingerprintMaterial = detail,
                    requiresRecovery = true,
                )
            )
        }
    }
}
