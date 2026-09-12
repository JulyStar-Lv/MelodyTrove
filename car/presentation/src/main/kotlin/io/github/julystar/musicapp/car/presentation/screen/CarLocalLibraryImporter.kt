package io.github.julystar.musicapp.car.presentation.screen

import io.github.julystar.musicapp.service.librarysync.domain.SourceAccountLibrarySyncResult

sealed interface CarLocalLibraryState {
    data object Idle : CarLocalLibraryState
    data object AwaitingPermission : CarLocalLibraryState
    data object Scanning : CarLocalLibraryState
    data class Complete(val result: SourceAccountLibrarySyncResult) : CarLocalLibraryState
    data class Failed(val message: String) : CarLocalLibraryState
}

fun interface CarLocalLibraryImporter {
    suspend fun importMusicFolder(): SourceAccountLibrarySyncResult
}
