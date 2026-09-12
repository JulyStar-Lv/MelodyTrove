package io.github.julystar.musicapp.car.presentation.screen

import io.github.julystar.musicapp.core.domain.model.DomainTrackBrowserItem
import io.github.julystar.musicapp.core.domain.model.FilterCriteria
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.model.SortCriteria
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.TrackBrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CarHomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsRealRecentSectionsAndFavoriteFlow() = runTest(dispatcher) {
        val browser = FakeTrackBrowserRepository(
            added = listOf(browserTrack(1, "Added")),
            played = listOf(browserTrack(2, "Played")),
        )
        val favoriteState = MutableStateFlow<RepositoryState<List<LibraryTrackItem>>>(
            RepositoryState.Loaded(listOf(libraryTrack(3, "Favorite"))),
        )
        val viewModel = CarHomeViewModel(browser, FakeFavoritesRepository(favoriteState))

        advanceUntilIdle()

        assertEquals(listOf(1L), assertIs<RepositoryState.Loaded<List<LibraryTrackItem>>>(viewModel.state.value.recentlyAdded).data.map { it.id })
        assertEquals(listOf(2L), assertIs<RepositoryState.Loaded<List<LibraryTrackItem>>>(viewModel.state.value.recentlyPlayed).data.map { it.id })
        assertEquals(listOf(3L), assertIs<RepositoryState.Loaded<List<LibraryTrackItem>>>(viewModel.state.value.favorites).data.map { it.id })

        favoriteState.value = RepositoryState.Empty("No favorites")
        advanceUntilIdle()
        assertIs<RepositoryState.Empty>(viewModel.state.value.favorites)
    }

    @Test
    fun exposesIndependentRecentSectionErrors() = runTest(dispatcher) {
        val browser = FakeTrackBrowserRepository(
            added = emptyList(),
            played = listOf(browserTrack(7, "Played")),
            addedFailure = IllegalStateException("database unavailable"),
        )
        val viewModel = CarHomeViewModel(
            browser,
            FakeFavoritesRepository(MutableStateFlow(RepositoryState.Empty())),
        )

        advanceUntilIdle()

        val added = assertIs<RepositoryState.Error>(viewModel.state.value.recentlyAdded)
        assertEquals("最近添加载入失败", added.message)
        assertEquals("database unavailable", added.exception.message)
        assertEquals(listOf(7L), assertIs<RepositoryState.Loaded<List<LibraryTrackItem>>>(viewModel.state.value.recentlyPlayed).data.map { it.id })
    }

    @Test
    fun refreshReplacesStaleRecentContent() = runTest(dispatcher) {
        val browser = FakeTrackBrowserRepository(
            added = listOf(browserTrack(1, "Old")),
            played = emptyList(),
        )
        val viewModel = CarHomeViewModel(
            browser,
            FakeFavoritesRepository(MutableStateFlow(RepositoryState.Empty())),
        )
        advanceUntilIdle()

        browser.added = listOf(browserTrack(9, "New"))
        viewModel.refresh()
        advanceUntilIdle()

        val current = assertIs<RepositoryState.Loaded<List<LibraryTrackItem>>>(viewModel.state.value.recentlyAdded)
        assertEquals(listOf(9L), current.data.map { it.id })
        assertIs<RepositoryState.Empty>(viewModel.state.value.recentlyPlayed)
    }
}

private class FakeTrackBrowserRepository(
    var added: List<DomainTrackBrowserItem>,
    var played: List<DomainTrackBrowserItem>,
    private val addedFailure: Throwable? = null,
) : TrackBrowserRepository {
    override suspend fun findTracksByGenre(genre: String, limit: Int) = emptyList<DomainTrackBrowserItem>()

    override suspend fun findRecentlyAdded(limit: Int): List<DomainTrackBrowserItem> {
        addedFailure?.let { throw it }
        return added.take(limit)
    }

    override suspend fun findRecentlyPlayed(limit: Int) = played.take(limit)
}

private class FakeFavoritesRepository(
    private val state: MutableStateFlow<RepositoryState<List<LibraryTrackItem>>>,
) : FavoritesRepository {
    override val favoriteTrackIds: Flow<Set<Long>> = MutableStateFlow(emptySet())

    override fun favoriteTracks(
        sort: SortCriteria,
        filter: FilterCriteria.FavoritesFilter,
    ): Flow<RepositoryState<List<LibraryTrackItem>>> = state

    override suspend fun isFavorite(trackId: Long) = false

    override suspend fun toggleFavorite(trackId: Long) = false

    override val favoriteCount: Flow<Int> = MutableStateFlow(0)
}

private fun browserTrack(id: Long, title: String) = DomainTrackBrowserItem(
    id = id,
    title = title,
    artist = "Artist",
    albumName = "Album",
    durationMs = 120_000,
    mediaId = null,
    canDownload = false,
)

private fun libraryTrack(id: Long, title: String) = LibraryTrackItem(
    id = id,
    title = title,
    artist = "Artist",
    durationMs = 120_000,
    mediaId = null,
)
