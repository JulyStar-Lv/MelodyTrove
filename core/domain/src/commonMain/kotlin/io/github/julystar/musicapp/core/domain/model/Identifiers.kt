package io.github.julystar.musicapp.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class SourceId(val value: String) {
    init {
        require(value.isNotBlank()) { "SourceId cannot be blank" }
    }
}

@Serializable
@JvmInline
value class SourceAccountId(val value: String) {
    init {
        require(value.isNotBlank()) { "SourceAccountId cannot be blank" }
    }
}

@Serializable
enum class MediaType {
    Track,
    Album,
    Artist,
    Playlist,
    Folder,
    Image,
}

@Serializable
data class MediaId(
    val sourceId: SourceId,
    val mediaType: MediaType,
    val remoteId: String,
) {
    init {
        require(remoteId.isNotBlank()) { "MediaId remoteId cannot be blank" }
    }
}


@Serializable
@JvmInline
value class LibraryPlaylistId(val value: Long) {
    init {
        require(value > 0) { "LibraryPlaylistId must be positive" }
    }
}

@Serializable
@JvmInline
value class LibraryTrackId(val value: Long) {
    init {
        require(value > 0) { "LibraryTrackId must be positive" }
    }
}
