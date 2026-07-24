package com.github.tidetunes.feature.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeStateTest {

    @Test
    fun `default state contains no demo content`() {
        val state = HomeState()

        assertTrue(state.featuredAlbums.isEmpty())
        assertTrue(state.recentlyAddedAlbums.isEmpty())
        assertTrue(state.artists.isEmpty())
        assertTrue(state.pinnedPlaylists.isEmpty())
        assertTrue(state.recentTracks.isEmpty())
    }

    @Test
    fun `play track action identifies the selected library track`() {
        val action = HomeAction.PlayTrack(trackId = 42L)

        assertEquals(42L, action.trackId)
    }
}
