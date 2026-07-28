package io.github.julystar.musicapp.feature.search.data

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.MediaType
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.feature.search.domain.SearchAlbumItem
import io.github.julystar.musicapp.feature.search.domain.SearchArtistItem
import io.github.julystar.musicapp.feature.search.domain.SearchRepository
import io.github.julystar.musicapp.feature.search.domain.SearchResults
import io.github.julystar.musicapp.feature.search.domain.SearchSourceAccount
import io.github.julystar.musicapp.feature.search.domain.SearchSourceFailureReason
import io.github.julystar.musicapp.source.api.MusicSource
import io.github.julystar.musicapp.source.api.MusicSourceDescriptor
import io.github.julystar.musicapp.source.api.MusicSourceRegistry
import io.github.julystar.musicapp.source.api.SourceAuthResult
import io.github.julystar.musicapp.source.api.SourceCapability
import io.github.julystar.musicapp.source.api.SourceConfiguration
import io.github.julystar.musicapp.source.api.SourceListResult
import io.github.julystar.musicapp.source.api.SourceMediaItem
import io.github.julystar.musicapp.source.api.SourcePlaybackFailureReason
import io.github.julystar.musicapp.source.api.SourcePlaybackResult
import io.github.julystar.musicapp.source.api.SourceSearchResult
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
