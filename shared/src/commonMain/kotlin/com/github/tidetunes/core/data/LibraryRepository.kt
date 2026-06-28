package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.storage.toLegacyStorageTrackMediaIdOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryRepositoryImpl(
    private val scope: CoroutineScope,
    private val trackDao: TrackDao,
    private val storageLookup: LegacyStorageLookup,
) : LibraryRepository {
    private val _tracks = MutableStateFlow<List<LibraryTrackItem>>(emptyList())

    override val tracks = _tracks.asStateFlow()

    init {
        scope.launch {
            trackDao.observeAll().collect { entities ->
                _tracks.value = entities.map { track ->
                    track.toLibraryTrackItem(
                        mediaId = track.toLegacyStorageTrackMediaIdOrNull(storageLookup),
                    )
                }
            }
        }
    }
}

internal fun TrackEntity.toLibraryTrackItem(
    mediaId: MediaId? = null,
): LibraryTrackItem {
    return LibraryTrackItem(
        id = id,
        title = title,
        artist = artist?.takeIf { it.isNotBlank() }
            ?: albumArtist?.takeIf { it.isNotBlank() }
            ?: composer?.takeIf { it.isNotBlank() },
        durationMs = durationMs,
        mediaId = mediaId,
    )
}
