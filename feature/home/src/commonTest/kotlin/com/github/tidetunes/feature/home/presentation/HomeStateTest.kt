package com.github.tidetunes.feature.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeStateTest {

    @Test
    fun `default state contains no demo content`() {
        val state = HomeState()

        assertTrue(state.isLoading)
        assertTrue(state.featuredAlbums.isEmpty())
        assertTrue(state.recentlyAddedAlbums.isEmpty())
        assertTrue(state.artists.isEmpty())
        assertTrue(state.pinnedPlaylists.isEmpty())
        assertTrue(state.dailyPickTracks.isEmpty())
        assertTrue(state.recentTracks.isEmpty())
        assertNull(state.statistics)
        assertFalse(state.shouldShowEmptyState)
    }

    @Test
    fun `loaded empty state shows the empty library prompt`() {
        val state = HomeState(isLoading = false)

        assertTrue(state.shouldShowEmptyState)
    }

    @Test
    fun `play track action identifies the selected library track`() {
        val action = HomeAction.PlayTrack(trackId = 42L)

        assertEquals(42L, action.trackId)
    }

    @Test
    fun `statistics can be attached to state`() {
        val stats = com.github.tidetunes.feature.home.domain.HomeStatistics(
            totalTracksEverPlayed = 10,
            totalListeningDurationMs = 3600_000L,
            tracksPlayedToday = 3,
            mostPlayedTrackIds = listOf(1L, 2L, 3L),
        )
        val state = HomeState(isLoading = false, statistics = stats)

        assertNotNull(state.statistics)
        assertTrue(state.shouldShowEmptyState)
        assertEquals(10, state.statistics.totalTracksEverPlayed)
        assertEquals(3600_000L, state.statistics.totalListeningDurationMs)
        assertEquals(3, state.statistics.tracksPlayedToday)
        assertEquals(listOf(1L, 2L, 3L), state.statistics.mostPlayedTrackIds)
    }
}
