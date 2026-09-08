package io.github.julystar.musicapp.car.presentation.screen

import io.github.julystar.musicapp.core.domain.model.LIBRARY_PLAYBACK_PLAYLIST_ID
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.service.playback.domain.PlayableItem

internal data class CarPlaybackRequest(
    val items: List<PlayableItem>,
    val startIndex: Int,
)

/** Preserves the complete visible queue and the selected semantic item position. */
internal fun List<LibraryTrackItem>.toCarPlaybackRequest(
    startIndex: Int,
    playlistId: Long? = LIBRARY_PLAYBACK_PLAYLIST_ID,
): CarPlaybackRequest {
    require(startIndex in indices) { "startIndex must select an item in the complete queue" }
    return CarPlaybackRequest(
        items = map { track ->
            PlayableItem(
                mediaId = track.mediaId,
                title = track.title,
                artist = track.artist,
                durationMs = track.durationMs,
                libraryTrackId = track.id,
                libraryPlaylistId = playlistId,
            )
        },
        startIndex = startIndex,
    )
}
