package com.github.tidetune.singleton

import com.github.tidetune.database.PlaylistDao
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import uniffi.tidetune_core.ArgCreatePlaylist
import uniffi.tidetune_core.ArgRemoveMusicFromPlaylist
import uniffi.tidetune_core.ArgUpdatePlaylist
import uniffi.tidetune_core.PlaylistAbstract
import uniffi.tidetune_core.MusicId
import uniffi.tidetune_core.PlaylistId
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class)
class PlaylistRepository(
    private val storageRepository: StorageRepository,
    private val _scope: CoroutineScope,
    private val playlistDao: PlaylistDao,
    private val roomLibraryStore: RoomLibraryStore,
) {
    private val _playlists = MutableStateFlow(persistentListOf<PlaylistAbstract>())
    private val _syncedTotalDuration = MutableSharedFlow<MusicId>()
    private val _debouncedReloadEvent = MutableSharedFlow<Unit>()
    private val _preRemovePlaylistEvent = MutableSharedFlow<PlaylistId>()
    private val _preRemoveMusicEvent = MutableSharedFlow<ArgRemoveMusicFromPlaylist>()

    val playlists = _playlists.asStateFlow()
    val syncedTotalDuration = _syncedTotalDuration.asSharedFlow()
    val preRemovePlaylistEvent = _preRemovePlaylistEvent.asSharedFlow()
    val preRemoveMusicEvent = _preRemoveMusicEvent.asSharedFlow()
    init {
        _scope.launch {
            playlistDao.observeSummaries().collect { rows ->
                _playlists.value = rows.map(roomLibraryStore::mapPlaylistSummary).toPersistentList()
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

    fun createPlaylist(arg: ArgCreatePlaylist) {
        _scope.launch {
            roomLibraryStore.createPlaylist(arg)
        }
    }

    fun editPlaylist(arg: ArgUpdatePlaylist) {
        _scope.launch {
            roomLibraryStore.updatePlaylist(arg)
        }
    }

    fun removePlaylist(id: PlaylistId) {
        _scope.launch {
            _preRemovePlaylistEvent.emit(id)
            playlistDao.delete(id.value)
        }
    }

    fun requestTotalDuration(added: List<MusicId>) {
        _scope.launch {
            added.forEach { _syncedTotalDuration.emit(it) }
        }
    }

    fun playlistMoveTo(fromIndex: Int, toIndex: Int) {
        val from = _playlists.value.getOrNull(fromIndex) ?: return

        _playlists.value = _playlists.value
            .removeAt(fromIndex)
            .add(toIndex, from)

        _scope.launch {
            roomLibraryStore.replacePlaylistOrder(_playlists.value.map { it.meta.id })
        }
    }


    suspend fun removeMusic(playlistId: PlaylistId, musicId: MusicId) {
        val arg = ArgRemoveMusicFromPlaylist(
            playlistId = playlistId,
            musicId = musicId
        )
        _preRemoveMusicEvent.emit(arg)
        roomLibraryStore.removeMusic(playlistId, musicId)
    }

    fun scheduleReload() {
        _scope.launch {
            _debouncedReloadEvent.emit(Unit)
        }
    }

    suspend fun reload() {
        // Room Flow drives the public state; this method remains for debounced callers.
    }
}
