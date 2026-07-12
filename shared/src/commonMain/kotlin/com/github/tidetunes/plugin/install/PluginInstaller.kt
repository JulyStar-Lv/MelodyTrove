package com.github.tidetunes.plugin.install

import com.github.tidetunes.database.PluginDao
import com.github.tidetunes.database.PluginEntity
import com.github.tidetunes.plugin.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import uniffi.tidetunes_core.extractPluginZip

class PluginInstallError(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ManifestConfigField(
    val key: String,
    val title: String,
    val summary: String? = null,
    val group: String? = null,
    val type: String = "text",
    val required: Boolean = false,
    val defaultValue: String? = null,
    val dependency: JsonObject? = null,
)

data class ParsedManifest(
    val id: String,
    val name: String,
    val versionCode: Long,
    val versionName: String,
    val author: String,
    val description: String,
    val apiVersion: Int,
    val minHostApiVersion: Int,
    val entryFile: String,
    val includeDirs: List<String>,
    val icon: String?,
    val capabilities: List<String>,
    val configFields: List<ManifestConfigField>,
    val raw: String,
)

data class PluginInstallFailure(val root: String, val reason: String)
data class PluginInstallResult(
    val installed: List<ParsedManifest>,
    val failed: List<PluginInstallFailure> = emptyList(),
)

class PluginInstaller(
    private val pluginDao: PluginDao,
    private val pluginsDir: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    companion object {
        const val REQUIRED_API_VERSION = 3
        const val HOST_API_VERSION = 3
        private const val MAX_ARCHIVE_FILES = 512L
        private const val MAX_ARCHIVE_BYTES = 64L * 1024L * 1024L
        private const val MAX_ARCHIVE_DEPTH = 32L
        private const val MAX_PLUGIN_COUNT = 32
    }

    suspend fun installFromZip(zipPath: Path): ParsedManifest {
        val result = installAllFromZip(zipPath)
        if (result.installed.isEmpty()) {
            val reason = result.failed.firstOrNull()?.reason ?: "no installable plugin found"
            throw PluginInstallError(reason)
        }
        return result.installed.first()
    }

    suspend fun installAllFromZip(zipPath: Path): PluginInstallResult = withContext(Dispatchers.Default) {
        val tempDir = pluginsDir / ".tmp-import-${currentTimeMillis()}"
        try {
            fileSystem.createDirectories(tempDir)
            extractPluginZip(
                zipPath.toString(),
                tempDir.toString(),
                MAX_ARCHIVE_FILES.toULong(),
                MAX_ARCHIVE_BYTES.toULong(),
                MAX_ARCHIVE_DEPTH.toULong(),
            )
            val candidates = buildCandidates(tempDir)
            val duplicateIds = candidates
                .mapNotNull { it.getOrNull()?.manifest?.id }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
            val failed = mutableListOf<PluginInstallFailure>()
            val valid = candidates.mapNotNull { result ->
                result.fold(
                    onSuccess = { candidate ->
                        if (candidate.manifest.id in duplicateIds) {
                            failed += PluginInstallFailure(
                                candidate.relativeRoot,
                                "duplicate plugin id in archive: ${candidate.manifest.id}",
                            )
                            null
                        } else {
                            candidate
                        }
                    },
                    onFailure = { error ->
                        failed += PluginInstallFailure(".", error.message ?: error::class.simpleName.orEmpty())
                        null
                    },
                )
            }
            val installed = mutableListOf<ParsedManifest>()
            for (candidate in valid.sortedBy { it.relativeRoot }) {
                try {
                    checkConflicts(candidate.manifest)
                    installCandidate(candidate, valid)
                    installed += candidate.manifest
                } catch (error: Throwable) {
                    failed += PluginInstallFailure(
                        candidate.relativeRoot,
                        error.message ?: error::class.simpleName.orEmpty(),
                    )
                }
            }
            PluginInstallResult(installed = installed, failed = failed)
        } catch (error: PluginInstallError) {
            throw error
        } catch (error: Throwable) {
            throw PluginInstallError("install failed: ${error.message ?: "unknown"}", error)
        } finally {
            if (fileSystem.exists(tempDir)) fileSystem.deleteRecursively(tempDir)
        }
    }

    suspend fun uninstall(pluginId: String) = withContext(Dispatchers.Default) {
        pluginDao.deleteConfigs(pluginId)
        pluginDao.deleteByPluginId(pluginId)
        val dir = pluginsDir / pluginId
        if (fileSystem.exists(dir)) fileSystem.deleteRecursively(dir)
    }

    internal fun readManifest(dir: Path): ParsedManifest {
        val path = dir / "manifest.json"
        require(fileSystem.metadataOrNull(path)?.isRegularFile == true) { "manifest.json not found" }
        val raw = fileSystem.read(path) { readUtf8() }
        val root = json.parseToJsonElement(raw).jsonObject
        val configs = (root["configFields"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { field ->
                ManifestConfigField(
                    key = field["key"]?.jsonPrimitive?.content ?: "",
                    title = field["title"]?.jsonPrimitive?.content ?: "",
                    summary = field["summary"]?.jsonPrimitive?.contentOrNull,
                    group = field["group"]?.jsonPrimitive?.contentOrNull,
                    type = field["type"]?.jsonPrimitive?.content ?: "text",
                    required = field["required"]?.jsonPrimitive?.booleanOrNull == true,
                    defaultValue = field["defaultValue"]?.jsonPrimitive?.contentOrNull,
                    dependency = field["dependency"] as? JsonObject,
                )
            }
            .orEmpty()
        return ParsedManifest(
            id = root.string("id"),
            name = root.string("name"),
            versionCode = root.long("versionCode"),
            versionName = root.string("versionName"),
            author = root.string("author"),
            description = root.string("description"),
            apiVersion = root.int("apiVersion"),
            minHostApiVersion = root.intOrNull("minHostApiVersion") ?: 1,
            entryFile = root.stringOrNull("entry") ?: root.stringOrNull("entryFile") ?: "source.js",
            includeDirs = (root["includeDirs"] as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                .orEmpty(),
            icon = root["icon"]?.jsonPrimitive?.contentOrNull,
            capabilities = (root["capabilities"] as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                .orEmpty(),
            configFields = configs,
            raw = raw,
        )
    }

    internal fun validateManifest(manifest: ParsedManifest) {
        require(manifest.id.matches(Regex("^[a-zA-Z][a-zA-Z0-9._-]*\\.[a-zA-Z][a-zA-Z0-9._-]*$"))) {
            "plugin id must be reverse-domain format"
        }
        require(manifest.apiVersion == REQUIRED_API_VERSION) {
            "unsupported apiVersion: ${manifest.apiVersion}, expected $REQUIRED_API_VERSION"
        }
        require(manifest.minHostApiVersion <= HOST_API_VERSION) {
            "plugin requires host API ${manifest.minHostApiVersion}"
        }
        require(manifest.versionCode >= 1) { "versionCode must be >= 1" }
        require(manifest.name.isNotBlank()) { "plugin name is required" }
    }

    private fun buildCandidates(tempDir: Path): List<Result<PluginCandidate>> {
        val manifests = fileSystem.listRecursively(tempDir)
            .filter { path -> path.name == "manifest.json" && fileSystem.metadata(path).isRegularFile }
            .take(MAX_PLUGIN_COUNT + 1)
            .toList()
        require(manifests.isNotEmpty()) { "manifest.json not found" }
        require(manifests.size <= MAX_PLUGIN_COUNT) { "archive contains too many plugins" }
        return manifests.map { manifestPath ->
            runCatching {
                val root = manifestPath.parent ?: error("manifest has no parent directory")
                val manifest = readManifest(root)
                validateManifest(manifest)
                validatePluginLayout(root, manifest)
                PluginCandidate(
                    root = root,
                    manifest = manifest,
                    relativeRoot = root.relativePathTo(tempDir).ifBlank { "." },
                )
            }
        }
    }

    private fun validatePluginLayout(root: Path, manifest: ParsedManifest) {
        val entry = resolveUnder(root, manifest.entryFile, "entry")
        require(fileSystem.metadataOrNull(entry)?.isRegularFile == true) { "entry file not found: ${manifest.entryFile}" }
        manifest.includeDirs.forEach { includeDir ->
            val dir = resolveUnder(root, includeDir, "includeDir")
            require(fileSystem.metadataOrNull(dir)?.isDirectory == true) { "includeDir not found: $includeDir" }
        }
        manifest.icon?.let { icon ->
            val iconPath = resolveUnder(root, icon, "icon")
            require(fileSystem.metadataOrNull(iconPath)?.isRegularFile == true) { "icon not found: $icon" }
        }
    }

    private suspend fun checkConflicts(manifest: ParsedManifest) {
        val existing = pluginDao.findByPluginId(manifest.id) ?: return
        if (existing.versionCode > manifest.versionCode) {
            throw PluginInstallError("a newer version already installed")
        }
    }

    private suspend fun installCandidate(candidate: PluginCandidate, allCandidates: List<PluginCandidate>) {
        fileSystem.createDirectories(pluginsDir)
        val stagingDir = pluginsDir / ".staging-${candidate.manifest.id}-${currentTimeMillis()}"
        val destDir = pluginsDir / candidate.manifest.id
        try {
            if (fileSystem.exists(stagingDir)) fileSystem.deleteRecursively(stagingDir)
            fileSystem.createDirectories(stagingDir)
            val excludedRoots = allCandidates
                .map { it.root.normalized() }
                .filter { it != candidate.root.normalized() && it.isUnderOrSame(candidate.root) }
            copyPluginRoot(candidate.root, stagingDir, excludedRoots)
            if (fileSystem.exists(destDir)) fileSystem.deleteRecursively(destDir)
            fileSystem.atomicMove(stagingDir, destDir)
            val now = currentTimeMillis()
            pluginDao.upsert(
                PluginEntity(
                    pluginId = candidate.manifest.id,
                    name = candidate.manifest.name,
                    versionCode = candidate.manifest.versionCode,
                    versionName = candidate.manifest.versionName,
                    author = candidate.manifest.author,
                    description = candidate.manifest.description,
                    apiVersion = candidate.manifest.apiVersion,
                    minHostApiVersion = candidate.manifest.minHostApiVersion,
                    entryFile = candidate.manifest.entryFile,
                    includeDirsJson = json.encodeToString(candidate.manifest.includeDirs),
                    iconPath = candidate.manifest.icon?.let { (destDir / it).toString() },
                    capabilitiesJson = json.encodeToString(candidate.manifest.capabilities),
                    manifestRawJson = candidate.manifest.raw,
                    installedAt = now,
                    updatedAt = now,
                    enabled = false,
                ),
            )
        } finally {
            if (fileSystem.exists(stagingDir)) fileSystem.deleteRecursively(stagingDir)
        }
    }

    private fun copyPluginRoot(sourceRoot: Path, targetRoot: Path, excludedRoots: List<Path>) {
        fileSystem.listRecursively(sourceRoot).forEach { source ->
            val normalizedSource = source.normalized()
            if (excludedRoots.any { normalizedSource.isUnderOrSame(it) }) return@forEach
            val relative = normalizedSource.relativePathTo(sourceRoot)
            if (relative.isBlank()) return@forEach
            val target = targetRoot / relative
            val metadata = fileSystem.metadata(source)
            if (metadata.isDirectory) {
                fileSystem.createDirectories(target)
            } else if (metadata.isRegularFile) {
                target.parent?.let { fileSystem.createDirectories(it) }
                fileSystem.read(source) {
                    val fileSource = this
                    fileSystem.write(target) {
                        writeAll(fileSource)
                    }
                }
            }
        }
    }

    private fun resolveUnder(root: Path, relative: String, label: String): Path {
        require(relative.isNotBlank()) { "$label path is blank" }
        val relativePath = relative.toPath(normalize = true)
        require(!relativePath.isAbsolute) { "$label path must be relative: $relative" }
        val resolved = (root / relative).normalized()
        require(resolved.isUnderOrSame(root)) { "$label path escapes plugin root: $relative" }
        return resolved
    }

    private data class PluginCandidate(
        val root: Path,
        val manifest: ParsedManifest,
        val relativeRoot: String,
    )
}

internal fun JsonObject.string(key: String): String =
    this[key]?.jsonPrimitive?.content ?: error("manifest missing: $key")

internal fun JsonObject.stringOrNull(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

internal fun JsonObject.long(key: String): Long =
    this[key]?.jsonPrimitive?.longOrNull ?: error("manifest missing: $key")

internal fun JsonObject.int(key: String): Int =
    this[key]?.jsonPrimitive?.intOrNull ?: error("manifest missing: $key")

internal fun JsonObject.intOrNull(key: String): Int? =
    this[key]?.jsonPrimitive?.intOrNull

private fun Path.isUnderOrSame(root: Path): Boolean {
    val target = normalized().toString().trimEnd('/', '\\')
    val rootText = root.normalized().toString().trimEnd('/', '\\')
    return target == rootText || target.startsWith("$rootText/") || target.startsWith("$rootText\\")
}

private fun Path.relativePathTo(root: Path): String {
    val target = normalized().toString()
    val rootText = root.normalized().toString().trimEnd('/', '\\')
    require(target.trimEnd('/', '\\') == rootText || target.startsWith("$rootText/") || target.startsWith("$rootText\\")) {
        "path is not under root"
    }
    return target.removePrefix(rootText).trimStart('/', '\\')
}
