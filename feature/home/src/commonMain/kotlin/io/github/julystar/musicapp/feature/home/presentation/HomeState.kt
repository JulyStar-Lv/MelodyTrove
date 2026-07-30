package io.github.julystar.musicapp.feature.home.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.presentation.components.QualityBadgeType
import io.github.julystar.musicapp.feature.home.domain.HomeStatistics
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeState(
    val isLoading: Boolean = true,
    val featuredAlbums: ImmutableList<HomeFeaturedAlbum> = persistentListOf(),
    val recentlyAddedAlbums: ImmutableList<HomeFeaturedAlbum> = persistentListOf(),
    val artists: ImmutableList<HomeArtist> = persistentListOf(),
    val pinnedPlaylists: ImmutableList<HomePlaylist> = persistentListOf(),
    val dailyPickTracks: ImmutableList<HomeRecentTrack> = persistentListOf(),
    val recentTracks: ImmutableList<HomeRecentTrack> = persistentListOf(),
    val statistics: HomeStatistics? = null,
)

internal val HomeState.shouldShowEmptyState: Boolean
    get() = !isLoading && dailyPickTracks.isEmpty()

internal val HomeState.shouldShowEmptyStateOnly: Boolean
    get() = shouldShowEmptyState

@Immutable
data class HomeFeaturedAlbum(
    val title: String,
    val subtitle: String,
    val artworkIndex: Int,
    val colors: ImmutableList<Color>,
)

@Immutable
data class HomeArtist(
    val name: String,
    val followers: String,
    val initials: String,
    val artworkIndex: Int,
    val colors: ImmutableList<Color>,
)

@Immutable
data class HomePlaylist(
    val title: String,
    val description: String,
    val meta: String,
    val trackCount: Int = 0,
    val durationMs: Long = 0L,
    val artworkIndex: Int,
    val colors: ImmutableList<Color>,
)

@Immutable
data class HomeRecentTrack(
    val id: Long,
    val mediaId: MediaId?,
    val durationMs: Long?,
    val title: String,
    val subtitle: String,
    val artworkIndex: Int,
    val color: Color,
    val qualityBadge: QualityBadgeType? = null,
    val liked: Boolean = false,
)

sealed interface HomeAction {
    data class PlayTrack(val trackId: Long) : HomeAction
    data class PlayLibraryTrack(val trackId: Long) : HomeAction
    data object PlayDailyPicks : HomeAction
    data object NavigateToDownloads : HomeAction
    data object NavigateToLibrary : HomeAction
    data object NavigateToSourceSettings : HomeAction
    data object NavigateToSearch : HomeAction
    data object NavigateToListening : HomeAction
    data object OpenSleepTimer : HomeAction
}

sealed interface HomeEvent {
    data object NavigateToDownloads : HomeEvent
    data object NavigateToLibrary : HomeEvent
    data object NavigateToSourceSettings : HomeEvent
    data object NavigateToSearch : HomeEvent
    data object NavigateToListening : HomeEvent
    data object OpenSleepTimer : HomeEvent
}
