package io.github.julystar.musicapp.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.PlaylistRepository
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import io.github.julystar.musicapp.feature.home.domain.HistoryPlayItem
import io.github.julystar.musicapp.feature.home.domain.HomeHistoryRepository
import io.github.julystar.musicapp.feature.home.domain.HomeStatisticsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    libraryRepository: LibraryRepository,
    playlistRepository: PlaylistRepository,
    historyRepository: HomeHistoryRepository,
    statisticsRepository: HomeStatisticsRepository,
    favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)

    val state = combine(
        combine(
            libraryRepository.tracks,
            libraryRepository.albums,
            libraryRepository.artists,
            playlistRepository.playlistSummaries,
            favoritesRepository.favoriteTrackIds,
        ) { tracks, albums, artists, playlists, favoriteTrackIds ->
            HomeLibraryContent(tracks, albums, artists, playlists, favoriteTrackIds)
        },
        combine(
            historyRepository.recentPlays,
            statisticsRepository.statistics,
        ) { history, stats ->
            Pair(history, stats)
        },
        libraryRepository.initialLoadComplete,
    ) { libraryData, pair, initialLoadComplete ->
        val (tracks, albums, artists, playlists, favoriteTrackIds) = libraryData
        val (historyTracks, stats) = pair
        HomeState(
            isLoading = !initialLoadComplete,
            featuredAlbums = albums.map { it.toHomeAlbum() }.toPersistentList(),
            recentlyAddedAlbums = albums.map { it.toHomeAlbum() }.toPersistentList(),
            artists = artists.map { it.toHomeArtist() }.toPersistentList(),
            pinnedPlaylists = playlists.map { it.toHomePlaylist() }.toPersistentList(),
            dailyPickTracks = tracks.map { it.toHomeTrack(it.id in favoriteTrackIds) }.toPersistentList(),
            recentTracks = historyTracks
                .map { it.toHomeRecentTrack(it.trackId in favoriteTrackIds) }
                .toPersistentList(),
            statistics = stats,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = HomeState(),
    )
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        val event = when (action) {
            is HomeAction.PlayTrack -> return
            is HomeAction.PlayLibraryTrack -> return
            HomeAction.PlayDailyPicks -> return
            HomeAction.NavigateToDownloads -> HomeEvent.NavigateToDownloads
            HomeAction.NavigateToLibrary -> HomeEvent.NavigateToLibrary
            HomeAction.NavigateToSourceSettings -> HomeEvent.NavigateToSourceSettings
            HomeAction.NavigateToSearch -> HomeEvent.NavigateToSearch
            HomeAction.NavigateToListening -> HomeEvent.NavigateToListening
            HomeAction.OpenSleepTimer -> HomeEvent.OpenSleepTimer
        }
        _events.trySend(event)
    }
}

private data class HomeLibraryContent(
    val tracks: List<LibraryTrackItem>,
    val albums: List<LibraryAlbumItem>,
    val artists: List<LibraryArtistItem>,
    val playlists: List<PlaylistSummary>,
    val favoriteTrackIds: Set<Long>,
)

private fun HistoryPlayItem.toHomeRecentTrack(liked: Boolean): HomeRecentTrack = HomeRecentTrack(
    id = trackId,
    mediaId = mediaId,
    durationMs = durationMs,
    title = title,
    subtitle = artist.orEmpty(),
    artworkIndex = artworkIndex,
    color = homeGradient(trackId).first(),
    liked = liked,
)

private fun LibraryTrackItem.toHomeTrack(liked: Boolean): HomeRecentTrack = HomeRecentTrack(
    id = id,
    mediaId = mediaId,
    durationMs = durationMs,
    title = title,
    subtitle = artist.orEmpty(),
    artworkIndex = indexFor(id),
    color = homeGradient(id).first(),
    liked = liked,
)

private fun LibraryAlbumItem.toHomeAlbum(): HomeFeaturedAlbum = HomeFeaturedAlbum(
    title = name,
    subtitle = year?.toString().orEmpty(),
    artworkIndex = indexFor(id),
    colors = homeGradient(id),
)

private fun LibraryArtistItem.toHomeArtist(): HomeArtist = HomeArtist(
    name = name,
    followers = "",
    initials = name
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .joinToString(separator = "") { it.first().uppercase() }
        .ifBlank { "?" },
    artworkIndex = indexFor(id),
    colors = homeGradient(id),
)

private fun PlaylistSummary.toHomePlaylist(): HomePlaylist = HomePlaylist(
    title = title,
    description = "$musicCount tracks",
    meta = durationMs.toHomeDurationLabel(),
    artworkIndex = indexFor(id),
    colors = homeGradient(id),
)

private fun Long.toHomeDurationLabel(): String {
    val totalMinutes = (this / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "$hours h $minutes m" else "$minutes min"
}

private fun indexFor(id: Long): Int = ((id % 8L + 8L) % 8L).toInt() + 1

private fun homeGradient(id: Long): ImmutableList<Color> {
    val colors = listOf(
        DesignPalette.Primary,
        DesignPalette.Secondary,
        DesignPalette.SupportBlue,
        DesignPalette.SupportGreen,
        DesignPalette.SupportOrange,
        DesignPalette.SupportYellow,
    )
    val startIndex = ((id % colors.size + colors.size) % colors.size).toInt()
    return persistentListOf(colors[startIndex], colors[(startIndex + 1) % colors.size])
}
