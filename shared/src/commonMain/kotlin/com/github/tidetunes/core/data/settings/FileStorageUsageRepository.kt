package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.domain.model.StorageUsage
import com.github.tidetunes.core.domain.repository.StorageUsageRepository
import com.github.tidetunes.platform.getAppCacheDir
import com.github.tidetunes.platform.getAppDatabasePath
import com.github.tidetunes.platform.getAppDocumentDir
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileStorageUsageRepository(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : StorageUsageRepository {

    override suspend fun loadUsage(): StorageUsage {
        val cacheDir = getAppCacheDir().toPath()
        val documentDir = getAppDocumentDir().toPath()
        val databasePath = getAppDatabasePath()?.toPath()

        val audioBytes = sumExisting(AUDIO_CACHE_DIR_NAMES.map { cacheDir / it })
        val imageBytes = sumExisting(IMAGE_CACHE_DIR_NAMES.map { cacheDir / it })
        val databaseBytes = databasePath?.let { sumExisting(databaseRelatedPaths(it)) }
        val logBytes = null
        val totalBytes = sumExisting(
            listOfNotNull(cacheDir, documentDir, databasePath).distinct()
        )

        return StorageUsage(
            audioBytes = audioBytes,
            imageBytes = imageBytes,
            databaseBytes = databaseBytes,
            logBytes = logBytes,
            totalBytes = totalBytes,
        )
    }

    override suspend fun clearAudioCache() {
        deleteChildren(AUDIO_CACHE_DIR_NAMES.map { getAppCacheDir().toPath() / it })
    }

    override suspend fun clearImageCache() {
        deleteChildren(IMAGE_CACHE_DIR_NAMES.map { getAppCacheDir().toPath() / it })
    }

    private fun sumExisting(paths: List<Path>): Long {
        return paths.sumOf { path -> sizeOf(path) }
    }

    private fun sizeOf(path: Path): Long {
        val metadata = fileSystem.metadataOrNull(path) ?: return 0L
        if (metadata.isRegularFile) {
            return metadata.size ?: 0L
        }
        if (!metadata.isDirectory) {
            return 0L
        }
        return fileSystem.listRecursively(path).sumOf { child ->
            val childMetadata = fileSystem.metadataOrNull(child)
            if (childMetadata?.isRegularFile == true) {
                childMetadata.size ?: 0L
            } else {
                0L
            }
        }
    }

    private fun deleteChildren(paths: List<Path>) {
        paths.forEach { path ->
            val metadata = fileSystem.metadataOrNull(path) ?: return@forEach
            if (metadata.isDirectory) {
                fileSystem.list(path).forEach { child ->
                    fileSystem.deleteRecursively(child, mustExist = false)
                }
            } else if (metadata.isRegularFile) {
                fileSystem.delete(path, mustExist = false)
            }
        }
    }
}

private fun databaseRelatedPaths(databasePath: Path): List<Path> {
    return listOf(
        databasePath,
        "$databasePath-wal".toPath(),
        "$databasePath-shm".toPath(),
    )
}

private val AUDIO_CACHE_DIR_NAMES = listOf(
    "audio",
    "audio-cache",
    "playback",
    "playback-cache",
    "streams",
)

private val IMAGE_CACHE_DIR_NAMES = listOf(
    "images",
    "image-cache",
    "artwork",
    "artwork-cache",
    "covers",
    "thumbnails",
)
