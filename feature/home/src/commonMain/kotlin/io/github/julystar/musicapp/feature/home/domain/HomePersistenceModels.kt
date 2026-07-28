package io.github.julystar.musicapp.feature.home.domain

import androidx.compose.runtime.Immutable
import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.presentation.components.QualityBadgeType
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

@Immutable
data class ListeningHistoryEntry(
    val id: Long,
    val trackId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val listenedMs: Long,
    val playedAtEpochMs: Long,
)

@Immutable
data class ListeningTrackStatistics(
    val trackId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val playCount: Int,
    val listenedMs: Long,
    val lastPlayedAtEpochMs: Long,
)

@Immutable
data class ListeningDistributionBucket(
    val label: String,
    val trackCount: Int,
)

@Immutable
data class ListeningLibraryAnalysis(
    val formatDistribution: List<ListeningDistributionBucket> = emptyList(),
    val qualityDistribution: List<ListeningDistributionBucket> = emptyList(),
)

@Immutable
data class ListeningStatisticsSnapshot(
    val history: List<ListeningHistoryEntry> = emptyList(),
    val tracks: List<ListeningTrackStatistics> = emptyList(),
    val libraryAnalysis: ListeningLibraryAnalysis = ListeningLibraryAnalysis(),
)

@Immutable
data class ListeningPlaybackTrack(
    val trackId: Long,
    val title: String,
    val durationMs: Long?,
)
