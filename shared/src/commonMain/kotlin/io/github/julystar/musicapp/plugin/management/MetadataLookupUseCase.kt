package io.github.julystar.musicapp.plugin.management

import io.github.julystar.musicapp.plugin.runtime.LyricoJsMetaSource
import io.github.julystar.musicapp.plugin.runtime.PluginLookupMode
import io.github.julystar.musicapp.source.api.MetaCoverCandidate
import io.github.julystar.musicapp.source.api.MetaLyrics
import io.github.julystar.musicapp.source.api.MetaSongCandidate
import io.github.julystar.musicapp.source.api.MetaSongQuery
import io.github.julystar.musicapp.source.api.MetaSource
import io.github.julystar.musicapp.source.api.MetaSourceRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

enum class MetadataLookupOperation {
    SEARCH_SONGS,
    GET_LYRICS,
    SEARCH_COVERS,
}

data class MetadataLookupFailure(
    val sourceId: String,
    val operation: MetadataLookupOperation,
    val message: String,
    val errorType: String,
)

data class MetadataLookupCollection<T>(
    val items: List<T>,
    val failures: List<MetadataLookupFailure> = emptyList(),
)

data class MetadataLookupValue<T>(
    val value: T?,
    val failures: List<MetadataLookupFailure> = emptyList(),
)

/**
 * Production metadata lookup entry point. Each source failure is isolated so automatic and
 * batch scans can continue with remaining sources and preserve existing local metadata.
 */
class MetadataLookupUseCase(
    private val registry: MetaSourceRegistry,
    private val pluginRepository: PluginRepository,
    private val manualOperationTimeoutMs: Long = 30_000,
) {
    suspend fun searchSongs(
        query: MetaSongQuery,
        mode: PluginLookupMode,
        sourceIds: Set<String>? = null,
    ): MetadataLookupCollection<MetaSongCandidate> = withinModeTimeout(mode) {
        val candidates = mutableListOf<MetaSongCandidate>()
        val failures = mutableListOf<MetadataLookupFailure>()
        selectedSources(sourceIds).forEach { source ->
            try {
                val sourceCandidates = when (source) {
                    is LyricoJsMetaSource -> source.searchSongs(query, mode)
                    else -> source.searchSongs(query)
                }
                candidates += sourceCandidates.take(query.pageSize.coerceAtLeast(1)).map { candidate ->
                    candidate.copy(sourceId = source.id)
                }
                clearPluginError(source)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                recordPluginError(source, error)
                failures += error.toFailure(source.id, MetadataLookupOperation.SEARCH_SONGS)
            }
        }
        MetadataLookupCollection(candidates, failures)
    }

    suspend fun getLyrics(
        candidate: MetaSongCandidate,
        mode: PluginLookupMode,
        config: Map<String, String> = emptyMap(),
    ): MetadataLookupValue<MetaLyrics> = withinModeTimeout(mode) {
        val sourceId = candidate.sourceId
            ?: return@withinModeTimeout MetadataLookupValue(
                value = null,
                failures = listOf(
                    MetadataLookupFailure(
                        sourceId = "unknown",
                        operation = MetadataLookupOperation.GET_LYRICS,
                        message = "Metadata candidate does not identify its source",
                        errorType = "MissingSourceId",
                    ),
                ),
            )
        val source = registry.sourceOrNull(sourceId)
            ?: return@withinModeTimeout MetadataLookupValue(
                value = null,
                failures = listOf(
                    MetadataLookupFailure(
                        sourceId = sourceId,
                        operation = MetadataLookupOperation.GET_LYRICS,
                        message = "Metadata source is no longer available",
                        errorType = "SourceUnavailable",
                    ),
                ),
            )

        try {
            val lyrics = when (source) {
                is LyricoJsMetaSource -> source.getLyrics(candidate, config, mode)
                else -> source.getLyrics(candidate, config)
            }
            clearPluginError(source)
            MetadataLookupValue(lyrics)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            recordPluginError(source, error)
            MetadataLookupValue(
                value = null,
                failures = listOf(error.toFailure(source.id, MetadataLookupOperation.GET_LYRICS)),
            )
        }
    }

    suspend fun searchCovers(
        query: MetaSongQuery,
        mode: PluginLookupMode,
        sourceIds: Set<String>? = null,
    ): MetadataLookupCollection<MetaCoverCandidate> = withinModeTimeout(mode) {
        val covers = mutableListOf<MetaCoverCandidate>()
        val failures = mutableListOf<MetadataLookupFailure>()
        selectedSources(sourceIds).forEach { source ->
            try {
                val sourceCovers = when (source) {
                    is LyricoJsMetaSource -> source.searchCovers(query, mode)
                    else -> source.searchCovers(query)
                }
                covers += sourceCovers.map { cover ->
                    if (cover.sourceId == null) cover.copy(sourceId = source.id) else cover
                }
                clearPluginError(source)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                recordPluginError(source, error)
                failures += error.toFailure(source.id, MetadataLookupOperation.SEARCH_COVERS)
            }
        }
        MetadataLookupCollection(covers, failures)
    }

    private fun selectedSources(sourceIds: Set<String>?): List<MetaSource> =
        registry.sources.filter { candidate ->
            sourceIds == null || candidate.id in sourceIds
        }

    private suspend fun recordPluginError(
        source: MetaSource,
        error: Throwable,
    ) {
        if (source is LyricoJsMetaSource) {
            pluginRepository.recordError(source.id, error)
        }
    }

    private suspend fun clearPluginError(source: MetaSource) {
        if (source is LyricoJsMetaSource) {
            pluginRepository.clearError(source.id)
        }
    }

    private suspend fun <T> withinModeTimeout(
        mode: PluginLookupMode,
        block: suspend () -> T,
    ): T = if (mode == PluginLookupMode.MANUAL) {
        withTimeout(manualOperationTimeoutMs.coerceAtLeast(1)) { block() }
    } else {
        block()
    }

    private fun Throwable.toFailure(
        sourceId: String,
        operation: MetadataLookupOperation,
    ): MetadataLookupFailure = MetadataLookupFailure(
        sourceId = sourceId,
        operation = operation,
        message = message?.take(2_000).orEmpty().ifBlank { "Plugin metadata lookup failed" },
        errorType = this::class.simpleName ?: "UnknownError",
    )
}
