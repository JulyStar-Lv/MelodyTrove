package com.github.tidetune.singleton

import com.github.tidetune.database.TrackDao
import com.github.tidetune.database.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryTrackItem(
    val id: Long,
    val title: String,
    val artist: String?,
    val durationMs: Long?,
)

class LibraryRepository(
    private val scope: CoroutineScope,
    private val trackDao: TrackDao,
) {
    private val _tracks = MutableStateFlow<List<LibraryTrackItem>>(emptyList())

    val tracks = _tracks.asStateFlow()

    init {
        scope.launch {
            trackDao.observeAll().collect { entities ->
                _tracks.value = entities.map { it.toLibraryTrackItem() }
            }
        }
    }
}

internal fun TrackEntity.toLibraryTrackItem(): LibraryTrackItem {
    return LibraryTrackItem(
        id = id,
        title = title,
        artist = artist?.takeIf { it.isNotBlank() }
            ?: albumArtist?.takeIf { it.isNotBlank() }
            ?: composer?.takeIf { it.isNotBlank() },
        durationMs = durationMs,
    )
}
