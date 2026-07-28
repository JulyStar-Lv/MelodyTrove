package io.github.julystar.musicapp.feature.playlist.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.toStorageRouteIdOrNull
import io.github.julystar.musicapp.feature.playlist.domain.EditPlaylistGateway
import io.github.julystar.musicapp.source.api.ImportRepository
import io.github.julystar.musicapp.source.api.SourceNodeSelection
import io.github.julystar.musicapp.source.api.SourceNodeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.firstOrNull

class EditPlaylistVM constructor(
    private val importRepository: ImportRepository,
    private val editPlaylistGateway: EditPlaylistGateway,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _id: Long = savedStateHandle["id"]!!
    private val _modalOpen = MutableStateFlow(false)
    private val _name = MutableStateFlow("")
    private val _cover = MutableStateFlow<SourceNodeSelection?>(null)
    val name = _name.asStateFlow()
    val cover = _cover.asStateFlow()
    val coverArtwork = _cover.map { cover ->
        cover?.let { sel ->
            Artwork.LegacyStorageEntry(
                storageId = sel.accountId.toStorageRouteIdOrNull() ?: 0L,
                path = "/" + sel.node.path.trimStart('/'),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val modalOpen = _modalOpen.asStateFlow()

    val canSubmit = combine(name, cover) { name, _ ->
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

        val meta = editPlaylistGateway.getPlaylistMetaToEdit(_id)
        if (meta != null) {
            _name.value = meta.title
            _cover.value = meta.coverSelection
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
        importRepository.prepare(listOf(SourceNodeType.Image)) { entries ->
            _cover.value = entries.firstOrNull { entry -> entry.node.type == SourceNodeType.Image }
        }
    }

    fun finish() {
        editPlaylistGateway.updatePlaylist(_id, _name.value, _cover.value)
        closeModal()
    }
}
