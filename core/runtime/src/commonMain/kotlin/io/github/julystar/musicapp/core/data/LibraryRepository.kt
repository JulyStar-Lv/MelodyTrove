package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import io.github.julystar.musicapp.database.MetadataDao
import io.github.julystar.musicapp.database.TrackDao
import io.github.julystar.musicapp.database.TrackEntity
import io.github.julystar.musicapp.database.TrackSourceRefDao
import io.github.julystar.musicapp.source.storage.toSourceTrackMediaIdOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private val _loadError = MutableStateFlow<String?>(null)

    override val initialLoadComplete = _initialLoadComplete.asStateFlow()
    override val loadError = _loadError.asStateFlow()
    override val tracks = _tracks.asStateFlow()
    override val albums = _albums.asStateFlow()
    override val artists = _artists.asStateFlow()

    init {
        scope.launch {
            trackDao.observeAll()
                .combine(metadataDao.observeAlbumsWithTracks()) { entities, albumRows -> entities to albumRows }
                .catch { error -> recordLoadError("tracks", error) }
                .collect { (entities, albumRows) ->
                val albumNames = albumRows.associate { it.album.id to it.album.name }
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
                        albumName = track.albumId?.let(albumNames::get),
                    )
                }
                _initialLoadComplete.value = true
            }
        }
        scope.launch {
            metadataDao.observeAlbumsWithTracks().catch { error -> recordLoadError("albums", error) }.collect { rows ->
                _albums.value = rows.map { row ->
                    LibraryAlbumItem(
                        id = row.album.id,
                        name = row.album.name,
                        year = row.album.year,
                        artist = row.artistName,
                    )
                }
            }
        }
        scope.launch {
            metadataDao.observeArtistsWithTracks().catch { error -> recordLoadError("artists", error) }.collect { entities ->
                _artists.value = entities.map { LibraryArtistItem(it.id, it.name) }
            }
        }
    }

    private fun recordLoadError(source: String, error: Throwable) {
        _loadError.value = "$source: ${error.message ?: error::class.simpleName ?: "unknown error"}"
        _initialLoadComplete.value = true
    }
}

internal fun TrackEntity.toLibraryTrackItem(
    mediaId: MediaId? = null,
    albumName: String? = null,
): LibraryTrackItem {
    return LibraryTrackItem(
        id = id,
        title = title,
        artist = artist?.takeIf { it.isNotBlank() }
            ?: albumArtist?.takeIf { it.isNotBlank() }
            ?: composer?.takeIf { it.isNotBlank() },
        durationMs = durationMs,
        mediaId = mediaId,
        albumName = albumName,
        albumId = albumId,
        codec = codec,
        sampleRateHz = sampleRate,
        bitDepth = bitsPerSample,
        createdAt = createdAt,
        lastPlayedAt = lastPlayedAt,
    )
}
