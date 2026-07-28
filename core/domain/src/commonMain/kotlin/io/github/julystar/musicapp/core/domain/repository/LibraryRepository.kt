package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val initialLoadComplete: StateFlow<Boolean>
    val tracks: StateFlow<List<LibraryTrackItem>>
    val albums: StateFlow<List<LibraryAlbumItem>>
    val artists: StateFlow<List<LibraryArtistItem>>
}
