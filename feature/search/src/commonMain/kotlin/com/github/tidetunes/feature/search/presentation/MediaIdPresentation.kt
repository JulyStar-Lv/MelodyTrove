package com.github.tidetunes.feature.search.presentation

import com.github.tidetunes.core.domain.model.MediaId

/** Stable key for source-aware search result rows. */
internal val MediaId.value: String
    get() = "${sourceId.value}:${mediaType}:$remoteId"
