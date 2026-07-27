package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.database.TrackSourceRefDao
import com.github.tidetunes.source.storage.toSourceTrackMediaIdOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryRepositoryImpl(
    private val scope: CoroutineScope,
    private val trackDao: TrackDao,
    private val metadataDao: MetadataDao,
    private val trackSourceRefDao: TrackSourceRefDao,
) : LibraryRepository {
    private val _tracks = MutableStateFlow<List<LibraryTrackItem>>(emptyList())
    private val _albums = MutableStateFlow<List<LibraryAlbumItem>>(emptyList())
    private val _artists = MutableStateFlow<List<LibraryArtistItem>>(emptyList())
    private val _initialLoadComplete = MutableStateFlow(false)

    override val initialLoadComplete = _initialLoadComplete.asStateFlow()
    override val tracks = _tracks.asStateFlow()
    override val albums = _albums.asStateFlow()
    override val artists = _artists.asStateFlow()

    init {
        scope.launch {
            trackDao.observeAll().collect { entities ->
                val mediaIds = if (entities.isEmpty()) {
                    emptyMap()
                } else {
                    trackSourceRefDao
                        .playbackCandidatesForTracks(entities.map(TrackEntity::id))
                        .groupBy { candidate -> candidate.ref.trackId }
                        .mapValues { (_, candidates) ->
                            candidates.firstNotNullOfOrNull { candidate -> candidate.toSourceTrackMediaIdOrNull() }
                        }
                }
                _tracks.value = entities.map { track ->
                    track.toLibraryTrackItem(
                        mediaId = mediaIds[track.id],
                    )
                }
                _initialLoadComplete.value = true
            }
        }
        scope.launch {
            metadataDao.observeAlbumsWithTracks().collect { entities ->
                _albums.value = entities.map { LibraryAlbumItem(it.id, it.name, it.year) }
            }
        }
        scope.launch {
            metadataDao.observeArtistsWithTracks().collect { entities ->
                _artists.value = entities.map { LibraryArtistItem(it.id, it.name) }
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
