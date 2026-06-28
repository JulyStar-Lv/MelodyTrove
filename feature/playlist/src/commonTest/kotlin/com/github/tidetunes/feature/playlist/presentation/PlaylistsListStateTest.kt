package com.github.tidetunes.feature.playlist.presentation

import com.github.tidetunes.core.domain.model.Artwork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class PlaylistsListStateTest {

    @Test
    fun constructsDefaultStateEmpty() {
        val state = PlaylistsListState()
        assertTrue(state.isEmpty)
        assertTrue(state.playlists.isEmpty())
        assertEquals(PlaylistsListMode.Normal, state.mode)
    }

    @Test
    fun constructsNonEmptyStateWithItems() {
        val items = listOf(
            PlaylistListItem(id = 10, title = "A", musicCount = "3", durationLabel = "00:05:00", cover = null),
            PlaylistListItem(id = 20, title = "B", musicCount = "7", durationLabel = "00:12:00", cover = null),
        ).toImmutableList()

        val state = PlaylistsListState(
            playlists = items,
            mode = PlaylistsListMode.Normal,
            isEmpty = false,
        )

        assertEquals(2, state.playlists.size)
        assertEquals(10, state.playlists[0].id)
        assertEquals("A", state.playlists[0].title)
        assertEquals("3", state.playlists[0].musicCount)
        assertEquals("00:05:00", state.playlists[0].durationLabel)
        assertEquals(null, state.playlists[0].cover)
        assertEquals(false, state.isEmpty)
    }

    @Test
    fun playlistListItemHoldsCoverArtwork() {
        val cover: Artwork = Artwork.LibraryCover(trackId = 99)
        val item = PlaylistListItem(
            id = 1,
            title = "With Cover",
            musicCount = "5",
            durationLabel = "00:30:00",
            cover = cover,
        )

        assertEquals(1, item.id)
        val itemCover = assertIs<Artwork.LibraryCover>(item.cover)
        assertEquals(99, itemCover.trackId)
    }

    @Test
    fun playlistListItemCoverCanBeNull() {
        val item = PlaylistListItem(
            id = 2, title = "No Cover", musicCount = "0", durationLabel = "--:--:--", cover = null,
        )
        assertEquals(null, item.cover)
    }

    @Test
    fun adjustModeIsReflectedInState() {
        val state = PlaylistsListState(
            playlists = persistentListOf(),
            mode = PlaylistsListMode.Adjust,
            isEmpty = true,
        )
        assertEquals(PlaylistsListMode.Adjust, state.mode)
    }

    @Test
    fun movePlaylistActionCarriesIndices() {
        val action = PlaylistsListAction.MovePlaylist(fromIndex = 2, toIndex = 5)
        assertEquals(2, action.fromIndex)
        assertEquals(5, action.toIndex)
    }

    @Test
    fun navigateToPlaylistActionCarriesId() {
        val action = PlaylistsListAction.NavigateToPlaylist(42)
        assertEquals(42, action.id)
    }

    @Test
    fun toggleModeAndSetModeNormalAreDistinctActions() {
        val toggle = PlaylistsListAction.ToggleMode
        val setNormal = PlaylistsListAction.SetModeNormal
        assertTrue(toggle != setNormal)
    }
}
