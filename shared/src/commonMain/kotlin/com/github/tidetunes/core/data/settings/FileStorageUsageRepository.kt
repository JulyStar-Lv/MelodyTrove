package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.data.datastore.APP_DATA_STORE_FILE_NAME
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
        val downloadBytes = sumExisting(
            DOWNLOAD_DIR_NAMES.flatMap { name -> listOf(cacheDir / name, documentDir / name) }
        )
        val databaseBytes = databasePath?.let { sumExisting(databaseRelatedPaths(it)) }
        val logBytes = null
        val totalRoots = mutableListOf(cacheDir, documentDir)
        if (databasePath != null && totalRoots.none { root -> databasePath.isWithin(root) }) {
            totalRoots += databasePath
        }
        val totalBytes = sumExisting(totalRoots.distinct())

        return StorageUsage(
            audioBytes = audioBytes,
            imageBytes = imageBytes,
            downloadBytes = downloadBytes,
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

    override suspend fun clearAllCaches() {
        clearAudioCache()
        clearImageCache()
    }

    override suspend fun clearAllStoredFiles() {
        val cacheDir = getAppCacheDir().toPath()
        val documentDir = getAppDocumentDir().toPath()
        val preservedDocumentPaths = buildSet {
            add(documentDir / APP_DATA_STORE_FILE_NAME)
            getAppDatabasePath()?.toPath()?.let { databasePath ->
                if (databasePath.parent == documentDir) {
                    addAll(databaseRelatedPaths(databasePath))
                }
            }
        }

        deleteChildren(listOf(cacheDir))
        fileSystem.metadataOrNull(documentDir)
            ?.takeIf { it.isDirectory }
            ?.let {
                fileSystem.list(documentDir)
                    .filterNot(preservedDocumentPaths::contains)
                    .forEach { path -> fileSystem.deleteRecursively(path, mustExist = false) }
            }
    }

    override suspend fun enforceCacheLimits(audioLimitBytes: Long, imageLimitBytes: Long) {
        val cacheDir = getAppCacheDir().toPath()
        pruneToLimit(AUDIO_CACHE_DIR_NAMES.map { cacheDir / it }, audioLimitBytes)
        pruneToLimit(IMAGE_CACHE_DIR_NAMES.map { cacheDir / it }, imageLimitBytes)
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

    private fun pruneToLimit(paths: List<Path>, limitBytes: Long) {
        if (limitBytes <= 0L) {
            deleteChildren(paths)
            return
        }
        val files = paths.flatMap { path ->
            val metadata = fileSystem.metadataOrNull(path) ?: return@flatMap emptyList()
            when {
                metadata.isRegularFile -> listOf(path)
                metadata.isDirectory -> fileSystem.listRecursively(path).filter { child ->
                    fileSystem.metadataOrNull(child)?.isRegularFile == true
                }.toList()
                else -> emptyList()
            }
        }.distinct()
        var totalBytes = files.sumOf { path -> fileSystem.metadataOrNull(path)?.size ?: 0L }
        if (totalBytes <= limitBytes) return
        files.sortedBy { path ->
            fileSystem.metadataOrNull(path)?.lastModifiedAtMillis ?: Long.MIN_VALUE
        }.forEach { path ->
            if (totalBytes <= limitBytes) return
            val size = fileSystem.metadataOrNull(path)?.size ?: 0L
            fileSystem.delete(path, mustExist = false)
            totalBytes -= size
        }
    }
}

private fun Path.isWithin(root: Path): Boolean {
    if (this == root) return true
    val rootPrefix = root.toString().trimEnd('/') + "/"
    return toString().startsWith(rootPrefix)
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

private val DOWNLOAD_DIR_NAMES = listOf("downloads")
