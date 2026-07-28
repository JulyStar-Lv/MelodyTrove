package io.github.julystar.musicapp.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RootNavHostTest {

    @Test
    fun `root navigation exposes only the four primary destinations`() {
        assertEquals(
            listOf(HomeTab.HOME, HomeTab.SEARCH, HomeTab.LIBRARY, HomeTab.SETTINGS),
            HomeTab.entries.toList(),
        )
    }

    @Test
    fun `persistent mini player is hidden on root home and now playing`() {
        assertFalse(shouldShowPersistentMiniPlayer("Home"))
        assertFalse(shouldShowPersistentMiniPlayer("io.github.julystar.musicapp.MusicGraph.NowPlaying"))
    }

    @Test
    fun `persistent mini player is shown on secondary routes`() {
        listOf(
            "io.github.julystar.musicapp.MusicGraph.Album",
            "io.github.julystar.musicapp.MusicGraph.Artist",
            "io.github.julystar.musicapp.MusicGraph.Playlist",
            "io.github.julystar.musicapp.MusicGraph.Playlists",
            "io.github.julystar.musicapp.MusicGraph.EditStorage",
            "io.github.julystar.musicapp.MusicGraph.Import",
            "io.github.julystar.musicapp.MusicGraph.Downloads",
            "io.github.julystar.musicapp.MusicGraph.Lyrics",
            "io.github.julystar.musicapp.MusicGraph.PluginSettings",
        ).forEach { route ->
            assertTrue(shouldShowPersistentMiniPlayer(route), route)
        }
    }
}
