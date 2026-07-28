package io.github.julystar.musicapp.feature.search.presentation

import io.github.julystar.musicapp.core.domain.model.MediaId

/** Stable key for source-aware search result rows. */
internal val MediaId.value: String
    get() = "${sourceId.value}:${mediaType}:$remoteId"
