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
        HomeFeaturedAlbum("Tidal Drift", "Luna Waves", TideTunesGradients.PinkPurple.colors),
        HomeFeaturedAlbum("Voltage Dreams", "Prism Circuit", TideTunesGradients.BluePurple.colors),
        HomeFeaturedAlbum("Open Water", "Coastal Drift", tideGradient(TideTunesBrand.SupportOrange, TideTunesBrand.Primary)),
        HomeFeaturedAlbum("Northern Lights", "Polar Echo", TideTunesGradients.GreenBlue.colors),
        HomeFeaturedAlbum("Subsonic", "Ocean Syntax", tideGradient(TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange)),
        HomeFeaturedAlbum("Glass Architecture", "Fractal Mind", TideTunesGradients.BluePurple.colors),
    ),
    val recentlyAddedAlbums: ImmutableList<HomeFeaturedAlbum> = persistentListOf(
        HomeFeaturedAlbum("Open Water", "Coastal Drift", tideGradient(TideTunesBrand.SupportOrange, TideTunesBrand.Primary)),
        HomeFeaturedAlbum("Northern Lights", "Polar Echo", TideTunesGradients.GreenBlue.colors),
        HomeFeaturedAlbum("Subsonic", "Ocean Syntax", tideGradient(TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange)),
        HomeFeaturedAlbum("Glass Architecture", "Fractal Mind", TideTunesGradients.BluePurple.colors),
        HomeFeaturedAlbum("Quantum", "Wave Function", tideGradient(TideTunesBrand.Primary, TideTunesBrand.SupportOrange)),
        HomeFeaturedAlbum("Between", "Threshold", tideGradient(TideTunesBrand.Secondary, TideTunesBrand.SupportGreen)),
    ),
    val artists: ImmutableList<HomeArtist> = persistentListOf(
        HomeArtist("Luna Waves", "2.4M", "LW", TideTunesGradients.PinkPurple.colors),
        HomeArtist("Prism Circuit", "1.8M", "PC", TideTunesGradients.BluePurple.colors),
        HomeArtist("Coastal Drift", "890K", "CD", tideGradient(TideTunesBrand.SupportOrange, TideTunesBrand.Primary)),
        HomeArtist("Polar Echo", "3.1M", "PE", TideTunesGradients.GreenBlue.colors),
        HomeArtist("Ocean Syntax", "670K", "OS", tideGradient(TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange)),
        HomeArtist("Fractal Mind", "1.2M", "FM", TideTunesGradients.BluePurple.colors),
    ),
    val pinnedPlaylists: ImmutableList<HomePlaylist> = persistentListOf(
        HomePlaylist("Evening Frequencies", "Deep electronic for golden hour", "24 tracks · 1h 32m", TideTunesGradients.PinkPurple.colors),
        HomePlaylist("Spatial Audio Mix", "Hi-Res Dolby Atmos collection", "18 tracks · 1h 08m", TideTunesGradients.BluePurple.colors),
        HomePlaylist("Deep Focus", "Minimal ambient for concentration", "32 tracks · 2h 15m", tideGradient(TideTunesBrand.SupportOrange, TideTunesBrand.Primary)),
        HomePlaylist("Night Drive", "Synthwave for late-night cruising", "20 tracks · 1h 22m", TideTunesGradients.GreenBlue.colors),
        HomePlaylist("Sunrise Protocol", "Gentle morning electronic", "16 tracks · 58m", tideGradient(TideTunesBrand.SupportYellow, TideTunesBrand.SupportOrange)),
        HomePlaylist("System Override", "High-energy techno and industrial", "28 tracks · 1h 45m", TideTunesGradients.BluePurple.colors),
    ),
    val recentTracks: ImmutableList<HomeRecentTrack> = persistentListOf(
        HomeRecentTrack("Midnight Cascade", "Luna Waves · Tidal Drift", TideTunesBrand.Primary, QualityBadgeType.HiRes, liked = true),
        HomeRecentTrack("Neon Undertow", "Prism Circuit · Voltage Dreams", TideTunesBrand.SupportBlue, QualityBadgeType.Flac),
        HomeRecentTrack("Silver Tide", "Coastal Drift · Open Water", TideTunesBrand.SupportOrange, liked = true),
        HomeRecentTrack("Aurora Sequence", "Polar Echo · Northern Lights", TideTunesBrand.SupportGreen, QualityBadgeType.DolbyAtmos),
        HomeRecentTrack("Depth Protocol", "Ocean Syntax · Subsonic", TideTunesBrand.SupportYellow, liked = true),
        HomeRecentTrack("Glass Architecture", "Fractal Mind · Prism", TideTunesBrand.SupportBlue, QualityBadgeType.Flac),
    ),
)

@Immutable
data class HomeFeaturedAlbum(
    val title: String,
    val subtitle: String,
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
    val colors: ImmutableList<Color>,
)

@Immutable
data class HomeRecentTrack(
    val title: String,
    val subtitle: String,
    val color: Color,
    val qualityBadge: QualityBadgeType? = null,
    val liked: Boolean = false,
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

private fun tideGradient(
    start: Color,
    end: Color,
): ImmutableList<Color> = persistentListOf(start, end)
