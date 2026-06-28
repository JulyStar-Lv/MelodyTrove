package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.RouteImport
import com.github.tidetunes.core.data.CreatePlaylistRequest
import com.github.tidetunes.core.domain.repository.PlaylistRepository
import com.github.tidetunes.core.data.PlaylistRepositoryImpl
import com.github.tidetunes.feature.importing.data.RouteImportType
import com.github.tidetunes.source.api.ImportRepository
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

@Composable
fun CreatePlaylistRoot(
    createPlaylistVM: CreatePlaylistVM = run {
        val playlistRepo: PlaylistRepository = koinInject()
        val importRepo: ImportRepository = koinInject()
        remember(playlistRepo, importRepo) {
            CreatePlaylistVM(
                importRepository = importRepo,
                onCreatePlaylistRequest = { title, cover, entries ->
                    (playlistRepo as PlaylistRepositoryImpl).createPlaylist(
                        CreatePlaylistRequest(
                            title = title,
                            cover = cover,
                            entries = entries,
                        )
                    )
                },
            )
        }
    },
) {
    val navController = LocalNavController.current

    val isOpen by createPlaylistVM.modalOpen.collectAsState()
    val mode by createPlaylistVM.mode.collectAsState()
    val name by createPlaylistVM.name.collectAsState()
    val musicCount by createPlaylistVM.musicCount.collectAsState()
    val recommendNames by createPlaylistVM.recommendPlaylistNames.collectAsState()
    val coverArtwork by createPlaylistVM.coverArtwork.collectAsState()
    val fullImported by createPlaylistVM.fullImported.collectAsState()
    val canSubmit by createPlaylistVM.canSubmit.collectAsState()

    val state = CreatePlaylistState(
        isOpen = isOpen,
        mode = mode,
        name = name,
        musicCount = musicCount,
        recommendNames = recommendNames.toImmutableList(),
        coverArtwork = coverArtwork,
        fullImported = fullImported,
        canSubmit = canSubmit,
    )

    CreatePlaylistScreen(
        state = state,
        onAction = { action ->
            when (action) {
                CreatePlaylistAction.Close -> createPlaylistVM.closeModal()
                CreatePlaylistAction.SwitchToFull -> createPlaylistVM.updateMode(CreatePlaylistTab.Full)
                CreatePlaylistAction.SwitchToEmpty -> createPlaylistVM.updateMode(CreatePlaylistTab.Empty)
                is CreatePlaylistAction.UpdateName -> createPlaylistVM.updateName(action.name)
                CreatePlaylistAction.PrepareImport -> {
                    createPlaylistVM.prepareImportCreate()
                    navController.navigate(RouteImport(RouteImportType.EditPlaylist))
                }
                CreatePlaylistAction.NavigateToImport -> {
                    navController.navigate(RouteImport(RouteImportType.EditPlaylistCover))
                }
                CreatePlaylistAction.ClearCover -> createPlaylistVM.clearCover()
                CreatePlaylistAction.Reset -> createPlaylistVM.reset()
                CreatePlaylistAction.Submit -> {
                    createPlaylistVM.finish()
                    createPlaylistVM.closeModal()
                }
            }
        },
    )
}
