package io.github.julystar.musicapp.plugin.management

import androidx.compose.runtime.Composable

/**
 * Production navigation entry. The one-argument overload is preferred over the
 * legacy implementation whose manager parameter has a default value.
 */
@Composable
fun PluginSettingsRoot(onBack: () -> Unit) {
    LocalizedPluginSettingsRoot(onBack = onBack)
}
