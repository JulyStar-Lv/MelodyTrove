package com.github.tidetunes.feature.home.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeState(
    val featuredAlbums: ImmutableList<HomeFeaturedAlbum> = persistentListOf(
        HomeFeaturedAlbum("Midnight City", "M83", persistentListOf(Color(0xFF151C36), Color(0xFFFF2D78))),
        HomeFeaturedAlbum("Ocean Eyes", "Billie Eilish", persistentListOf(Color(0xFF102A43), Color(0xFF38BDF8))),
        HomeFeaturedAlbum("Dream On", "Aerosmith", persistentListOf(Color(0xFF3B1D1D), Color(0xFFF97316))),
        HomeFeaturedAlbum("Horizon", "Tycho", persistentListOf(Color(0xFF172554), Color(0xFF22C55E))),
    ),
    val recentTracks: ImmutableList<HomeRecentTrack> = persistentListOf(
        HomeRecentTrack("Midnight City", "M83 · Synthwave", Color(0xFFFF2D78)),
        HomeRecentTrack("Ocean Eyes", "Billie Eilish · Pop", Color(0xFF38BDF8)),
        HomeRecentTrack("Dream On", "Aerosmith · Rock", Color(0xFFF97316)),
        HomeRecentTrack("Horizon", "Tycho · Ambient", Color(0xFF22C55E)),
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
