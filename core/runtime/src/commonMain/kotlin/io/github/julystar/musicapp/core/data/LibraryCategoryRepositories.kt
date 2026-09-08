package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.data.datastore.AppPreferencesRepository
import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.LibrarySortField
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
import io.github.julystar.musicapp.core.domain.model.SortDirection
import io.github.julystar.musicapp.core.domain.repository.DownloadCollectionRepository
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.FolderRepository
import io.github.julystar.musicapp.core.domain.repository.GenreRepository
import io.github.julystar.musicapp.core.domain.repository.HistoryRepository
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import io.github.julystar.musicapp.core.domain.repository.LibraryFolderItem
import io.github.julystar.musicapp.core.domain.repository.LosslessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// ── Genre ──

class StubGenreRepository : GenreRepository {
    override val genreNames: Flow<RepositoryState<List<String>>> =
        MutableStateFlow(RepositoryState.Empty("Genre metadata is unavailable")).asStateFlow()

    override fun genreTracks(
        genreName: String,
        sort: SortCriteria,
        filter: FilterCriteria.GenreFilter,
    ): Flow<RepositoryState<List<DomainTrackBrowserItem>>> =
        MutableStateFlow(RepositoryState.Empty("No tracks found for \"$genreName\".")).asStateFlow()
}

// ── Folder ──

class StubFolderRepository : FolderRepository {
    override val libraryRoots: Flow<RepositoryState<List<LibraryFolderItem>>> =
        MutableStateFlow(RepositoryState.Empty("Import a music folder to get started.")).asStateFlow()

    override fun browseFolder(
        parentPath: String,
        sort: SortCriteria,
        filter: FilterCriteria.FolderFilter,
    ): Flow<RepositoryState<List<LibraryFolderItem>>> =
        MutableStateFlow(RepositoryState.Empty("Folder is empty.")).asStateFlow()
}

// ── Favorites ──

class DataStoreFavoritesRepository(
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: AppPreferencesRepository,
) : FavoritesRepository {
    override val favoriteTrackIds: Flow<Set<Long>> =
        preferencesRepository.favoriteTrackIds.distinctUntilChanged()

    override fun favoriteTracks(
        sort: SortCriteria,
        filter: FilterCriteria.FavoritesFilter,
    ): Flow<RepositoryState<List<LibraryTrackItem>>> = combine(
        libraryRepository.tracks,
        favoriteTrackIds,
    ) { tracks, favoriteIds ->
        val filtered = tracks
            .filter { track -> track.id in favoriteIds }
            .filter { track ->
                filter.query.isBlank() || listOfNotNull(track.title, track.artist)
                    .any { value -> value.contains(filter.query, ignoreCase = true) }
            }
            .filter { track ->
                filter.artistFilter == null || track.artist.equals(filter.artistFilter, ignoreCase = true)
            }
        val comparator = when (sort.field) {
            LibrarySortField.Artist ->
                compareBy<LibraryTrackItem> { it.artist.orEmpty().lowercase() }
                    .thenBy { it.title.lowercase() }
            LibrarySortField.Duration ->
                compareBy<LibraryTrackItem> { it.durationMs ?: Long.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            else -> compareBy { track: LibraryTrackItem -> track.title.lowercase() }
        }
        val sorted = filtered.sortedWith(
            if (sort.direction == SortDirection.Descending) comparator.reversed() else comparator,
        )
        if (sorted.isEmpty()) {
            RepositoryState.Empty("Favorite songs will appear here.")
        } else {
            RepositoryState.Loaded(sorted)
        }
    }.catch { error ->
        emit(RepositoryState.Error(error, "Failed to load favorite songs."))
    }

    override suspend fun isFavorite(trackId: Long): Boolean =
        preferencesRepository.favoriteTrackIds.map { trackId in it }.first()

    override suspend fun toggleFavorite(trackId: Long): Boolean =
        preferencesRepository.toggleFavoriteTrack(trackId)

    override val favoriteCount: Flow<Int> = favoriteTrackIds
        .map { favoriteIds -> favoriteIds.size }
        .distinctUntilChanged()
}

// ── History ──

class StubHistoryRepository : HistoryRepository {
    private val _history = MutableStateFlow<RepositoryState<List<LibraryTrackItem>>>(
        RepositoryState.Empty("Played tracks will appear here.")
    )

    override fun recentTracks(
        limit: Int,
        sort: SortCriteria,
        filter: FilterCriteria.HistoryFilter,
    ): Flow<RepositoryState<List<LibraryTrackItem>>> = _history.asStateFlow()

    override val historyCount: Flow<Int> = MutableStateFlow(0).asStateFlow()

    override suspend fun clearHistory() {}
}

// ── Lossless ──

class StubLosslessRepository : LosslessRepository {
    private val _lossless = MutableStateFlow<RepositoryState<List<LibraryTrackItem>>>(
        RepositoryState.Empty("Lossless tracks appear after scanning your library.")
    )
    private val _hiRes = MutableStateFlow<RepositoryState<List<LibraryTrackItem>>>(
        RepositoryState.Empty("Hi-Res tracks appear after scanning your library.")
    )

    override fun losslessTracks(
        sort: SortCriteria,
        filter: FilterCriteria.LosslessFilter,
    ): Flow<RepositoryState<List<LibraryTrackItem>>> = _lossless.asStateFlow()

    override val losslessCount: Flow<Int> = MutableStateFlow(0).asStateFlow()

    override fun hiResTracks(
        sort: SortCriteria,
        filter: FilterCriteria.LosslessFilter,
    ): Flow<RepositoryState<List<LibraryTrackItem>>> = _hiRes.asStateFlow()

    override val hiResCount: Flow<Int> = MutableStateFlow(0).asStateFlow()
}

// ── Downloads ──

class StubDownloadCollectionRepository : DownloadCollectionRepository {
    private val _downloads = MutableStateFlow<RepositoryState<List<LibraryTrackItem>>>(
        RepositoryState.Empty("Downloaded songs will appear here.")
    )

    override fun downloadedTracks(
        sort: SortCriteria,
        filter: FilterCriteria.DownloadsFilter,
    ): Flow<RepositoryState<List<LibraryTrackItem>>> = _downloads.asStateFlow()

    override val downloadCount: Flow<Int> = MutableStateFlow(0).asStateFlow()

    override val totalDownloadedBytes: Flow<Long> = MutableStateFlow(0L).asStateFlow()

    override suspend fun removeDownload(trackId: Long) {}
}
