package io.github.julystar.musicapp.service.playback.domain

import io.github.julystar.musicapp.core.domain.model.MediaId

data class PlayableItem(
    val mediaId: MediaId? = null,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val artworkId: String? = null,
    val libraryTrackId: Long? = null,
    val libraryPlaylistId: Long? = null,
) {
    init {
        require(title.isNotBlank()) { "PlayableItem title cannot be blank" }
        require(mediaId != null || libraryTrackId != null) {
            "PlayableItem must have a mediaId or transitional libraryTrackId"
        }
    }
}

data class PlayerState(
    val currentItem: PlayableItem? = null,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val shuffleEnabled: Boolean = false,
)

enum class PlaybackStatus {
    Idle,
    Loading,
    Playing,
    Paused,
    Error,
}

enum class RepeatMode {
    Off,
    One,
    All,
}

data class PlaybackPosition(
    val positionMs: Long = 0,
    val bufferedMs: Long = 0,
    val durationMs: Long = 0,
    val isSeeking: Boolean = false,
) {
    companion object {
        val Zero = PlaybackPosition()
    }
}

data class PlaybackQueue(
    val items: List<PlayableItem> = emptyList(),
    val currentIndex: Int = -1,
) {
    val currentItem: PlayableItem?
        get() = items.getOrNull(currentIndex)

    fun moveItem(from: Int, to: Int): PlaybackQueue {
        if (from !in items.indices || to !in items.indices || from == to) return this
        val mutableItems = items.toMutableList()
        val item = mutableItems.removeAt(from)
        mutableItems.add(to, item)
        val nextIndex = when (currentIndex) {
            from -> to
            in (from + 1)..to -> currentIndex - 1
            in to until from -> currentIndex + 1
            else -> currentIndex
        }
        return copy(items = mutableItems, currentIndex = nextIndex)
    }

    fun removeItem(index: Int): PlaybackQueue {
        if (index !in items.indices) return this
        val mutableItems = items.toMutableList()
        mutableItems.removeAt(index)
        val nextIndex = when {
            mutableItems.isEmpty() -> -1
            currentIndex == index -> index.coerceAtMost(mutableItems.lastIndex)
            currentIndex > index -> currentIndex - 1
            else -> currentIndex
        }
        return copy(items = mutableItems, currentIndex = nextIndex)
    }

    companion object {
        val Empty = PlaybackQueue()
    }
}
