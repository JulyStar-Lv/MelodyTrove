package io.github.julystar.musicapp.feature.search.domain

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import kotlinx.coroutines.flow.Flow

data class SearchTrackItem(
    val id: Long? = null,
    val title: String,
    val artist: String?,
    val durationMs: Long?,
    val mediaId: MediaId? = null,
    val sourceAccountId: SourceAccountId? = null,
    val sourceLabel: String = LOCAL_LIBRARY_SOURCE_LABEL,
)

data class SearchResults(
    val tracks: List<SearchTrackItem>,
    val failedSources: List<SearchSourceFailure> = emptyList(),
    val albums: List<SearchAlbumItem> = emptyList(),
    val artists: List<SearchArtistItem> = emptyList(),
) {
    companion object {
        val Empty = SearchResults(
            tracks = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
        )
    }
}

data class SearchAlbumItem(
    val id: Long,
    val name: String,
    val artist: String? = null,
)

data class SearchArtistItem(
    val id: Long,
    val name: String,
)

data class SearchSourceAccount(
    val sourceId: SourceId,
    val accountId: SourceAccountId,
    val displayName: String? = null,
)

data class SearchSourceFailure(
    val sourceId: SourceId,
    val accountId: SourceAccountId,
    val sourceLabel: String,
    val reason: SearchSourceFailureReason,
)

enum class SearchSourceFailureReason {
    Unsupported,
    Unauthorized,
    Timeout,
    UnsupportedAccount,
    Unavailable,
    Unknown,
}

interface SearchRepository {
    suspend fun searchLocalLibrary(
        query: String,
        limit: Int = DEFAULT_SEARCH_LIMIT,
    ): SearchResults

    suspend fun suggestLocalLibrary(
        query: String,
        limit: Int = DEFAULT_SEARCH_SUGGESTION_LIMIT,
    ): List<String>

    suspend fun searchLocalAlbums(
        query: String,
        limit: Int = DEFAULT_SEARCH_ALBUM_ARTIST_LIMIT,
    ): List<SearchAlbumItem>

    suspend fun searchLocalArtists(
        query: String,
        limit: Int = DEFAULT_SEARCH_ALBUM_ARTIST_LIMIT,
    ): List<SearchArtistItem>
}

interface SearchAggregator {
    suspend fun suggestSources(
        query: String,
        sourceAccounts: List<SearchSourceAccount> = emptyList(),
        limit: Int = DEFAULT_SEARCH_SUGGESTION_LIMIT,
    ): List<String>

    suspend fun search(
        query: String,
        sourceAccounts: List<SearchSourceAccount> = emptyList(),
        limit: Int = DEFAULT_SEARCH_LIMIT,
    ): SearchResults
}

interface SearchSourceAccountProvider {
    fun sourceAccounts(): List<SearchSourceAccount>
}

interface SearchHistoryRepository {
    val history: Flow<List<String>>

    suspend fun remember(query: String)

    suspend fun clear()
}

class SearchLibraryUseCase(
    private val aggregator: SearchAggregator,
) {
    suspend operator fun invoke(
        query: String,
        sourceAccounts: List<SearchSourceAccount> = emptyList(),
        limit: Int = DEFAULT_SEARCH_LIMIT,
    ): SearchResults {
        return aggregator.search(query, sourceAccounts, limit)
    }
}

class SearchSuggestionsUseCase(
    private val searchRepository: SearchRepository,
    private val aggregator: SearchAggregator? = null,
    private val sourceAccountProvider: SearchSourceAccountProvider? = null,
) {
    suspend operator fun invoke(
        query: String,
        history: List<String>,
        limit: Int = DEFAULT_SEARCH_SUGGESTION_LIMIT,
    ): List<String> {
        if (limit <= 0) return emptyList()
        val normalizedQuery = query.trim()
        val localSuggestions = if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            searchRepository.suggestLocalLibrary(
                query = normalizedQuery,
                limit = limit,
            )
        }
        val sourceSuggestions = if (aggregator != null && sourceAccountProvider != null && normalizedQuery.isNotBlank()) {
            aggregator.suggestSources(
                query = normalizedQuery,
                sourceAccounts = sourceAccountProvider.sourceAccounts(),
                limit = limit,
            )
        } else {
            emptyList()
        }
        return mergeSearchSuggestions(
            query = normalizedQuery,
            history = history,
            localSuggestions = localSuggestions,
            sourceSuggestions = sourceSuggestions,
            limit = limit,
        )
    }
}

internal fun mergeSearchSuggestions(
    query: String,
    history: List<String>,
    localSuggestions: List<String>,
    sourceSuggestions: List<String> = emptyList(),
    limit: Int = DEFAULT_SEARCH_SUGGESTION_LIMIT,
): List<String> {
    if (limit <= 0) return emptyList()
    val normalizedQuery = query.trim()
    val historySuggestions = history
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { suggestion ->
            normalizedQuery.isBlank() || suggestion.contains(normalizedQuery, ignoreCase = true)
        }
    val suggestions = if (normalizedQuery.isBlank()) {
        historySuggestions
    } else {
        historySuggestions + localSuggestions + sourceSuggestions
    }
    return suggestions
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .take(limit)
}

const val DEFAULT_SEARCH_LIMIT = 50
const val DEFAULT_SEARCH_SUGGESTION_LIMIT = 10
const val LOCAL_LIBRARY_SOURCE_LABEL = "Library"
const val MAX_SEARCH_HISTORY_SIZE = 10
const val DEFAULT_SEARCH_ALBUM_ARTIST_LIMIT = 5

