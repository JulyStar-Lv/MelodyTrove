package com.github.tidetunes.navigation

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
        assertFalse(shouldShowPersistentMiniPlayer("com.github.tidetunes.MusicGraph.NowPlaying"))
    }

    @Test
    fun `persistent mini player is shown on secondary routes`() {
        listOf(
            "com.github.tidetunes.MusicGraph.Album",
            "com.github.tidetunes.MusicGraph.Artist",
            "com.github.tidetunes.MusicGraph.Playlist",
            "com.github.tidetunes.MusicGraph.Playlists",
            "com.github.tidetunes.MusicGraph.EditStorage",
            "com.github.tidetunes.MusicGraph.Import",
            "com.github.tidetunes.MusicGraph.Downloads",
            "com.github.tidetunes.MusicGraph.Queue",
            "com.github.tidetunes.MusicGraph.Lyrics",
            "com.github.tidetunes.MusicGraph.PluginSettings",
        ).forEach { route ->
            assertTrue(shouldShowPersistentMiniPlayer(route), route)
        }
    }
}
