package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.Artwork
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistArtworkTest {
    @Test
    fun usesFirstTrackArtworkWhenPlaylistHasNoCustomCover() {
        assertEquals(
            Artwork.LibraryTrack(trackId = 42),
            resolvePlaylistCoverArtwork(explicitArtwork = null, firstTrackId = 42),
        )
    }

    @Test
    fun preservesExplicitPlaylistCover() {
        val explicit = Artwork.LegacyStorageEntry(
            storageId = 7,
            path = "/covers/custom.jpg",
        )

        assertEquals(
            explicit,
            resolvePlaylistCoverArtwork(explicitArtwork = explicit, firstTrackId = 42),
        )
    }
}
