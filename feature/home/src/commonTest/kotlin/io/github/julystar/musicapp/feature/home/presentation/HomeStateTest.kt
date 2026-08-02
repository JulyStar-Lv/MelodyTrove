package io.github.julystar.musicapp.feature.home.presentation

import androidx.compose.ui.graphics.Color
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import kotlinx.collections.immutable.persistentListOf
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
        assertFalse(state.shouldShowEmptyStateOnly)
    }

    @Test
    fun `loaded empty state shows the empty library prompt`() {
        val state = HomeState(isLoading = false)

        assertTrue(state.shouldShowEmptyState)
        assertTrue(state.shouldShowEmptyStateOnly)
    }

    @Test
    fun `configured empty library only shows the empty library state`() {
        val state = HomeState(
            isLoading = false,
        )

        assertTrue(state.shouldShowEmptyState)
        assertTrue(state.shouldShowEmptyStateOnly)
    }

    @Test
    fun `empty indexed library hides persisted home sections`() {
        val state = HomeState(
            isLoading = false,
            pinnedPlaylists = persistentListOf(
                HomePlaylist(
                    title = "Saved playlist",
                    description = "From a previous library",
                    meta = "0 tracks",
                    artworkIndex = 1,
                    colors = persistentListOf(Color.Black),
                ),
            ),
        )

        assertTrue(state.shouldShowEmptyState)
        assertTrue(state.shouldShowEmptyStateOnly)
    }

    @Test
    fun `play track action identifies the selected library track`() {
        val action = HomeAction.PlayTrack(trackId = 42L)

        assertEquals(42L, action.trackId)
    }

    @Test
    fun `statistics can be attached to state`() {
        val stats = io.github.julystar.musicapp.feature.home.domain.HomeStatistics(
            totalTracksEverPlayed = 10,
            totalListeningDurationMs = 3600_000L,
            tracksPlayedToday = 3,
            mostPlayedTrackIds = listOf(1L, 2L, 3L),
        )
        val state = HomeState(isLoading = false, statistics = stats)

        assertNotNull(state.statistics)
        assertTrue(state.shouldShowEmptyState)
        assertTrue(state.shouldShowEmptyStateOnly)
        assertEquals(10, state.statistics.totalTracksEverPlayed)
        assertEquals(3600_000L, state.statistics.totalListeningDurationMs)
        assertEquals(3, state.statistics.tracksPlayedToday)
        assertEquals(listOf(1L, 2L, 3L), state.statistics.mostPlayedTrackIds)
    }

    @Test
    fun `home tracks request metadata and plugin artwork lookup`() {
        val item = LibraryTrackItem(
            id = 42L,
            title = "Real track",
            artist = "Artist",
            durationMs = 180_000L,
        ).toHomeTrack(liked = false)

        assertEquals(
            Artwork.LibraryTrack(trackId = 42L, allowPluginLookup = true),
            item.artwork,
        )
    }

    @Test
    fun `home albums resolve artwork by album id`() {
        val item = LibraryAlbumItem(
            id = 7L,
            name = "Real album",
            year = 2026,
        ).toHomeAlbum()

        assertEquals(Artwork.LibraryAlbum(albumId = 7L), item.artwork)
    }
}
