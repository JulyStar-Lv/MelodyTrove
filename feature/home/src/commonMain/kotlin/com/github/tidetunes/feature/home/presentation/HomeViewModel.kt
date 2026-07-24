package com.github.tidetunes.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.PlaylistSummary
import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.core.domain.repository.PlaylistRepository
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
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
) : ViewModel() {
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)

    val state = combine(
        libraryRepository.tracks,
        libraryRepository.albums,
        libraryRepository.artists,
        playlistRepository.playlistSummaries,
    ) { tracks, albums, artists, playlists ->
        HomeState(
            featuredAlbums = albums.map { it.toHomeAlbum() }.toPersistentList(),
            recentlyAddedAlbums = albums.map { it.toHomeAlbum() }.toPersistentList(),
            artists = artists.map { it.toHomeArtist() }.toPersistentList(),
            pinnedPlaylists = playlists.map { it.toHomePlaylist() }.toPersistentList(),
            recentTracks = tracks.map { it.toHomeTrack() }.toPersistentList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeState(),
    )
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        val event = when (action) {
            is HomeAction.PlayTrack -> return
            HomeAction.NavigateToDownloads -> HomeEvent.NavigateToDownloads
            HomeAction.NavigateToLibrary -> HomeEvent.NavigateToLibrary
            HomeAction.NavigateToSearch -> HomeEvent.NavigateToSearch
            HomeAction.OpenSleepTimer -> HomeEvent.OpenSleepTimer
        }
        _events.trySend(event)
    }
}

private fun LibraryTrackItem.toHomeTrack(): HomeRecentTrack = HomeRecentTrack(
    id = id,
    mediaId = mediaId,
    durationMs = durationMs,
    title = title,
    subtitle = artist.orEmpty(),
    artworkIndex = indexFor(id),
    color = homeGradient(id).first(),
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
        TideTunesBrand.Primary,
        TideTunesBrand.Secondary,
        TideTunesBrand.SupportBlue,
        TideTunesBrand.SupportGreen,
        TideTunesBrand.SupportOrange,
        TideTunesBrand.SupportYellow,
    )
    val startIndex = ((id % colors.size + colors.size) % colors.size).toInt()
    return persistentListOf(colors[startIndex], colors[(startIndex + 1) % colors.size])
}
