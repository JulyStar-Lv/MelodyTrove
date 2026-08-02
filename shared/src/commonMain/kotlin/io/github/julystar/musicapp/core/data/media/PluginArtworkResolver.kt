package io.github.julystar.musicapp.core.data.media

import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.database.MetadataDao
import io.github.julystar.musicapp.database.TrackDao
import io.github.julystar.musicapp.database.TrackEntity
import io.github.julystar.musicapp.platform.fetchRemoteImageBytes
import io.github.julystar.musicapp.platform.getAppCacheDir
import io.github.julystar.musicapp.plugin.management.MetadataLookupUseCase
import io.github.julystar.musicapp.plugin.management.PluginMetaSourceRegistry
import io.github.julystar.musicapp.plugin.management.PluginRepository
import io.github.julystar.musicapp.plugin.management.PluginSummary
import io.github.julystar.musicapp.plugin.runtime.PluginLookupMode
import io.github.julystar.musicapp.source.api.MetaCoverCandidate
import io.github.julystar.musicapp.source.api.MetaSongCandidate
import io.github.julystar.musicapp.source.api.MetaSongQuery
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.ByteString.Companion.encodeUtf8

/** Resolves plugin artwork only after the caller has exhausted persisted file metadata artwork. */
class PluginArtworkResolver(
    private val scope: CoroutineScope,
    private val trackDao: TrackDao,
    private val metadataDao: MetadataDao,
    private val lookup: MetadataLookupUseCase,
    private val pluginRepository: PluginRepository,
    private val pluginRegistry: PluginMetaSourceRegistry,
    private val cacheDirectory: Path = getAppCacheDir().toPath() / CACHE_DIRECTORY_NAME,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val fetchBytes: suspend (String, Long) -> ByteArray? = ::fetchRemoteImageBytes,
    private val timeoutMs: Long = DEFAULT_LOOKUP_TIMEOUT_MS,
) {
    private val stateMutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<ByteArray?>>()
    private val attempted = mutableSetOf<String>()

    suspend fun load(artwork: Artwork): ByteArray? {
        val target = resolveTarget(artwork) ?: return null
        val plugins = automaticArtworkPlugins()
        if (plugins.isEmpty()) return null
        val key = target.cacheKey(plugins)
        val request = stateMutex.withLock {
            if (key in attempted) return null
            inFlight[key]?.let { return@withLock it }
            scope.async {
                withTimeoutOrNull(timeoutMs.coerceAtLeast(1)) {
                    loadOrFetch(target, plugins, key)
                }
            }.also { deferred ->
                inFlight[key] = deferred
                deferred.invokeOnCompletion {
                    scope.launch {
                        stateMutex.withLock {
                            if (inFlight[key] === deferred) inFlight.remove(key)
                            attempted += key
                        }
                    }
                }
            }
        }
        return try {
            request.await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveTarget(artwork: Artwork): PluginArtworkTarget? {
        val track = when (artwork) {
            is Artwork.LibraryTrack -> trackDao.get(artwork.trackId)
            is Artwork.LibraryAlbum -> trackDao.findByAlbumId(artwork.albumId).firstOrNull()
            else -> null
        } ?: return null
        val album = track.albumId?.let { metadataDao.getAlbum(it)?.name }
        val artist = metadataDao.artistNamesForTrack(track.id)
            .joinToString(" / ")
            .ifBlank { track.artist.orEmpty() }
            .takeIf(String::isNotBlank)
        return PluginArtworkTarget(
            identity = when (artwork) {
                is Artwork.LibraryAlbum -> "album-${artwork.albumId}"
                else -> "track-${track.id}"
            },
            track = track,
            query = MetaSongQuery(
                title = track.title,
                artist = artist,
                album = album,
                durationMs = track.durationMs,
                pageSize = RESULTS_PER_SOURCE,
            ),
        )
    }

    private suspend fun automaticArtworkPlugins(): List<PluginSummary> =
        pluginRepository.allSnapshot().filter { plugin ->
            val capabilities = plugin.capabilities.ifEmpty { listOf("searchSongs") }
            plugin.enabled &&
                plugin.allowAutomaticLookup &&
                capabilities.any { it == "searchSongs" || it == "searchCovers" }
        }

    private suspend fun loadOrFetch(
        target: PluginArtworkTarget,
        plugins: List<PluginSummary>,
        key: String,
    ): ByteArray? {
        val cachePath = cacheDirectory / "$key.image"
        readCachedBytes(cachePath)?.takeIf(ByteArray::isSupportedImage)?.let { return it }

        pluginRegistry.refresh()
        val songSourceIds = plugins.sourceIdsFor("searchSongs")
        val coverSourceIds = plugins.sourceIdsFor("searchCovers")
        val songCandidates = if (songSourceIds.isEmpty()) {
            emptyList()
        } else {
            lookup.searchSongs(
                query = target.query,
                mode = PluginLookupMode.AUTOMATIC,
                sourceIds = songSourceIds,
            ).items
        }
        val songUrl = selectPluginSongArtworkUrl(target.query, songCandidates)
        val coverCandidates = if (songUrl != null || coverSourceIds.isEmpty()) {
            emptyList()
        } else {
            lookup.searchCovers(
                query = target.query,
                mode = PluginLookupMode.AUTOMATIC,
                sourceIds = coverSourceIds,
            ).items
        }
        val url = songUrl ?: selectPluginCoverArtworkUrl(coverCandidates) ?: return null
        if (!url.startsWith("https://") && !url.startsWith("http://")) return null
        val bytes = fetchBytes(url, MAX_ARTWORK_BYTES)
            ?.takeIf(ByteArray::isSupportedImage)
            ?: return null
        persist(cachePath, bytes)
        return bytes
    }

    private fun readCachedBytes(path: Path): ByteArray? {
        val metadata = fileSystem.metadataOrNull(path) ?: return null
        if (!metadata.isRegularFile || (metadata.size ?: 0L) > MAX_ARTWORK_BYTES) return null
        return try {
            fileSystem.read(path) { readByteArray() }
        } catch (_: Exception) {
            null
        }
    }

    private fun persist(path: Path, bytes: ByteArray) {
        try {
            fileSystem.createDirectories(cacheDirectory)
            fileSystem.write(path) { write(bytes) }
        } catch (_: Exception) {
            // A valid in-memory result is still useful when the cache directory is unavailable.
        }
    }
}

private data class PluginArtworkTarget(
    val identity: String,
    val track: TrackEntity,
    val query: MetaSongQuery,
) {
    fun cacheKey(plugins: List<PluginSummary>): String = buildString {
        append(identity)
        append('|')
        append(track.updatedAt)
        append('|')
        append(query.title)
        append('|')
        append(query.artist)
        append('|')
        append(query.album)
        plugins.sortedBy(PluginSummary::id).forEach { plugin ->
            append('|')
            append(plugin.id)
            append(':')
            append(plugin.updatedAt)
        }
    }.encodeUtf8().sha256().hex()
}

private fun List<PluginSummary>.sourceIdsFor(capability: String): Set<String> =
    filter { plugin -> capability in plugin.capabilities.ifEmpty { listOf("searchSongs") } }
        .mapTo(mutableSetOf(), PluginSummary::id)

internal fun selectPluginSongArtworkUrl(
    query: MetaSongQuery,
    candidates: List<MetaSongCandidate>,
): String? = candidates
    .mapNotNull { candidate ->
        val url = candidate.pictureUrl?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
        candidate.artworkMatchScore(query)?.let { score -> url to score }
    }
    .maxByOrNull { (_, score) -> score }
    ?.first

internal fun selectPluginCoverArtworkUrl(candidates: List<MetaCoverCandidate>): String? = candidates
    .filter { it.url.isNotBlank() }
    .maxByOrNull { candidate ->
        candidate.width?.toLong()?.times(candidate.height?.toLong() ?: 0L) ?: 0L
    }
    ?.url
    ?.trim()

private fun MetaSongCandidate.artworkMatchScore(query: MetaSongQuery): Int? {
    if (title.matchKey() != query.title.matchKey()) return null
    var score = 100
    val expectedArtist = query.artist?.matchKey().orEmpty()
    val candidateArtist = artist?.matchKey().orEmpty()
    if (expectedArtist.isNotEmpty() && candidateArtist.isNotEmpty()) {
        if (
            expectedArtist != candidateArtist &&
            expectedArtist !in candidateArtist &&
            candidateArtist !in expectedArtist
        ) return null
        score += if (expectedArtist == candidateArtist) 30 else 15
    }
    if (query.album?.matchKey() == album?.matchKey() && query.album?.isNotBlank() == true) score += 10
    val expectedDuration = query.durationMs
    val candidateDuration = durationMs
    if (expectedDuration != null && candidateDuration != null) {
        val difference = abs(expectedDuration - candidateDuration)
        if (difference > MAX_DURATION_DIFFERENCE_MS) return null
        score += when {
            difference <= 2_000 -> 30
            difference <= 5_000 -> 15
            else -> 5
        }
    }
    return score
}

private fun String.matchKey(): String = lowercase().filter(Char::isLetterOrDigit)

private fun ByteArray.isSupportedImage(): Boolean =
    isJpeg() || isPng() || isGif() || isWebP() || isIsoBaseMediaImage()

private fun ByteArray.isJpeg(): Boolean =
    size >= 3 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte() && this[2] == 0xFF.toByte()

private fun ByteArray.isPng(): Boolean =
    size >= 8 && copyOfRange(0, 8).contentEquals(
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
    )

private fun ByteArray.isGif(): Boolean =
    size >= 6 && decodeToString(0, 6).let { it == "GIF87a" || it == "GIF89a" }

private fun ByteArray.isWebP(): Boolean =
    size >= 12 && decodeToString(0, 4) == "RIFF" && decodeToString(8, 12) == "WEBP"

private fun ByteArray.isIsoBaseMediaImage(): Boolean =
    size >= 12 && decodeToString(4, 8) == "ftyp" &&
        decodeToString(8, 12) in setOf("avif", "avis", "heic", "heix", "mif1")

private const val CACHE_DIRECTORY_NAME = "artwork-cache/plugin"
private const val RESULTS_PER_SOURCE = 3
private const val MAX_DURATION_DIFFERENCE_MS = 10_000L
private const val MAX_ARTWORK_BYTES = 16L * 1024 * 1024
private const val DEFAULT_LOOKUP_TIMEOUT_MS = 15_000L
