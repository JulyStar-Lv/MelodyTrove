package com.github.tidetunes.service.playback.data

import com.github.tidetunes.core.domain.model.PlaylistSummary
import uniffi.tidetunes_backend.MusicAbstract
import uniffi.tidetunes_backend.MusicId
import uniffi.tidetunes_backend.MusicMeta
import uniffi.tidetunes_backend.Playlist
import uniffi.tidetunes_backend.PlaylistAbstract
import uniffi.tidetunes_backend.PlaylistId
import uniffi.tidetunes_backend.PlaylistMeta
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
