package com.github.tidetunes.core.presentation.media

import androidx.compose.ui.graphics.ImageBitmap
import com.github.tidetunes.core.domain.model.Artwork

interface ArtworkImageLoader {
    fun cachedBitmap(artwork: Artwork): ImageBitmap?

    suspend fun loadBitmap(artwork: Artwork): ImageBitmap?
}
