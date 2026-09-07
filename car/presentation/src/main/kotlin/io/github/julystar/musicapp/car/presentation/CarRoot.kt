package io.github.julystar.musicapp.car.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.navigation.CarNavigationRoot
import io.github.julystar.musicapp.car.presentation.theme.CarTheme
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors

/** Placeholder shell for the independent Automotive presentation boundary. */
@Composable
fun CarRoot(
    metrics: CarLayoutMetrics? = null,
    modifier: Modifier = Modifier,
) {
    CarTheme(layoutMetrics = metrics) {
        val colors = LocalCarColors.current
        val safeMetrics = metrics?.takeIf { it.metricsAvailable }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.backgroundBase),
        ) {
            CarNavigationRoot(
                metrics = metrics,
                modifier = Modifier.padding(
                    horizontal = safeMetrics?.shellHorizontalPadding ?: 0.dp,
                    vertical = safeMetrics?.shellVerticalPadding ?: 0.dp,
                ),
            )
        }
    }
}
