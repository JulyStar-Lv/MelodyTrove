package io.github.julystar.musicapp.service.playback.domain

data class PlaybackSourceOption(
    val sourceItemId: Long,
    val accountName: String,
    val displayName: String,
    val quality: String?,
    val isSelected: Boolean,
)

interface PlaybackSourceRepository {
    suspend fun sources(trackId: Long): List<PlaybackSourceOption>
    suspend fun select(trackId: Long, sourceItemId: Long): Boolean
}
