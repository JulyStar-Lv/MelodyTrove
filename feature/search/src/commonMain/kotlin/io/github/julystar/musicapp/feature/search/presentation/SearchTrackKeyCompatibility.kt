package io.github.julystar.musicapp.feature.search.presentation

import io.github.julystar.musicapp.feature.search.domain.SearchTrackItem

/**
 * Compatibility key used by the restored design screen.
 * Keeps the same identity rule as the current search results implementation.
 */
internal fun SearchTrackItem.lazyListKey(index: Int): String {
    val mediaKey = mediaId?.let { "${it.sourceId.value}:${it.remoteId}" }
        ?: "local:${id ?: index}"
    return "search-track-$index-$mediaKey"
}
