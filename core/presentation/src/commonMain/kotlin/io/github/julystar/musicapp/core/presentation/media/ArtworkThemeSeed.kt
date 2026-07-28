package io.github.julystar.musicapp.core.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.presentation.theme.ArtworkThemeSeedStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Immutable
data class ArtworkThemeSeedResult(
    val status: ArtworkThemeSeedStatus,
    val argb: Long? = null,
)

internal class ArtworkThemeSeedCache(
    private val maxEntries: Int = 48,
) {
    private val values = LinkedHashMap<Artwork, Long>()

    fun get(artwork: Artwork): Long? = values[artwork]

    fun put(artwork: Artwork, argb: Long) {
        values.remove(artwork)
        values[artwork] = argb
        while (values.size > maxEntries) {
            values.remove(values.keys.first())
        }
    }
}

private val artworkThemeSeedCache = ArtworkThemeSeedCache()

@Composable
fun rememberArtworkThemeSeed(
    artwork: Artwork?,
    enabled: Boolean,
    loader: ArtworkImageLoader = koinInject(),
): ArtworkThemeSeedResult {
    var result by remember {
        mutableStateOf(
            when {
                !enabled -> ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Disabled)
                artwork == null -> ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Missing)
                else -> ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Loading)
            },
        )
    }

    LaunchedEffect(artwork, enabled) {
        when {
            !enabled -> result = ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Disabled)
            artwork == null -> result = ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Missing)
            else -> {
                val cached = artworkThemeSeedCache.get(artwork)
                if (cached != null) {
                    result = ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Available, cached)
                    return@LaunchedEffect
                }
                result = ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Loading)
                val seed = try {
                    val bitmap = withContext(Dispatchers.Default) {
                        loader.cachedBitmap(artwork) ?: loader.loadBitmap(artwork)
                    }
                    bitmap?.let {
                        withContext(Dispatchers.Default) { extractArtworkThemeSeedArgb(it) }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    null
                }
                result = if (seed == null) {
                    ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Failed)
                } else {
                    artworkThemeSeedCache.put(artwork, seed)
                    ArtworkThemeSeedResult(ArtworkThemeSeedStatus.Available, seed)
                }
            }
        }
    }

    return result
}

internal fun extractArtworkThemeSeedArgb(
    bitmap: ImageBitmap,
    sampleSize: Int = 24,
): Long? {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null

    data class Bucket(
        var weight: Double = 0.0,
        var red: Double = 0.0,
        var green: Double = 0.0,
        var blue: Double = 0.0,
    )

    val buckets = Array(24) { Bucket() }
    val widthSamples = min(sampleSize, bitmap.width)
    val heightSamples = min(sampleSize, bitmap.height)
    val pixel = IntArray(1)

    for (sampleY in 0 until heightSamples) {
        val y = if (heightSamples == 1) 0 else sampleY * (bitmap.height - 1) / (heightSamples - 1)
        for (sampleX in 0 until widthSamples) {
            val x = if (widthSamples == 1) 0 else sampleX * (bitmap.width - 1) / (widthSamples - 1)
            bitmap.readPixels(pixel, x, y, 1, 1, 0, 1)
            val argb = pixel[0]
            val red = (argb shr 16 and 0xFF) / 255.0
            val green = (argb shr 8 and 0xFF) / 255.0
            val blue = (argb and 0xFF) / 255.0
            val maximum = max(red, max(green, blue))
            val minimum = min(red, min(green, blue))
            val delta = maximum - minimum
            val saturation = if (maximum == 0.0) 0.0 else delta / maximum
            if (maximum < 0.12 || maximum > 0.96 || saturation < 0.12) continue

            val hue = when {
                delta == 0.0 -> 0.0
                maximum == red -> 60.0 * (((green - blue) / delta) % 6.0)
                maximum == green -> 60.0 * ((blue - red) / delta + 2.0)
                else -> 60.0 * ((red - green) / delta + 4.0)
            }.let { if (it < 0.0) it + 360.0 else it }
            val bucket = buckets[(hue / 15.0).toInt().coerceIn(0, buckets.lastIndex)]
            val midValuePreference = 1.0 - abs(maximum - 0.65)
            val weight = saturation * saturation * midValuePreference.coerceAtLeast(0.25)
            bucket.weight += weight
            bucket.red += red * weight
            bucket.green += green * weight
            bucket.blue += blue * weight
        }
    }

    val winner = buckets.maxByOrNull(Bucket::weight)?.takeIf { it.weight > 0.0 } ?: return null
    val red = (winner.red / winner.weight * 255.0).toInt().coerceIn(0, 255)
    val green = (winner.green / winner.weight * 255.0).toInt().coerceIn(0, 255)
    val blue = (winner.blue / winner.weight * 255.0).toInt().coerceIn(0, 255)
    return 0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
}
