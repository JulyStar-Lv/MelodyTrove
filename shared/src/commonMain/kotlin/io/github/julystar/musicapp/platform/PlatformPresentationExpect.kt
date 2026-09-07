package io.github.julystar.musicapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

expect fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap?

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
