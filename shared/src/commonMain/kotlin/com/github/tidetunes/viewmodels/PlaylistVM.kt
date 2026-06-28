package com.github.tidetunes.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.core.domain.model.PlaylistSummary
import com.github.tidetunes.feature.playlist.presentation.PlaylistAction
import com.github.tidetunes.feature.playlist.presentation.PlaylistEvent
import com.github.tidetunes.feature.playlist.presentation.PlaylistState
import com.github.tidetunes.feature.playlist.presentation.PlaylistTrackItem
import com.github.tidetunes.feature.playlist.presentation.toPlaylistHeaderState
import com.github.tidetunes.feature.importing.data.ImportRepositoryImpl
import com.github.tidetunes.feature.playlist.presentation.toPlaylistTrackItem
import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import com.github.tidetunes.service.download.domain.DownloadRequest
import com.github.tidetunes.service.download.domain.EnqueueDownloadUseCase
import com.github.tidetunes.service.playback.data.PlaylistMusicEntry
import com.github.tidetunes.service.playback.data.PlayerController
import com.github.tidetunes.service.playback.data.buildLegacyPlaylist
import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.source.api.SourceNodeType
import com.github.tidetunes.source.storage.LegacyStorageLookup
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PlaylistVM constructor(
    private val playlistRepository: PlaylistRepositoryImpl,
    private val storageRepository: StorageRepositoryImpl,
    private val importRepository: ImportRepositoryImpl,
    private val playerControllerRepository: PlayerController,
    private val roomLibraryStore: RoomLibraryStore,
    private val storageLookup: LegacyStorageLookup,
    private val enqueueDownload: EnqueueDownloadUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _id: Long = savedStateHandle["id"]!!

    private val _removeModalOpen = MutableStateFlow(false)
    private val _playlistSummary = MutableStateFlow<PlaylistSummary?>(null)
    private val _playlistEntries = MutableStateFlow(persistentListOf<PlaylistMusicEntry>())
    private val _state = MutableStateFlow(PlaylistState())
    private val _events = Channel<PlaylistEvent>(Channel.BUFFERED)

    val removeModalOpen = _removeModalOpen.asStateFlow()
    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            playlistRepository.playlistSummaries.collect { summaries ->
                val summary = summaries.find { it.id == _id }
                _playlistSummary.value = summary
                if (summary != null) {
                    _state.value = summary.toPlaylistHeaderState(_state.value)
                    syncToPlayer()
                }
            }
        }
        viewModelScope.launch {
            playlistRepository.observePlaylistTracks(_id).collect { rows ->
                _playlistEntries.value = rows.map { row ->
                    PlaylistMusicEntry(
                        id = row.trackId,
                        title = row.title,
                        duration = row.durationMs?.milliseconds,
                        sortOrder = row.sortOrder,
                    )
                }.toPersistentList()
                _state.value = PlaylistState(
                    tracks = rows.map { row ->
                        row.toPlaylistTrackItem(storageLookup)
                    }.toPersistentList()
                )
                syncToPlayer()
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

    fun onAction(action: PlaylistAction) {
        when (action) {
            PlaylistAction.NavigateBack -> Unit
            PlaylistAction.EditPlaylist -> Unit
            PlaylistAction.PlayAll -> Unit
            is PlaylistAction.PlayTrack -> Unit
            PlaylistAction.ImportTracks -> prepareImportMusics()
            PlaylistAction.OpenRemoveDialog -> openRemoveModal()
            PlaylistAction.CloseRemoveDialog -> closeRemoveModal()
            PlaylistAction.ConfirmRemovePlaylist -> {
                closeRemoveModal()
                remove()
            }
            is PlaylistAction.DownloadTrack -> downloadTrack(action.track)
            is PlaylistAction.RemoveTrack -> removeMusic(action.trackId)
            is PlaylistAction.MoveTrack -> musicMoveTo(action.fromIndex, action.toIndex)
        }
    }

    fun remove() {
        playlistRepository.removePlaylist(_id)
    }

    fun removeMusic(trackId: Long) {
        viewModelScope.launch {
            playlistRepository.removeMusic(_id, trackId)
        }
    }

    fun prepareImportMusics() {
        importRepository.prepare(listOf(SourceNodeType.Track)) { entries ->
            viewModelScope.launch {
                val added = roomLibraryStore.addMusicSelectionsById(_id, entries)
                playlistRepository.requestTotalDurationById(added)
                reload()
                playlistRepository.reload()
            }
        }
    }

    fun musicMoveTo(fromIndex: Int, toIndex: Int) {
        val from = _playlistEntries.value.getOrNull(fromIndex) ?: return

        _playlistEntries.value = _playlistEntries.value
            .removeAt(fromIndex)
            .add(toIndex, from)

        viewModelScope.launch {
            roomLibraryStore.replaceMusicOrderById(
                _id,
                _playlistEntries.value.map { it.id },
            )
            reload()
        }
    }

    fun openRemoveModal() {
        _removeModalOpen.value = true
        _state.value = _state.value.copy(isRemoveDialogOpen = true)
    }

    fun closeRemoveModal() {
        _removeModalOpen.value = false
        _state.value = _state.value.copy(isRemoveDialogOpen = false)
    }

    private suspend fun reload() {
        val playlist = roomLibraryStore.getPlaylistById(_id)
        if (playlist != null) {
            playerControllerRepository.refreshPlaylistIfMatch(playlist)
        }
    }

    private fun syncToPlayer() {
        val summary = _playlistSummary.value ?: return
        val entries = _playlistEntries.value
        playerControllerRepository.refreshPlaylistIfMatch(
            buildLegacyPlaylist(summary, entries)
        )
    }

    private fun downloadTrack(track: PlaylistTrackItem) {
        val mediaId = track.mediaId
        if (mediaId == null) {
            viewModelScope.launch {
                _events.send(PlaylistEvent.ShowMessage("This track cannot be downloaded yet."))
            }
            return
        }
        viewModelScope.launch {
            try {
                enqueueDownload(
                    DownloadRequest(
                        mediaId = mediaId,
                        title = track.title,
                        durationMs = track.durationMs,
                    )
                )
                _events.send(PlaylistEvent.ShowMessage("Added to Downloads."))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _events.send(
                    PlaylistEvent.ShowMessage(
                        exception.message?.takeIf { it.isNotBlank() } ?: "Failed to add download.",
                    )
                )
            }
        }
    }
}
