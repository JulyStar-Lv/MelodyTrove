package io.github.julystar.musicapp.feature.playlist.presentation

import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.MediaType
import io.github.julystar.musicapp.core.domain.model.SourceId
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun testArtwork(): Artwork {
    return Artwork.SourceMedia(
        MediaId(
            sourceId = SourceId("webdav"),
            mediaType = MediaType.Image,
            remoteId = "cover-media-id",
        )
    )
}

class CreatePlaylistStateTest {

    @Test
    fun `default state has sensible initial values`() {
        val state = CreatePlaylistState()

        assertFalse(state.isOpen)
        assertEquals(CreatePlaylistTab.Full, state.mode)
        assertEquals("", state.name)
        assertEquals(0, state.musicCount)
        assertEquals(persistentListOf(), state.recommendNames)
        assertEquals(null, state.coverArtwork)
        assertFalse(state.fullImported)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `empty mode canSubmit true when name is non-blank`() {
        val state = CreatePlaylistState(
            mode = CreatePlaylistTab.Empty,
            name = "My Playlist",
            canSubmit = true,
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `empty mode canSubmit false when name is blank`() {
        val state = CreatePlaylistState(
            mode = CreatePlaylistTab.Empty,
            name = "",
            canSubmit = false,
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `full mode canSubmit true when name and musicCount are present`() {
        val state = CreatePlaylistState(
            mode = CreatePlaylistTab.Full,
            name = "Imported",
            musicCount = 3,
            canSubmit = true,
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `full mode canSubmit true when name and cover are present even with no music`() {
        val state = CreatePlaylistState(
            mode = CreatePlaylistTab.Full,
            name = "Cover Only",
            musicCount = 0,
            coverArtwork = testArtwork(),
            canSubmit = true,
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `full mode canSubmit false when only name is present`() {
        val state = CreatePlaylistState(
            mode = CreatePlaylistTab.Full,
            name = "Just Name",
            musicCount = 0,
            coverArtwork = null,
            canSubmit = false,
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `full mode canSubmit false when only cover is present`() {
        val state = CreatePlaylistState(
            mode = CreatePlaylistTab.Full,
            name = "",
            musicCount = 0,
            coverArtwork = testArtwork(),
            canSubmit = false,
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `fullImported flag is preserved in state`() {
        val imported = CreatePlaylistState(fullImported = true)
        val notImported = CreatePlaylistState(fullImported = false)

        assertTrue(imported.fullImported)
        assertFalse(notImported.fullImported)
    }
}

class EditPlaylistStateTest {

    @Test
    fun `default state has sensible initial values`() {
        val state = EditPlaylistState()

        assertFalse(state.isOpen)
        assertEquals("", state.name)
        assertEquals(null, state.coverArtwork)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit true when name is non-blank`() {
        val state = EditPlaylistState(
            name = "Renamed",
            canSubmit = true,
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `canSubmit false when name is blank`() {
        val state = EditPlaylistState(
            name = "",
            canSubmit = false,
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `canSubmit false when name is whitespace only`() {
        val state = EditPlaylistState(
            name = "   ",
            canSubmit = false,
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `coverArtwork field preserves value`() {
        val artwork = testArtwork()
        val state = EditPlaylistState(coverArtwork = artwork)

        assertEquals(artwork, state.coverArtwork)
    }
}
