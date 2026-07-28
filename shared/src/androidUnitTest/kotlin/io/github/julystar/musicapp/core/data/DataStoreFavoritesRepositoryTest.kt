package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.data.datastore.AppPreferencesRepository
import io.github.julystar.musicapp.core.data.datastore.createAppDataStore
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataStoreFavoritesRepositoryTest {
    @Test
    fun favoriteToggleUpdatesIdsCountAndPlaylistTracks() = runBlocking {
        val file = File.createTempFile("musicapp-favorites-repository-", ".preferences_pb").apply {
            delete()
        }
        val libraryRepository = FakeLibraryRepository(
            listOf(
                LibraryTrackItem(1L, "First", "Artist", 1_000L),
                LibraryTrackItem(2L, "Second", "Artist", 2_000L),
            ),
        )

        try {
            val repository = DataStoreFavoritesRepository(
                libraryRepository = libraryRepository,
                preferencesRepository = AppPreferencesRepository(
                    createAppDataStore { file.absolutePath.toPath() },
                ),
            )

            assertTrue(repository.toggleFavorite(2L))
            assertTrue(repository.isFavorite(2L))
            assertEquals(setOf(2L), withTimeout(5_000) { repository.favoriteTrackIds.first() })
            assertEquals(1, withTimeout(5_000) { repository.favoriteCount.first() })
            val loaded = withTimeout(5_000) {
                repository.favoriteTracks().first { it is RepositoryState.Loaded }
            }
            assertEquals(listOf(2L), loaded.dataOrNull?.map(LibraryTrackItem::id))

            assertFalse(repository.toggleFavorite(2L))
            val empty = withTimeout(5_000) {
                repository.favoriteTracks().first { it is RepositoryState.Empty }
            }
            assertTrue(empty is RepositoryState.Empty)
        } finally {
            file.delete()
        }
    }
}

private class FakeLibraryRepository(
    tracks: List<LibraryTrackItem>,
) : LibraryRepository {
    override val initialLoadComplete = MutableStateFlow(true)
    override val tracks = MutableStateFlow(tracks)
    override val albums = MutableStateFlow<List<LibraryAlbumItem>>(emptyList())
    override val artists = MutableStateFlow<List<LibraryArtistItem>>(emptyList())
}
