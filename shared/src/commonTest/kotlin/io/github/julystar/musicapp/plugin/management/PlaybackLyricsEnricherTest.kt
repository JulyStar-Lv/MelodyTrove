package io.github.julystar.musicapp.plugin.management

import io.github.julystar.musicapp.source.api.MetaSongCandidate
import io.github.julystar.musicapp.source.api.MetaSongQuery
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlaybackLyricsEnricherTest {
    @Test
    fun matchesCandidateWithTrailingFeaturedArtist() {
        val candidate = MetaSongCandidate(
            id = "235175539",
            title = "Soon You'll Get Better (feat. Dixie Chicks)",
            artist = "Taylor Swift / The Chicks",
            album = "Lover",
            durationMs = 201_000,
        )

        assertNotNull(
            candidate.matchScore(
                MetaSongQuery(
                    title = "Soon You'll Get Better",
                    artist = "Taylor Swift",
                    album = "Lover",
                    durationMs = 201_589,
                ),
            ),
        )
    }
}
