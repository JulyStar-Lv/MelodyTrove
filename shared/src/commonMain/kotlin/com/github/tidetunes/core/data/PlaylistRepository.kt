package com.github.tidetunes.core.data

import com.github.tidetunes.database.PlaylistDao
import com.github.tidetunes.database.PlaylistTrackRow as DaoPlaylistTrackRow
import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import com.github.tidetunes.core.domain.model.PlaylistSummary
import com.github.tidetunes.core.toArtwork
import com.github.tidetunes.source.api.SourceNodeSelection
import com.github.tidetunes.core.domain.repository.PlaylistRepository
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.storage.legacyStorageTrackMediaIdOrNull
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import uniffi.tidetunes_core.ArgRemoveMusicFromPlaylist
import uniffi.tidetunes_core.PlaylistAbstract
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.PlaylistId
import kotlin.time.Duration.Companion.milliseconds

data class CreatePlaylistRequest(
    val title: String,
    val cover: SourceNodeSelection?,
    val entries: List<SourceNodeSelection>,
)

data class UpdatePlaylistRequest(
    val id: Long,
    val title: String,
    val cover: SourceNodeSelection?,
)

@OptIn(FlowPreview::class)
class PlaylistRepositoryImpl(
    private val storageRepository: StorageRepositoryImpl,
    private val _scope: CoroutineScope,
    private val playlistDao: PlaylistDao,
    private val roomLibraryStore: RoomLibraryStore,
    private val storageLookup: LegacyStorageLookup,
) : PlaylistRepository {
    private val _playlists = MutableStateFlow(persistentListOf<PlaylistAbstract>())
    private val _syncedTotalDuration = MutableSharedFlow<MusicId>()
    private val _debouncedReloadEvent = MutableSharedFlow<Unit>()
    private val _preRemovePlaylistEvent = MutableSharedFlow<PlaylistId>()
    private val _preRemoveMusicEvent = MutableSharedFlow<ArgRemoveMusicFromPlaylist>()

    val playlists = _playlists.asStateFlow()
    private val _playlistSummaries = MutableStateFlow<List<PlaylistSummary>>(emptyList())
    override val playlistSummaries = _playlistSummaries.asStateFlow()
    val syncedTotalDuration = _syncedTotalDuration.asSharedFlow()
    override val playlistRefreshEvents: Flow<Unit> = merge(
        syncedTotalDuration.debounce(500.milliseconds).map { Unit },
        storageRepository.onRemoveStorageEvent,
    )
    val preRemovePlaylistEvent = _preRemovePlaylistEvent.asSharedFlow()
    val preRemoveMusicEvent = _preRemoveMusicEvent.asSharedFlow()

    init {
        _scope.launch {
            playlistDao.observeSummaries().collect { rows ->
                val mapped = rows.map(roomLibraryStore::mapPlaylistSummary).toPersistentList()
                _playlists.value = mapped
                _playlistSummaries.value = mapped.map { it.toPlaylistSummary() }
            }
        }
        _scope.launch {
            _debouncedReloadEvent.debounce(500.milliseconds).collect {
                reload()
            }
        }
        _scope.launch {
            storageRepository.onRemoveStorageEvent.collect {
                reload()
            }
        }
    }

    override fun observePlaylistTracks(playlistId: Long): Flow<List<DomainPlaylistTrack>> {
        return playlistDao.observeTracks(playlistId).map { rows ->
            rows.map { it.toDomainRow(storageLookup) }
        }
    }

    fun createPlaylist(request: CreatePlaylistRequest) {
        _scope.launch {
            roomLibraryStore.createPlaylist(request)
        }
    }

    fun editPlaylist(request: UpdatePlaylistRequest) {
        _scope.launch {
            roomLibraryStore.updatePlaylist(request)
        }
    }

    override fun removePlaylist(id: Long) {
        _scope.launch {
            _preRemovePlaylistEvent.emit(PlaylistId(id))
            playlistDao.delete(id)
        }
    }

    fun requestTotalDuration(added: List<MusicId>) {
        _scope.launch {
            added.forEach { _syncedTotalDuration.emit(it) }
        }
    }

    override fun requestTotalDurationById(addedTrackIds: List<Long>) {
        requestTotalDuration(addedTrackIds.map { MusicId(it) })
    }

    override fun playlistMoveTo(fromIndex: Int, toIndex: Int) {
        val from = _playlists.value.getOrNull(fromIndex) ?: return

        _playlists.value = _playlists.value
            .removingAt(fromIndex)
            .addingAt(toIndex, from)

        _scope.launch {
            roomLibraryStore.replacePlaylistOrder(_playlists.value.map { it.meta.id })
        }
    }

    override suspend fun removeMusic(playlistId: Long, musicId: Long) {
        val arg = ArgRemoveMusicFromPlaylist(
            playlistId = PlaylistId(playlistId),
            musicId = MusicId(musicId)
        )
        _preRemoveMusicEvent.emit(arg)
        roomLibraryStore.removeMusic(PlaylistId(playlistId), MusicId(musicId))
    }

    override suspend fun replaceMusicOrderById(
        playlistId: Long,
        orderedTrackIds: List<Long>,
    ) {
        roomLibraryStore.replaceMusicOrderById(playlistId, orderedTrackIds)
    }

    override fun scheduleReload() {
        _scope.launch {
            _debouncedReloadEvent.emit(Unit)
        }
    }

    companion object {
        internal fun PlaylistAbstract.toPlaylistSummary(): PlaylistSummary {
            return PlaylistSummary(
                id = meta.id.value,
                title = meta.title,
                musicCount = musicCount.toLong(),
                durationMs = duration?.inWholeMilliseconds ?: 0L,
                coverArtwork = meta.showCover?.toArtwork(),
            )
        }
    }

    suspend fun reload() {
    }
}

internal suspend fun DaoPlaylistTrackRow.toDomainRow(
    storageLookup: LegacyStorageLookup,
): DomainPlaylistTrack {
    return DomainPlaylistTrack(
        trackId = trackId,
        title = title,
        durationMs = durationMs,
        sortOrder = sortOrder,
        sourceStorageId = sourceAccountId,
        sourcePath = sourcePath,
        mediaId = legacyStorageTrackMediaIdOrNull(
            storageLookup = storageLookup,
            sourceStorageId = sourceAccountId,
            sourcePath = sourcePath,
        ),
    )
}
