package io.github.julystar.musicapp.car.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import io.github.julystar.musicapp.car.presentation.focus.CarFocusId
import io.github.julystar.musicapp.car.presentation.focus.CarFocusRegistry
import io.github.julystar.musicapp.car.presentation.focus.carFocusId
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTouchTargets
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography

/** Static shell seam; route content and focus traversal are supplied in later phases. */
@Composable
fun CarNavigationRoot(
    metrics: CarLayoutMetrics?,
    focusRegistry: CarFocusRegistry = CarFocusRegistry(),
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val typography = LocalCarTypography.current
    val spacing = LocalCarSpacing.current
    val touchTargets = LocalCarTouchTargets.current
    val safeMetrics = metrics?.takeIf { it.metricsAvailable }
    val titleId = focusRegistry.restore("root") ?: CarFocusId("shell.title")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(min = touchTargets.minimum)
            .heightIn(min = touchTargets.minimum)
            .padding(bottom = safeMetrics?.libraryGap ?: spacing.section),
    ) {
        BasicText(
            text = "Tide Player",
            style = typography.title.copy(color = colors.textPrimary),
            modifier = Modifier.semantics { carFocusId = titleId.value },
        )
    }
}
