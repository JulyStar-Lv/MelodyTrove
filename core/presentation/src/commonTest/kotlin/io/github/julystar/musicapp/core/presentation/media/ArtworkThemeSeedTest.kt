package io.github.julystar.musicapp.core.presentation.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import io.github.julystar.musicapp.core.domain.model.Artwork
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.math.abs

class ArtworkThemeSeedTest {

    @Test
    fun `extractor returns the representative saturated color`() {
        val bitmap = SolidImageBitmap(0xFFCC2040.toInt())

        val result = extractArtworkThemeSeedArgb(bitmap) ?: error("Expected an extracted seed")
        assertTrue(abs((result shr 16 and 0xFF) - 0xCC) <= 1)
        assertTrue(abs((result shr 8 and 0xFF) - 0x20) <= 1)
        assertTrue(abs((result and 0xFF) - 0x40) <= 1)
    }

    @Test
    fun `extractor rejects artwork without an eligible color`() {
        val bitmap = SolidImageBitmap(0xFF000000.toInt())

        assertNull(extractArtworkThemeSeedArgb(bitmap))
    }

    @Test
    fun `cache refreshes entries and evicts the least recently inserted artwork`() {
        val cache = ArtworkThemeSeedCache(maxEntries = 2)
        val first = Artwork.LibraryTrack(1)
        val second = Artwork.LibraryTrack(2)
        val third = Artwork.LibraryTrack(3)

        cache.put(first, 0xFFFF5B8AL)
        cache.put(second, 0xFF3D9AFFL)
        cache.put(first, 0xFF7A6CFFL)
        cache.put(third, 0xFFFFD93DL)

        assertEquals(0xFF7A6CFFL, cache.get(first))
        assertNull(cache.get(second))
        assertEquals(0xFFFFD93DL, cache.get(third))
    }

    private class SolidImageBitmap(
        private val argb: Int,
    ) : ImageBitmap {
        override val width: Int = 8
        override val height: Int = 8
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val hasAlpha: Boolean = true
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) {
            repeat(height) { y ->
                repeat(width) { x ->
                    buffer[bufferOffset + y * stride + x] = argb
                }
            }
        }

        override fun prepareToDraw() = Unit
    }
}
