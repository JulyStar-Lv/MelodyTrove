package io.github.julystar.musicapp.feature.library.presentation

import androidx.compose.runtime.Composable
import musicapp.feature.library.generated.resources.Res
import musicapp.feature.library.generated.resources.library_action_browse_songs
import musicapp.feature.library.generated.resources.library_action_clear_search
import musicapp.feature.library.generated.resources.library_action_import_folder
import musicapp.feature.library.generated.resources.library_action_new
import musicapp.feature.library.generated.resources.library_action_play_all
import musicapp.feature.library.generated.resources.library_action_shuffle
import musicapp.feature.library.generated.resources.library_add_favorite
import musicapp.feature.library.generated.resources.library_album_count
import musicapp.feature.library.generated.resources.library_artist_count
import musicapp.feature.library.generated.resources.library_artist_fallback
import musicapp.feature.library.generated.resources.library_available_offline
import musicapp.feature.library.generated.resources.library_category_albums
import musicapp.feature.library.generated.resources.library_category_artists
import musicapp.feature.library.generated.resources.library_category_downloads
import musicapp.feature.library.generated.resources.library_category_favorites
import musicapp.feature.library.generated.resources.library_category_folders
import musicapp.feature.library.generated.resources.library_category_genres
import musicapp.feature.library.generated.resources.library_category_hires
import musicapp.feature.library.generated.resources.library_category_history
import musicapp.feature.library.generated.resources.library_category_lossless
import musicapp.feature.library.generated.resources.library_category_playlists
import musicapp.feature.library.generated.resources.library_category_recently_added
import musicapp.feature.library.generated.resources.library_category_recently_played
import musicapp.feature.library.generated.resources.library_category_songs
import musicapp.feature.library.generated.resources.library_category_sources
import musicapp.feature.library.generated.resources.library_duration_hours
import musicapp.feature.library.generated.resources.library_duration_minutes
import musicapp.feature.library.generated.resources.library_empty_collection_message
import musicapp.feature.library.generated.resources.library_empty_favorites
import musicapp.feature.library.generated.resources.library_empty_hires_after_scan
import musicapp.feature.library.generated.resources.library_empty_lossless_after_scan
import musicapp.feature.library.generated.resources.library_filter_sort_description
import musicapp.feature.library.generated.resources.library_genre_count
import musicapp.feature.library.generated.resources.library_import_folder_message
import musicapp.feature.library.generated.resources.library_import_music_folder
import musicapp.feature.library.generated.resources.library_liked_songs
import musicapp.feature.library.generated.resources.library_metadata_playlist_count
import musicapp.feature.library.generated.resources.library_metadata_song_count
import musicapp.feature.library.generated.resources.library_more_actions
import musicapp.feature.library.generated.resources.library_my_favorites
import musicapp.feature.library.generated.resources.library_no_category
import musicapp.feature.library.generated.resources.library_no_collection_data
import musicapp.feature.library.generated.resources.library_no_downloads_yet
import musicapp.feature.library.generated.resources.library_no_folders_added
import musicapp.feature.library.generated.resources.library_no_matches
import musicapp.feature.library.generated.resources.library_no_tracks
import musicapp.feature.library.generated.resources.library_offline_message
import musicapp.feature.library.generated.resources.library_pin_playlist
import musicapp.feature.library.generated.resources.library_remove_favorite
import musicapp.feature.library.generated.resources.library_search_category_hint
import musicapp.feature.library.generated.resources.library_search_hint
import musicapp.feature.library.generated.resources.library_sidebar_collection
import musicapp.feature.library.generated.resources.library_sidebar_more
import musicapp.feature.library.generated.resources.library_sidebar_storage
import musicapp.feature.library.generated.resources.library_sort_album
import musicapp.feature.library.generated.resources.library_sort_title
import musicapp.feature.library.generated.resources.library_sources_message
import musicapp.feature.library.generated.resources.library_sources_title
import musicapp.feature.library.generated.resources.library_title
import musicapp.feature.library.generated.resources.library_track_count
import musicapp.feature.library.generated.resources.library_track_count_with_duration
import musicapp.feature.library.generated.resources.library_try_different_search
import musicapp.feature.library.generated.resources.library_unknown_artist
import musicapp.feature.library.generated.resources.library_unpin_playlist
import org.jetbrains.compose.resources.stringResource

private val LibraryHoursMinutesPattern = Regex("""^(\d+)h (\d+)m$""")
private val LibraryMinutesPattern = Regex("""^(\d+) min$""")
private val LibrarySongDurationPattern = Regex("""^(\d+) songs · (.+)$""")
private val LibraryTrackDurationPattern = Regex("""^(\d+) tracks? · (.+)$""")
private val LibraryPlaylistMetadataPattern = Regex("""^(\d+) playlists · Long press to edit$""")
private val LibraryAlbumCountPattern = Regex("""^(\d+) albums$""")
private val LibraryArtistCountPattern = Regex("""^(\d+) artists$""")
private val LibraryGenreCountPattern = Regex("""^(\d+) genres$""")
private val LibraryTrackCountPattern = Regex("""^(\d+) tracks$""")
private val LibraryFilterDescriptionPattern = Regex("""^Filter songs, sorted by (.+)$""")
private val LibraryMoreActionsPattern = Regex("""^More actions for (.+)$""")

@Composable
internal fun localizedLibraryText(value: String): String {
    localizedLibraryCategory(value)?.let { return it }

    when (value) {
        "Library" -> return stringResource(Res.string.library_title)
        "Collection" -> return stringResource(Res.string.library_sidebar_collection)
        "Storage" -> return stringResource(Res.string.library_sidebar_storage)
        "More" -> return stringResource(Res.string.library_sidebar_more)
        "Shuffle" -> return stringResource(Res.string.library_action_shuffle)
        "Play all" -> return stringResource(Res.string.library_action_play_all)
        "New" -> return stringResource(Res.string.library_action_new)
        "Search songs, artists, or albums" -> return stringResource(Res.string.library_search_hint)
        "No matches" -> return stringResource(Res.string.library_no_matches)
        "No tracks" -> return stringResource(Res.string.library_no_tracks)
        "Try a different search." -> return stringResource(Res.string.library_try_different_search)
        "Clear search" -> return stringResource(Res.string.library_action_clear_search)
        "No folders added" -> return stringResource(Res.string.library_no_folders_added)
        "Import a folder to add its music to your library." -> return stringResource(Res.string.library_import_folder_message)
        "Import folder" -> return stringResource(Res.string.library_action_import_folder)
        "No downloads yet" -> return stringResource(Res.string.library_no_downloads_yet)
        "Keep music available when you are offline." -> return stringResource(Res.string.library_offline_message)
        "Browse songs" -> return stringResource(Res.string.library_action_browse_songs)
        "One library, every source" -> return stringResource(Res.string.library_sources_title)
        "Manage Local and WebDAV sources from Settings." -> return stringResource(Res.string.library_sources_message)
        "Unknown Artist" -> return stringResource(Res.string.library_unknown_artist)
        "Artist" -> return stringResource(Res.string.library_artist_fallback)
        "Remove from favorites" -> return stringResource(Res.string.library_remove_favorite)
        "Add to favorites" -> return stringResource(Res.string.library_add_favorite)
        "Unpin playlist" -> return stringResource(Res.string.library_unpin_playlist)
        "Pin playlist" -> return stringResource(Res.string.library_pin_playlist)
        "Import a music folder" -> return stringResource(Res.string.library_import_music_folder)
        "Available offline" -> return stringResource(Res.string.library_available_offline)
        "No collection data available" -> return stringResource(Res.string.library_no_collection_data)
        "My Favorites" -> return stringResource(Res.string.library_my_favorites)
        "Your liked songs" -> return stringResource(Res.string.library_liked_songs)
        "No music is available in this collection yet." -> return stringResource(Res.string.library_empty_collection_message)
        "Favorite songs will appear here." -> return stringResource(Res.string.library_empty_favorites)
        "Lossless tracks appear after scan." -> return stringResource(Res.string.library_empty_lossless_after_scan)
        "Hi-Res tracks appear after scan." -> return stringResource(Res.string.library_empty_hires_after_scan)
        "Title" -> return stringResource(Res.string.library_sort_title)
        "Album" -> return stringResource(Res.string.library_sort_album)
    }

    if (value.startsWith("Search ")) {
        localizedLibraryCategory(value.removePrefix("Search "))?.let { category ->
            return stringResource(Res.string.library_search_category_hint, category)
        }
    }

    if (value.startsWith("No ")) {
        localizedLibraryCategory(value.removePrefix("No "))?.let { category ->
            return stringResource(Res.string.library_no_category, category)
        }
    }

    LibraryHoursMinutesPattern.matchEntire(value)?.let { match ->
        return stringResource(
            Res.string.library_duration_hours,
            match.groupValues[1].toLong(),
            match.groupValues[2].toLong(),
        )
    }
    LibraryMinutesPattern.matchEntire(value)?.let { match ->
        return stringResource(Res.string.library_duration_minutes, match.groupValues[1].toLong())
    }
    LibrarySongDurationPattern.matchEntire(value)?.let { match ->
        return stringResource(
            Res.string.library_metadata_song_count,
            match.groupValues[1].toLong(),
            localizedLibraryText(match.groupValues[2]),
        )
    }
    LibraryTrackDurationPattern.matchEntire(value)?.let { match ->
        return stringResource(
            Res.string.library_track_count_with_duration,
            match.groupValues[1].toLong(),
            localizedLibraryText(match.groupValues[2]),
        )
    }
    LibraryPlaylistMetadataPattern.matchEntire(value)?.let { match ->
        return stringResource(Res.string.library_metadata_playlist_count, match.groupValues[1].toLong())
    }
    LibraryAlbumCountPattern.matchEntire(value)?.let { match ->
        return stringResource(Res.string.library_album_count, match.groupValues[1].toLong())
    }
    LibraryArtistCountPattern.matchEntire(value)?.let { match ->
        return stringResource(Res.string.library_artist_count, match.groupValues[1].toLong())
    }
    LibraryGenreCountPattern.matchEntire(value)?.let { match ->
        return stringResource(Res.string.library_genre_count, match.groupValues[1].toLong())
    }
    LibraryTrackCountPattern.matchEntire(value)?.let { match ->
        return stringResource(Res.string.library_track_count, match.groupValues[1].toLong())
    }
    LibraryFilterDescriptionPattern.matchEntire(value)?.let { match ->
        return stringResource(
            Res.string.library_filter_sort_description,
            localizedLibraryText(match.groupValues[1]),
        )
    }
    LibraryMoreActionsPattern.matchEntire(value)?.let { match ->
        return stringResource(Res.string.library_more_actions, match.groupValues[1])
    }

    return value
}

@Composable
private fun localizedLibraryCategory(value: String): String? = when (value.lowercase()) {
    "playlists" -> stringResource(Res.string.library_category_playlists)
    "songs" -> stringResource(Res.string.library_category_songs)
    "albums" -> stringResource(Res.string.library_category_albums)
    "artists" -> stringResource(Res.string.library_category_artists)
    "genres" -> stringResource(Res.string.library_category_genres)
    "folders" -> stringResource(Res.string.library_category_folders)
    "favorites" -> stringResource(Res.string.library_category_favorites)
    "downloads" -> stringResource(Res.string.library_category_downloads)
    "history" -> stringResource(Res.string.library_category_history)
    "recently added" -> stringResource(Res.string.library_category_recently_added)
    "recently played" -> stringResource(Res.string.library_category_recently_played)
    "lossless" -> stringResource(Res.string.library_category_lossless)
    "hi-res" -> stringResource(Res.string.library_category_hires)
    "sources" -> stringResource(Res.string.library_category_sources)
    else -> null
}
