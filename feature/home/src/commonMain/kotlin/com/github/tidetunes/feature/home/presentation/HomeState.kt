package com.github.tidetunes.feature.home.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.presentation.components.QualityBadgeType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeState(
    val featuredAlbums: ImmutableList<HomeFeaturedAlbum> = persistentListOf(),
    val recentlyAddedAlbums: ImmutableList<HomeFeaturedAlbum> = persistentListOf(),
    val artists: ImmutableList<HomeArtist> = persistentListOf(),
    val pinnedPlaylists: ImmutableList<HomePlaylist> = persistentListOf(),
    val recentTracks: ImmutableList<HomeRecentTrack> = persistentListOf(),
)

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
    val colors: ImmutableList<Color>,
)

@Immutable
data class HomePlaylist(
    val title: String,
    val description: String,
    val meta: String,
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
    data object NavigateToDownloads : HomeAction
    data object NavigateToLibrary : HomeAction
    data object NavigateToSearch : HomeAction
    data object OpenSleepTimer : HomeAction
}

sealed interface HomeEvent {
    data object NavigateToDownloads : HomeEvent
    data object NavigateToLibrary : HomeEvent
    data object NavigateToSearch : HomeEvent
    data object OpenSleepTimer : HomeEvent
}
