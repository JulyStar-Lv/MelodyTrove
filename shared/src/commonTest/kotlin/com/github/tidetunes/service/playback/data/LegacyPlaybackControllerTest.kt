package com.github.tidetunes.service.playback.data

import com.github.tidetunes.service.playback.domain.PlaybackStatus
import com.github.tidetunes.service.playback.domain.RepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.tidetunes_core.Music
import uniffi.tidetunes_core.MusicAbstract
import uniffi.tidetunes_core.MusicId
import uniffi.tidetunes_core.MusicMeta
import uniffi.tidetunes_core.PlayMode
import uniffi.tidetunes_core.Playlist
import uniffi.tidetunes_core.PlaylistAbstract
import uniffi.tidetunes_core.PlaylistId
import uniffi.tidetunes_core.PlaylistMeta
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.StorageId
import kotlin.time.Duration.Companion.milliseconds

class LegacyPlaybackControllerTest {
    @Test
    fun mapsLegacyStateToSeparatedPlayerState() {
        val state = legacyPlayerState(
            music = music(id = 7, title = "Moon"),
            playing = true,
            loading = false,
            playMode = PlayMode.LIST_LOOP,
            playlistId = 3,
        )

        assertEquals(PlaybackStatus.Playing, state.status)
        assertEquals(RepeatMode.All, state.repeatMode)
        assertEquals(false, state.shuffleEnabled)
        assertEquals(7L, state.currentItem?.libraryTrackId)
        assertEquals(3L, state.currentItem?.libraryPlaylistId)
        assertEquals("Moon", state.currentItem?.title)
        assertEquals(180_000L, state.currentItem?.durationMs)
    }

    @Test
    fun loadingStateWinsOverPlayingFlag() {
        val state = legacyPlayerState(
            music = music(id = 7, title = "Moon"),
            playing = true,
            loading = true,
            playMode = PlayMode.SINGLE_LOOP,
            playlistId = 3,
        )

        assertEquals(PlaybackStatus.Loading, state.status)
        assertEquals(RepeatMode.One, state.repeatMode)
    }

    @Test
    fun mapsLegacyPlaylistToPlaybackQueue() {
        val queue = legacyPlaybackQueue(
            playlist = playlist(
                id = 3,
                musics = listOf(
                    musicAbstract(id = 1, title = "One"),
                    musicAbstract(id = 2, title = "Two"),
                    musicAbstract(id = 3, title = "Three"),
                ),
            ),
            currentMusic = music(id = 2, title = "Two"),
        )

        assertEquals(listOf(1L, 2L, 3L), queue.items.map { it.libraryTrackId })
        assertEquals(listOf(3L, 3L, 3L), queue.items.map { it.libraryPlaylistId })
        assertEquals(1, queue.currentIndex)
        assertEquals("Two", queue.currentItem?.title)
    }

    private fun music(
        id: Long,
        title: String,
    ): Music {
        return Music(
            meta = musicMeta(id = id, title = title),
            loc = StorageEntryLoc(
                storageId = StorageId(1),
                path = "/Music/$title.flac",
            ),
            cover = null,
            lyric = null,
        )
    }

    private fun musicAbstract(
        id: Long,
        title: String,
    ): MusicAbstract {
        return MusicAbstract(
            meta = musicMeta(id = id, title = title),
            cover = null,
        )
    }

    private fun musicMeta(
        id: Long,
        title: String,
    ): MusicMeta {
        return MusicMeta(
            id = MusicId(id),
            title = title,
            duration = 180_000.milliseconds,
            order = listOf(id.toUInt()),
        )
    }

    private fun playlist(
        id: Long,
        musics: List<MusicAbstract>,
    ): Playlist {
        return Playlist(
            abstr = PlaylistAbstract(
                meta = PlaylistMeta(
                    id = PlaylistId(id),
                    title = "Playlist",
                    cover = null,
                    showCover = null,
                    createdTime = 0.milliseconds,
                    order = listOf(id.toUInt()),
                ),
                musicCount = musics.size.toULong(),
                duration = null,
            ),
            musics = musics,
        )
    }
}
