package io.github.julystar.musicapp.plugin.management

import androidx.compose.ui.unit.Dp
import io.github.julystar.musicapp.core.presentation.components.DesignDialogDefaults

/**
 * Compatibility policy required by the current plugin configuration tests.
 * The restored plugin settings screen keeps its original UI and layout.
 */
internal fun isCompactPluginConfigurationDialog(windowWidth: Dp): Boolean =
    DesignDialogDefaults.isCompactWindow(windowWidth)
