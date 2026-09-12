package io.github.julystar.musicapp.car.presentation.layout

enum class CarLayoutProfile {
    Expanded,
    VehiclePanel,
    FullscreenCockpit,
}

enum class CarLayoutProfileHint {
    Automatic,
    Expanded,
    VehiclePanel,
    FullscreenCockpit,
}

fun interface CarLayoutProfileStrategy {
    fun select(
        usableSize: androidx.compose.ui.unit.DpSize,
        insets: CarLayoutInsets,
    ): CarLayoutProfile
}
