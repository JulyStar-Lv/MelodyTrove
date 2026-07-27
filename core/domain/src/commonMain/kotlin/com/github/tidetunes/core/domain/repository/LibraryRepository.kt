package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val initialLoadComplete: StateFlow<Boolean>
    val tracks: StateFlow<List<LibraryTrackItem>>
    val albums: StateFlow<List<LibraryAlbumItem>>
    val artists: StateFlow<List<LibraryArtistItem>>
}
