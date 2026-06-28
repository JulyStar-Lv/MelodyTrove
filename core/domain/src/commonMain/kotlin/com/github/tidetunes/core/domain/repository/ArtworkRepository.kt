package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.ArtworkCacheKey

interface ArtworkRepository {
    fun cached(artwork: Artwork): ByteArray?

    suspend fun cacheKey(artwork: Artwork): ArtworkCacheKey?

    suspend fun load(artwork: Artwork): ByteArray?
}
