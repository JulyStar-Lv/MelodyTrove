package com.github.tidetunes.core.domain.model

/**
 * Generic sealed hierarchy for repository data state.
 * Every library category repository exposes its data through this wrapper,
 * providing a uniform Loading / Error / Empty / Loaded semantic surface.
 */
sealed interface RepositoryState<out T> {

    /** Initial or in-progress fetch; the caller may show a spinner or skeleton. */
    data object Loading : RepositoryState<Nothing>

    /** Fetch completed with zero items; for example, a genre with no tracks. */
    data class Empty(val message: String? = null) : RepositoryState<Nothing>

    /** Fetch completed successfully with non-empty content. */
    data class Loaded<T>(val data: T) : RepositoryState<T>

    /** Fetch failed with a recoverable or displayable error. */
    data class Error(val exception: Throwable, val message: String? = null) : RepositoryState<Nothing>

    /** Convenience: true when the state is Loading. */
    val isLoading: Boolean get() = this is Loading

    /** Convenience: true when state has usable data. */
    val isLoaded: Boolean get() = this is Loaded

    /** Convenience: extract data or null for safe UI consumption. */
    val dataOrNull: T?
        get() = when (this) {
            is Loaded -> data
            else -> null
        }

    /** Convenience: extract error or null. */
    val errorOrNull: Throwable?
        get() = when (this) {
            is Error -> exception
            else -> null
        }

    companion object {
        /** Inline factory for [Loaded] */
        fun <T> loaded(data: T): RepositoryState<T> = Loaded(data)
        /** Inline factory for [Error] */
        fun error(exception: Throwable, message: String? = null): RepositoryState<Nothing> = Error(exception, message)
    }
}
