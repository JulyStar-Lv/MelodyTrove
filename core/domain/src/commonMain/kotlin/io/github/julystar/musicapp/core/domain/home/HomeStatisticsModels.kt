package io.github.julystar.musicapp.core.domain.home

data class HomeStatistics(
    val totalTracksEverPlayed: Int,
    val totalListeningDurationMs: Long,
    val tracksPlayedToday: Int,
    val mostPlayedTrackIds: List<Long>,
)

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

data class ListeningDistributionBucket(
    val label: String,
    val trackCount: Int,
)

data class ListeningLibraryAnalysis(
    val formatDistribution: List<ListeningDistributionBucket> = emptyList(),
    val qualityDistribution: List<ListeningDistributionBucket> = emptyList(),
)

data class ListeningStatisticsSnapshot(
    val history: List<ListeningHistoryEntry> = emptyList(),
    val tracks: List<ListeningTrackStatistics> = emptyList(),
    val libraryAnalysis: ListeningLibraryAnalysis = ListeningLibraryAnalysis(),
)

data class ListeningPlaybackTrack(
    val trackId: Long,
    val title: String,
    val durationMs: Long?,
)
