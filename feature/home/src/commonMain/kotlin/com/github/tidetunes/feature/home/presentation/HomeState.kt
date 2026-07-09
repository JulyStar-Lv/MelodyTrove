package com.github.tidetunes.feature.home.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.github.tidetunes.core.presentation.components.QualityBadgeType
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesGradients
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeState(
    val featuredAlbums: ImmutableList<HomeFeaturedAlbum> = persistentListOf(
        HomeFeaturedAlbum("Midnight City", "M83", TideTunesGradients.PinkPurple.colors),
        HomeFeaturedAlbum("Ocean Eyes", "Billie Eilish", TideTunesGradients.GreenBlue.colors),
        HomeFeaturedAlbum("Dream On", "Aerosmith", TideTunesGradients.PinkOrange.colors),
        HomeFeaturedAlbum("Horizon", "Tycho", TideTunesGradients.BlueGreenPurple.colors),
    ),
    val recentTracks: ImmutableList<HomeRecentTrack> = persistentListOf(
        HomeRecentTrack("Midnight City", "M83 · Synthwave", TideTunesBrand.Primary, QualityBadgeType.Flac),
        HomeRecentTrack("Ocean Eyes", "Billie Eilish · Pop", TideTunesBrand.SupportBlue, QualityBadgeType.HiRes),
        HomeRecentTrack("Dream On", "Aerosmith · Rock", TideTunesBrand.SupportOrange, QualityBadgeType.DolbyAtmos),
        HomeRecentTrack("Horizon", "Tycho · Ambient", TideTunesBrand.SupportGreen),
    ),
)

@Immutable
data class HomeFeaturedAlbum(
    val title: String,
    val subtitle: String,
    val colors: ImmutableList<Color>,
)

@Immutable
data class HomeRecentTrack(
    val title: String,
    val subtitle: String,
    val color: Color,
    val qualityBadge: QualityBadgeType? = null,
)

sealed interface HomeAction {
    data object NavigateToDownloads : HomeAction
    data object NavigateToLibrary : HomeAction
    data object NavigateToSearch : HomeAction
    data object OpenSleepTimer : HomeAction
    data object OpenNowPlaying : HomeAction
}

sealed interface HomeEvent {
    data object NavigateToDownloads : HomeEvent
    data object NavigateToLibrary : HomeEvent
    data object NavigateToSearch : HomeEvent
    data object OpenSleepTimer : HomeEvent
    data object OpenNowPlaying : HomeEvent
}
