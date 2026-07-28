package io.github.julystar.musicapp.feature.search.data

import io.github.julystar.musicapp.feature.search.domain.DEFAULT_SEARCH_ALBUM_ARTIST_LIMIT
import io.github.julystar.musicapp.feature.search.domain.DEFAULT_SEARCH_LIMIT
import io.github.julystar.musicapp.feature.search.domain.SearchAggregator
import io.github.julystar.musicapp.feature.search.domain.SearchRepository
import io.github.julystar.musicapp.feature.search.domain.SearchResults
import io.github.julystar.musicapp.feature.search.domain.SearchSourceAccount
import io.github.julystar.musicapp.feature.search.domain.SearchSourceFailure
import io.github.julystar.musicapp.feature.search.domain.SearchSourceFailureReason
import io.github.julystar.musicapp.feature.search.domain.SearchTrackItem
import io.github.julystar.musicapp.source.api.MusicSource
import io.github.julystar.musicapp.source.api.MusicSourceRegistry
import io.github.julystar.musicapp.source.api.SourceCapability
import io.github.julystar.musicapp.source.api.SourceMediaItem
import io.github.julystar.musicapp.source.api.SourceSearchFailureReason
import io.github.julystar.musicapp.source.api.SourceSearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class MusicSourceSearchAggregator(
    private val localRepository: SearchRepository,
    private val sourceRegistry: MusicSourceRegistry,
    private val sourceSearchTimeoutMillis: Long = DEFAULT_SOURCE_SEARCH_TIMEOUT_MILLIS,
) : SearchAggregator {
    override suspend fun suggestSources(
        query: String,
        sourceAccounts: List<SearchSourceAccount>,
        limit: Int,
    ): List<String> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() || limit <= 0) return emptyList()

        val suggestResults = searchSources(
            query = normalizedQuery,
            sourceAccounts = sourceAccounts,
            limit = limit,
        )
        return suggestResults.tracks.map { it.title }
    }

    override suspend fun search(
        query: String,
        sourceAccounts: List<SearchSourceAccount>,
        limit: Int,
    ): SearchResults {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() || limit <= 0) {
            return SearchResults.Empty
        }

        val localResults = localRepository.searchLocalLibrary(normalizedQuery, limit)
        val remoteResults = searchSources(
            query = normalizedQuery,
            sourceAccounts = sourceAccounts,
            limit = limit,
        )
        val tracks = dedupeSearchTracks(localResults.tracks + remoteResults.tracks)
            .take(limit)

        val albums = localRepository.searchLocalAlbums(normalizedQuery, DEFAULT_SEARCH_ALBUM_ARTIST_LIMIT)
        val artists = localRepository.searchLocalArtists(normalizedQuery, DEFAULT_SEARCH_ALBUM_ARTIST_LIMIT)

        return SearchResults(
            tracks = tracks,
            failedSources = localResults.failedSources + remoteResults.failedSources,
            albums = albums,
            artists = artists,
        )
    }

    private suspend fun searchSources(
        query: String,
        sourceAccounts: List<SearchSourceAccount>,
        limit: Int,
    ): SearchResults = coroutineScope {
        sourceAccounts.mapNotNull { account ->
            val source = sourceRegistry.sourceOrNull(account.sourceId) ?: return@mapNotNull null
            if (SourceCapability.Search !in source.capabilities) {
                return@mapNotNull null
            }
            async {
                withTimeoutOrNull(sourceSearchTimeoutMillis.coerceAtLeast(1L)) {
                    source.searchAccount(
                        account = account,
                        query = query,
                        limit = limit,
                    )
                } ?: source.timeoutResult(account)
            }
        }.awaitAll().combine()
    }

    private fun MusicSource.timeoutResult(account: SearchSourceAccount): SearchResults {
        val label = account.displayName?.takeIf { it.isNotBlank() }
            ?: descriptor.displayName
        return SearchResults(
            tracks = emptyList(),
            failedSources = listOf(
                SearchSourceFailure(
                    sourceId = descriptor.id,
                    accountId = account.accountId,
                    sourceLabel = label,
                    reason = SearchSourceFailureReason.Timeout,
                ),
            ),
        )
    }

    private suspend fun MusicSource.searchAccount(
        account: SearchSourceAccount,
        query: String,
        limit: Int,
    ): SearchResults {
        val label = account.displayName?.takeIf { it.isNotBlank() }
            ?: descriptor.displayName
        val result = try {
            search(
                accountId = account.accountId,
                query = query,
                limit = limit,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            SourceSearchResult.Failure(SourceSearchFailureReason.Unknown)
        }

        return when (result) {
            is SourceSearchResult.Success -> SearchResults(
                tracks = result.items.map { item ->
                    item.toSearchTrackItem(sourceLabel = label)
                }
            )
            is SourceSearchResult.Failure -> SearchResults(
                tracks = emptyList(),
                failedSources = listOf(
                    SearchSourceFailure(
                        sourceId = descriptor.id,
                        accountId = account.accountId,
                        sourceLabel = label,
                        reason = result.reason.toSearchFailureReason(),
                    )
                ),
            )
        }
    }
}

private fun SourceMediaItem.toSearchTrackItem(sourceLabel: String): SearchTrackItem {
    return SearchTrackItem(
        title = title,
        artist = artist,
        durationMs = durationMs,
        mediaId = mediaId,
        sourceAccountId = accountId,
        sourceLabel = sourceLabel,
    )
}

private fun List<SearchResults>.combine(): SearchResults {
    return SearchResults(
        tracks = flatMap { it.tracks },
        failedSources = flatMap { it.failedSources },
    )
}

internal fun dedupeSearchTracks(tracks: List<SearchTrackItem>): List<SearchTrackItem> {
    val seen = mutableSetOf<String>()
    return tracks.filter { track ->
        seen.add(track.dedupeKey())
    }
}

private fun SearchTrackItem.dedupeKey(): String {
    val normalizedArtist = artist.orEmpty().normalizeSearchKey()
    val normalizedDuration = durationMs?.let { it / DEDUPE_DURATION_BUCKET_MS }.orEmpty()
    return listOf(
        title.normalizeSearchKey(),
        normalizedArtist,
        normalizedDuration,
    ).joinToString(separator = "|")
}

private fun String.normalizeSearchKey(): String {
    return trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}

private fun SourceSearchFailureReason.toSearchFailureReason(): SearchSourceFailureReason {
    return when (this) {
        SourceSearchFailureReason.Unsupported -> SearchSourceFailureReason.Unsupported
        SourceSearchFailureReason.Unauthorized -> SearchSourceFailureReason.Unauthorized
        SourceSearchFailureReason.Timeout -> SearchSourceFailureReason.Timeout
        SourceSearchFailureReason.UnsupportedAccount -> SearchSourceFailureReason.UnsupportedAccount
        SourceSearchFailureReason.Unavailable -> SearchSourceFailureReason.Unavailable
        SourceSearchFailureReason.Unknown -> SearchSourceFailureReason.Unknown
    }
}

private fun Long?.orEmpty(): String = this?.toString().orEmpty()

private const val DEDUPE_DURATION_BUCKET_MS = 1_000L
internal const val DEFAULT_SOURCE_SEARCH_TIMEOUT_MILLIS = 8_000L
