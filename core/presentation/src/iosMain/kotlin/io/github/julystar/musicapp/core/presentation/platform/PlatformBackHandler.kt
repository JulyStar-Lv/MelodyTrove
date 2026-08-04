package io.github.julystar.musicapp.core.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

private data class IosBackHandlerEntry(
    val token: Any,
    val onBack: () -> Unit,
)

private val iosBackHandlers = mutableListOf<IosBackHandlerEntry>()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    DisposableEffect(enabled, onBack) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }
        val token = Any()
        iosBackHandlers += IosBackHandlerEntry(token, onBack)
        onDispose {
            iosBackHandlers.removeAll { it.token === token }
        }
    }
}

fun dispatchPlatformBack(): Boolean {
    val handler = iosBackHandlers.lastOrNull()?.onBack ?: return false
    handler()
    return true
}
