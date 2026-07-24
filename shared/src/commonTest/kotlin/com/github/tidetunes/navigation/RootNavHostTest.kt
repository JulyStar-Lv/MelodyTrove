package com.github.tidetunes.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RootNavHostTest {

    @Test
    fun `persistent mini player is hidden on root home and now playing`() {
        assertFalse(shouldShowPersistentMiniPlayer("Home"))
        assertFalse(shouldShowPersistentMiniPlayer("com.github.tidetunes.MusicGraph.NowPlaying"))
    }

    @Test
    fun `persistent mini player is shown on secondary routes`() {
        assertTrue(shouldShowPersistentMiniPlayer("com.github.tidetunes.MusicGraph.Album"))
        assertTrue(shouldShowPersistentMiniPlayer("com.github.tidetunes.MusicGraph.Playlist"))
        assertTrue(shouldShowPersistentMiniPlayer("com.github.tidetunes.MusicGraph.Downloads"))
    }
}
