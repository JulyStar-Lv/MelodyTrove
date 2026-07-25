package com.github.tidetunes.feature.home.domain

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.presentation.components.QualityBadgeType
import kotlinx.serialization.Serializable

@Serializable
enum class PinnedItemType {
    Track,
    Playlist,
    Album,
    Artist,
}

@Serializable
@Immutable
data class PinnedHomeItem(
    val id: String,
    val type: PinnedItemType,
    val referenceId: Long,
    val order: Int,
    val pinnedAtEpochMs: Long,
)

@Immutable
data class HistoryPlayItem(
    val trackId: Long,
    val title: String,
    val artist: String?,
    val durationMs: Long?,
    val mediaId: MediaId?,
    val playedAtEpochMs: Long,
    val artworkIndex: Int,
    val qualityBadge: QualityBadgeType? = null,
)

@Immutable
data class HomeStatistics(
    val totalTracksEverPlayed: Int,
    val totalListeningDurationMs: Long,
    val tracksPlayedToday: Int,
    val mostPlayedTrackIds: List<Long>,
)

