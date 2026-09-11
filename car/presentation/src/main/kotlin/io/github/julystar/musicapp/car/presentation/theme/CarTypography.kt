package io.github.julystar.musicapp.car.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics

@Immutable
data class CarTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val pageTitle: TextStyle,
    val titleLarge: TextStyle,
    val title: TextStyle,
    val navigation: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val supporting: TextStyle,
)

val DefaultCarTypography = CarTypography(
    display = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold),
    headline = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold),
    pageTitle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
    title = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    navigation = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 26.sp),
    body = TextStyle(fontSize = 20.sp),
    label = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    supporting = TextStyle(fontSize = 16.sp),
)

internal fun carTypography(metrics: CarLayoutMetrics?): CarTypography {
    val height = metrics?.takeIf { it.metricsAvailable }?.contentSize?.height?.value ?: return DefaultCarTypography
    fun size(reference: Float) = (height * reference / 1080f).coerceAtLeast(12f).sp
    return CarTypography(
        display = TextStyle(fontSize = size(64f), fontWeight = FontWeight.Bold),
        headline = TextStyle(fontSize = size(56f), fontWeight = FontWeight.Bold),
        pageTitle = TextStyle(fontSize = size(40f), fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontSize = size(34f), fontWeight = FontWeight.SemiBold),
        title = TextStyle(fontSize = size(28f), fontWeight = FontWeight.SemiBold),
        navigation = TextStyle(fontSize = size(36f), lineHeight = size(42f), fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = size(26f)),
        body = TextStyle(fontSize = size(20f)),
        label = TextStyle(fontSize = size(18f), fontWeight = FontWeight.SemiBold),
        supporting = TextStyle(fontSize = size(16f)),
    )
}
