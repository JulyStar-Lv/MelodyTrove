package com.github.tidetune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetune.singleton.ImportRepository
import com.github.tidetune.singleton.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uniffi.tidetune_core.ArgUpdatePlaylist
import uniffi.tidetune_core.StorageEntryType
import uniffi.tidetune_core.PlaylistId
import uniffi.tidetune_core.StorageEntryLoc
import kotlin.collections.firstOrNull
import kotlin.collections.map

class EditPlaylistVM constructor(
    private val importRepository: ImportRepository,
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _id: PlaylistId = PlaylistId(savedStateHandle["id"]!!)
    private val _modalOpen = MutableStateFlow(false)
    private val _name = MutableStateFlow("")
    private val _cover = MutableStateFlow<StorageEntryLoc?>(null)
    val name = _name.asStateFlow()
    val cover = _cover.asStateFlow()
    val modalOpen = _modalOpen.asStateFlow()

    val canSubmit = combine(name, cover) {
            name, cover ->
        name.isNotBlank()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    fun updateName(name: String) {
        _name.value = name
    }

    fun clearCover() {
        _cover.value = null
    }

    fun openModal() {
        _modalOpen.value = true


        val list = playlistRepository.playlists.value
        val item = list.find { v -> v.meta.id == _id }

        if (item != null) {
            _name.value = item.meta.title
            _cover.value = item.meta.cover
        }
    }

    fun closeModal() {
        _modalOpen.value = false

        reset()
    }

    fun reset() {
        _name.value = ""
        _cover.value = null
    }

    fun prepareImportCover() {
        importRepository.prepare(listOf(StorageEntryType.IMAGE)) {
            entries ->
                _cover.value = entries.map { entry -> StorageEntryLoc(
                    storageId = entry.storageId,
                    path = entry.path
                ) }.firstOrNull()
        }
    }

    fun finish() {
        playlistRepository.editPlaylist(ArgUpdatePlaylist(
            id = _id,
            title = _name.value,
            cover = _cover.value,
        ))

        closeModal()
    }
}
