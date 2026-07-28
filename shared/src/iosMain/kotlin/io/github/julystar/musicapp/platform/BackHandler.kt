package io.github.julystar.musicapp.platform

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no hardware/system back button. Navigation gestures are owned by UIKit.
}
