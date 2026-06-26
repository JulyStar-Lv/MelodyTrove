package com.github.tidetune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetune.database.PlaylistDao
import com.github.tidetune.singleton.ImportRepository
import com.github.tidetune.singleton.PlayerController
import com.github.tidetune.singleton.PlaylistRepository
import com.github.tidetune.singleton.RoomLibraryStore
import com.github.tidetune.singleton.StorageRepository
import com.github.tidetune.utils.formatDuration
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import uniffi.tidetune_core.MusicAbstract
import uniffi.tidetune_core.MusicId
import uniffi.tidetune_core.MusicMeta
import uniffi.tidetune_core.Playlist
import uniffi.tidetune_core.PlaylistAbstract
import uniffi.tidetune_core.PlaylistId
import uniffi.tidetune_core.PlaylistMeta
import uniffi.tidetune_core.StorageEntryType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private fun defaultPlaylistAbstract(): PlaylistAbstract {
    return PlaylistAbstract(
        meta = PlaylistMeta(
            id = PlaylistId(0),
            title = "",
            cover = null,
            showCover = null,
            createdTime = Duration.ZERO,
            order = listOf(0u)
        ),
        musicCount = 0uL,
        duration = null
    )
}

class PlaylistVM constructor(
    private val playlistRepository: PlaylistRepository,
    private val storageRepository: StorageRepository,
    private val importRepository: ImportRepository,
    private val playerControllerRepository: PlayerController,
    private val playlistDao: PlaylistDao,
    private val roomLibraryStore: RoomLibraryStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _id: PlaylistId = PlaylistId(savedStateHandle["id"]!!)
    private val _removeModalOpen = MutableStateFlow(false)
    private val _playlistAbstr = MutableStateFlow(defaultPlaylistAbstract())
    private val _playlistMusics = MutableStateFlow(persistentListOf<MusicAbstract>())
    val removeModalOpen = _removeModalOpen.asStateFlow()
    val playlistAbstr = _playlistAbstr.asStateFlow()
    val playlistMusics = _playlistMusics.asStateFlow()

    init {
        viewModelScope.launch {
            reload()
        }
        viewModelScope.launch {
            playlistRepository.playlists.collect { playlists ->
                _playlistAbstr.value = playlists.find { it.meta.id == _id } ?: defaultPlaylistAbstract()
            }
        }
        viewModelScope.launch {
            playlistDao.observeTracks(_id.value).collect { rows ->
                _playlistMusics.value = rows.map { row ->
                    MusicAbstract(
                        meta = MusicMeta(
                            id = MusicId(row.trackId),
                            title = row.title,
                            duration = row.durationMs?.milliseconds,
                            order = listOf(row.sortOrder.toUInt()),
                        ),
                        cover = null,
                    )
                }.toPersistentList()
                playerControllerRepository.refreshPlaylistIfMatch(
                    Playlist(
                        abstr = _playlistAbstr.value,
                        musics = _playlistMusics.value,
                    )
                )
            }
        }
        viewModelScope.launch {
            playlistRepository.syncedTotalDuration.debounce(500.milliseconds).collect {
                reload()
            }
        }
        viewModelScope.launch {
            storageRepository.onRemoveStorageEvent.collect {
                reload()
            }
        }
    }

    fun remove() {
        playlistRepository.removePlaylist(_id)
    }

    fun removeMusic(id: MusicId) {
        viewModelScope.launch {
            playlistRepository.removeMusic(_id, id)
        }
    }

    fun prepareImportMusics() {
        importRepository.prepare(listOf(StorageEntryType.MUSIC)) {
            entries ->
                viewModelScope.launch {
                    val added = roomLibraryStore.addMusicEntries(_id, entries)
                    playlistRepository.requestTotalDuration(added)
                    reload()
                    playlistRepository.reload()
                }
        }
    }

    fun musicMoveTo(fromIndex: Int, toIndex: Int) {
        val from = _playlistMusics.value.getOrNull(fromIndex) ?: return

        _playlistMusics.value = _playlistMusics.value
            .removeAt(fromIndex)
            .add(toIndex, from)

        viewModelScope.launch {
            roomLibraryStore.replaceMusicOrder(
                _playlistAbstr.value.meta.id,
                _playlistMusics.value.map { it.meta.id },
            )
            reload()
        }
    }

    fun openRemoveModal() {
        _removeModalOpen.value = true
    }

    fun closeRemoveModal() {
        _removeModalOpen.value = false
    }

    private suspend fun reload() {
        val playlist = roomLibraryStore.getPlaylist(_id)
        if (playlist != null) {
            playerControllerRepository.refreshPlaylistIfMatch(playlist)
        }
    }
}

fun PlaylistAbstract.durationStr(): String {
    return formatDuration(duration)
}

fun MusicAbstract.durationStr(): String {
    return formatDuration(meta.duration)
}
