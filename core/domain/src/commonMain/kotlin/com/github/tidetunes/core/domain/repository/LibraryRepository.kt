package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.LibraryTrackItem
import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val tracks: StateFlow<List<LibraryTrackItem>>
}
