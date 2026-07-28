package io.github.julystar.musicapp.migration

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

internal object DesktopDataMigration {
    private const val COMPLETE_MARKER = ".legacy-data-migration-v1-complete"
    private const val IN_PROGRESS_MARKER = ".legacy-data-migration-v1-in-progress"
    private val sqliteHeader = "SQLite format 3\u0000".encodeToByteArray()

    fun defaultDataDirectory(): Path {
        val home = Path.of(System.getProperty("user.home"))
        val osName = System.getProperty("os.name").orEmpty()
        val base = when {
            osName.startsWith("Windows", ignoreCase = true) ->
                System.getenv("APPDATA")
                    ?.takeIf(String::isNotBlank)
                    ?.let { Path.of(it) }
                    ?: home.resolve("AppData").resolve("Roaming")

            osName.startsWith("Mac", ignoreCase = true) ->
                home.resolve("Library").resolve("Application Support")

            else ->
                System.getenv("XDG_DATA_HOME")
                    ?.takeIf(String::isNotBlank)
                    ?.let { Path.of(it) }
                    ?: home.resolve(".local").resolve("share")
        }
        return base.resolve(AppIdentifiers.BRAND_NAME)
    }

    fun legacyDataDirectory(): Path =
        Path.of(System.getProperty("user.home")).resolve(LegacyPaths.DESKTOP_DATA_DIRECTORY)

    fun ensureMigrated(
        newDirectory: Path = defaultDataDirectory(),
        legacyDirectory: Path = legacyDataDirectory(),
    ): Path {
        Files.createDirectories(newDirectory)
        val completeMarker = newDirectory.resolve(COMPLETE_MARKER)
        if (completeMarker.exists()) return newDirectory

        val inProgressMarker = newDirectory.resolve(IN_PROGRESS_MARKER)
        if (!inProgressMarker.exists() && isInitialized(newDirectory)) {
            writeMarker(completeMarker, "new-data-already-initialized")
            return newDirectory
        }

        if (!legacyDirectory.isDirectory()) {
            writeMarker(completeMarker, "no-legacy-data")
            return newDirectory
        }

        writeMarker(inProgressMarker, "migration-in-progress")
        LegacyPaths.FILE_MAPPINGS.forEach { (sourceName, targetName) ->
            migratePath(
                source = legacyDirectory.resolve(sourceName),
                target = newDirectory.resolve(targetName),
            )
        }
        LegacyPaths.DATA_DIRECTORIES.forEach { directoryName ->
            migratePath(
                source = legacyDirectory.resolve(directoryName),
                target = newDirectory.resolve(directoryName),
            )
        }

        validateDatabase(newDirectory.resolve(AppIdentifiers.DATABASE_FILE))
        writeMarker(completeMarker, "migration-complete")
        Files.deleteIfExists(inProgressMarker)
        return newDirectory
    }

    private fun isInitialized(directory: Path): Boolean =
        LegacyPaths.FILE_MAPPINGS.any { (_, targetName) -> directory.resolve(targetName).exists() } ||
            LegacyPaths.DATA_DIRECTORIES.any { directory.resolve(it).exists() }

    private fun migratePath(source: Path, target: Path) {
        if (!source.exists() || target.exists()) return
        target.parent?.let(Files::createDirectories)
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            copyAndVerify(source, target)
        } catch (error: IOException) {
            if (source.exists() && !target.exists()) {
                copyAndVerify(source, target)
            } else {
                throw error
            }
        }
    }

    private fun copyAndVerify(source: Path, target: Path) {
        if (source.isDirectory()) {
            Files.walk(source).use { paths ->
                paths.forEach { sourcePath ->
                    val targetPath = target.resolve(source.relativize(sourcePath))
                    if (sourcePath.isDirectory()) {
                        Files.createDirectories(targetPath)
                    } else {
                        targetPath.parent?.let(Files::createDirectories)
                        Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES)
                        verifyFile(sourcePath, targetPath)
                    }
                }
            }
        } else {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES)
            verifyFile(source, target)
        }
    }

    private fun verifyFile(source: Path, target: Path) {
        check(Files.size(source) == Files.size(target)) {
            "Legacy data migration size mismatch for $source"
        }
        check(checksum(source).contentEquals(checksum(target))) {
            "Legacy data migration checksum mismatch for $source"
        }
    }

    private fun checksum(path: Path): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest()
    }

    private fun validateDatabase(path: Path) {
        if (!path.exists() || Files.size(path) == 0L) return
        val header = ByteArray(sqliteHeader.size)
        Files.newInputStream(path).use { input ->
            check(input.read(header) == header.size && header.contentEquals(sqliteHeader)) {
                "Migrated database is not a readable SQLite database: $path"
            }
        }
    }

    private fun writeMarker(path: Path, value: String) {
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(temporary, value)
        try {
            Files.move(
                temporary,
                path,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
