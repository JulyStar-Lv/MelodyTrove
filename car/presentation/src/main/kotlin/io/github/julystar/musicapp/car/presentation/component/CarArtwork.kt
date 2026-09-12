package io.github.julystar.musicapp.car.presentation.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

@Composable
fun CarArtwork(
    artwork: Artwork?,
    repository: ArtworkRepository,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    fillBounds: Boolean = false,
) {
    var bitmap by remember(artwork) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(artwork) {
        bitmap = try {
            artwork?.let { target ->
                withContext(Dispatchers.IO) {
                    val bytes = repository.cached(target) ?: repository.load(target)
                    bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    val colors = LocalCarColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (fillBounds) Modifier.fillMaxSize() else Modifier.size(size))
            .clip(shape)
            .background(colors.surfaceContainerHighest),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: BasicText(
            text = "♪",
            style = LocalCarTypography.current.title.copy(color = colors.textSummary),
        )
    }
}
