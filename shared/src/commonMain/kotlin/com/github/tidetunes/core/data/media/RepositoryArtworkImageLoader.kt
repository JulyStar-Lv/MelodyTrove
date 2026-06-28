package com.github.tidetunes.core.data.media

import androidx.compose.ui.graphics.ImageBitmap
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.presentation.media.ArtworkImageLoader
import com.github.tidetunes.core.domain.repository.ArtworkRepository
import com.github.tidetunes.platform.byteArrayToImageBitmap

class RepositoryArtworkImageLoader(
    private val artworkRepository: ArtworkRepository,
) : ArtworkImageLoader {
    private val bitmapCache = HashMap<Artwork, ImageBitmap>()

    override fun cachedBitmap(artwork: Artwork): ImageBitmap? {
        bitmapCache[artwork]?.let { return it }
        val bytes = artworkRepository.cached(artwork) ?: return null
        return bytes.toCachedBitmap(artwork)
    }

    override suspend fun loadBitmap(artwork: Artwork): ImageBitmap? {
        cachedBitmap(artwork)?.let { return it }
        val bytes = artworkRepository.load(artwork) ?: return null
        return bytes.toCachedBitmap(artwork)
    }

    private fun ByteArray.toCachedBitmap(artwork: Artwork): ImageBitmap? {
        val bitmap = byteArrayToImageBitmap(this) ?: return null
        bitmapCache[artwork] = bitmap
        return bitmap
    }
}
