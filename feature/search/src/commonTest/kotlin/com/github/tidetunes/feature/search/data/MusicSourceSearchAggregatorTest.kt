package com.github.tidetunes.feature.search.data

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.feature.search.domain.SearchAlbumItem
import com.github.tidetunes.feature.search.domain.SearchArtistItem
import com.github.tidetunes.feature.search.domain.SearchRepository
import com.github.tidetunes.feature.search.domain.SearchResults
import com.github.tidetunes.feature.search.domain.SearchSourceAccount
import com.github.tidetunes.feature.search.domain.SearchSourceFailureReason
import com.github.tidetunes.source.api.MusicSource
import com.github.tidetunes.source.api.MusicSourceDescriptor
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.SourceAuthResult
import com.github.tidetunes.source.api.SourceCapability
import com.github.tidetunes.source.api.SourceConfiguration
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourceMediaItem
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.SourceSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicSourceSearchAggregatorTest {
    @Test
    fun `one timed out account does not hide another account result`() = runTest {
        val slowId = SourceId("slow")
        val readyId = SourceId("ready")
        val aggregator = MusicSourceSearchAggregator(
            localRepository = EmptySearchRepository,
            sourceRegistry = MusicSourceRegistry(
                listOf(
                    source(slowId) {
                        delay(1_000)
                        SourceSearchResult.Success(emptyList())
                    },
                    source(readyId) {
                        SourceSearchResult.Success(
                            listOf(
                                SourceMediaItem(
                                    mediaId = MediaId(readyId, MediaType.Track, "track-1"),
                                    accountId = SourceAccountId("ready-account"),
                                    title = "Ready track",
                                ),
                            ),
                        )
                    },
                ),
            ),
            sourceSearchTimeoutMillis = 50,
        )

        val results = aggregator.search(
            query = "track",
            sourceAccounts = listOf(
                SearchSourceAccount(slowId, SourceAccountId("slow-account"), "Slow source"),
                SearchSourceAccount(readyId, SourceAccountId("ready-account"), "Ready source"),
            ),
        )

        assertEquals(listOf("Ready track"), results.tracks.map { it.title })
        assertEquals(1, results.failedSources.size)
        assertEquals("Slow source", results.failedSources.single().sourceLabel)
        assertEquals(SearchSourceFailureReason.Timeout, results.failedSources.single().reason)
    }

    private fun source(
        id: SourceId,
        search: suspend () -> SourceSearchResult,
    ): MusicSource = object : MusicSource {
        override val descriptor = MusicSourceDescriptor(id, id.value)
        override val capabilities = setOf(SourceCapability.Search)

        override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
            return SourceAuthResult.Success
        }

        override suspend fun list(
            accountId: SourceAccountId,
            directoryId: String?,
        ): SourceListResult = SourceListResult.Success(emptyList())

        override suspend fun search(
            accountId: SourceAccountId,
            query: String,
            limit: Int,
        ): SourceSearchResult = search()

        override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)
        }
    }

    private object EmptySearchRepository : SearchRepository {
        override suspend fun searchLocalLibrary(query: String, limit: Int): SearchResults = SearchResults.Empty

        override suspend fun suggestLocalLibrary(query: String, limit: Int): List<String> = emptyList()

        override suspend fun searchLocalAlbums(query: String, limit: Int): List<SearchAlbumItem> = emptyList()

        override suspend fun searchLocalArtists(query: String, limit: Int): List<SearchArtistItem> = emptyList()
    }
}
