package io.github.julystar.musicapp.feature.search.presentation

import io.github.julystar.musicapp.feature.search.domain.SearchTrackItem

/**
 * Compatibility key retained by the restored design screen.
 * Keeps the original UI unchanged while using the current search model.
 */
internal fun SearchTrackItem.lazyListKey(index: Int): String {
    val mediaKey = mediaId?.let { "${it.sourceId.value}:${it.remoteId}" }
        ?: "local:${id ?: index}"
    return "search-track-$index-$mediaKey"
}
