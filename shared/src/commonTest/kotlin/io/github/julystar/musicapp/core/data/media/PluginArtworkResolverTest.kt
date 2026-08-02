package io.github.julystar.musicapp.core.data.media

import io.github.julystar.musicapp.source.api.MetaCoverCandidate
import io.github.julystar.musicapp.source.api.MetaSongCandidate
import io.github.julystar.musicapp.source.api.MetaSongQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginArtworkResolverTest {
    @Test
    fun selectsTheBestMatchingSongCoverInsteadOfTheFirstResult() {
        val query = MetaSongQuery(
            title = "Night Drive",
            artist = "The Waves",
            durationMs = 180_000L,
        )

        val selected = selectPluginSongArtworkUrl(
            query = query,
            candidates = listOf(
                MetaSongCandidate(
                    id = "wrong",
                    title = "Night Drive",
                    artist = "Someone Else",
                    durationMs = 180_000L,
                    pictureUrl = "https://example.test/wrong.jpg",
                ),
                MetaSongCandidate(
                    id = "right",
                    title = "Night Drive",
                    artist = "The Waves",
                    durationMs = 181_000L,
                    pictureUrl = "https://example.test/right.jpg",
                ),
            ),
        )

        assertEquals("https://example.test/right.jpg", selected)
    }

    @Test
    fun selectsTheLargestDedicatedCoverCandidate() {
        val selected = selectPluginCoverArtworkUrl(
            listOf(
                MetaCoverCandidate("https://example.test/small.jpg", width = 300, height = 300),
                MetaCoverCandidate("https://example.test/large.jpg", width = 1_000, height = 1_000),
            ),
        )

        assertEquals("https://example.test/large.jpg", selected)
    }
}
