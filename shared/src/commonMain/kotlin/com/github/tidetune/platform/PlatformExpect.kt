package com.github.tidetune.platform

import androidx.compose.runtime.Composable

expect fun getAppDocumentDir(): String
expect fun getAppCacheDir(): String
expect fun currentTimeMillis(): Long
expect fun byteArrayToImageBitmap(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap?

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
