package io.github.julystar.musicapp.car.presentation.layout

enum class CarLayoutProfile {
    Expanded,
    VehiclePanel,
}

enum class CarLayoutProfileHint {
    Automatic,
    Expanded,
    VehiclePanel,
}

fun interface CarLayoutProfileStrategy {
    fun select(
        usableSize: androidx.compose.ui.unit.DpSize,
        insets: CarLayoutInsets,
    ): CarLayoutProfile
}

/** Pure mapping of the two OEM screen-state values observed in FileManager. */
fun carLayoutHintFromOemState(
    keyScreenShow: Int?,
    keyVpaCuiShowLeft: Int?,
): CarLayoutProfileHint {
    if (keyScreenShow == null && keyVpaCuiShowLeft == null) {
        return CarLayoutProfileHint.Automatic
    }
    if (keyScreenShow != null && keyScreenShow != 0) {
        return CarLayoutProfileHint.VehiclePanel
    }
    return if (keyVpaCuiShowLeft == 1) {
        CarLayoutProfileHint.VehiclePanel
    } else {
        CarLayoutProfileHint.Expanded
    }
}
