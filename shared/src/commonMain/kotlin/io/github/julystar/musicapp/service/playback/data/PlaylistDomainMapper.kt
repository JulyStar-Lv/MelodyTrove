package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import uniffi.app_backend.MusicAbstract
import uniffi.app_backend.MusicId
import uniffi.app_backend.MusicMeta
import uniffi.app_backend.Playlist
import uniffi.app_backend.PlaylistAbstract
import uniffi.app_backend.PlaylistId
import uniffi.app_backend.PlaylistMeta
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Domain-level data used by PlaylistVM to track playlist music entries
 * without depending on UniFFI [MusicAbstract].
 */
data class PlaylistMusicEntry(
    val id: Long,
    val title: String,
    val duration: Duration?,
    val sortOrder: Long,
)

internal fun PlaylistMusicEntry.toMusicAbstract(): MusicAbstract {
    return MusicAbstract(
        meta = MusicMeta(
            id = MusicId(id),
            title = title,
            duration = duration,
            order = listOf(sortOrder.toUInt()),
        ),
        cover = null,
    )
}

internal fun PlaylistSummary.toPlaylistAbstract(): PlaylistAbstract {
    return PlaylistAbstract(
        meta = PlaylistMeta(
            id = PlaylistId(id),
            title = title,
            cover = null,
            showCover = null,
            createdTime = Duration.ZERO,
            order = listOf(0u),
        ),
        musicCount = musicCount.toULong(),
        duration = if (durationMs > 0L) durationMs.milliseconds else null,
    )
}

internal fun buildLegacyPlaylist(summary: PlaylistSummary, entries: List<PlaylistMusicEntry>): Playlist {
    return Playlist(
        abstr = summary.toPlaylistAbstract(),
        musics = entries.map { it.toMusicAbstract() },
    )
}
