package com.github.tidetunes.core.domain.model

/**
 * Common sort field choices across library categories.
 * Individual repositories may support only a subset.
 */
enum class LibrarySortField(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Album("Album"),
    Duration("Duration"),
    DateAdded("Date Added"),
    LastPlayed("Last Played"),
    Year("Year"),
    TrackCount("Track Count"),
    FileName("File Name"),
    FilePath("File Path"),
    FileSize("File Size"),
}

enum class SortDirection(val label: String) {
    Ascending("Ascending"),
    Descending("Descending"),
}

data class SortCriteria(
    val field: LibrarySortField = LibrarySortField.Title,
    val direction: SortDirection = SortDirection.Ascending,
) {
    companion object {
        val Default = SortCriteria()
    }
}

/**
 * Per-category filter parameters.
 * Each category repository exposes a [FilterCriteria] subclass that captures
 * the filtering inputs relevant to that category.
 */
sealed interface FilterCriteria {

    /** Genre list: search / sort. */
    data class GenreFilter(
        val query: String = "",
        val minTrackCount: Int? = null,
    ) : FilterCriteria

    /** Folder browser: path prefix + name query. */
    data class FolderFilter(
        val pathPrefix: String = "",
        val nameQuery: String = "",
    ) : FilterCriteria

    /** Favorites: query + artist filter. */
    data class FavoritesFilter(
        val query: String = "",
        val artistFilter: String? = null,
    ) : FilterCriteria

    /** Play history: date range + query. */
    data class HistoryFilter(
        val query: String = "",
        val fromDateMs: Long? = null,
        val toDateMs: Long? = null,
    ) : FilterCriteria

    /** Lossless: format + bitrate range + query. */
    data class LosslessFilter(
        val query: String = "",
        val format: String? = null,
        val minSampleRate: Int? = null,
        val minBitRate: Int? = null,
    ) : FilterCriteria

    /** Downloads: status + query. */
    data class DownloadsFilter(
        val query: String = "",
        val onlyCompleted: Boolean = false,
        val onlyFailed: Boolean = false,
    ) : FilterCriteria
}
