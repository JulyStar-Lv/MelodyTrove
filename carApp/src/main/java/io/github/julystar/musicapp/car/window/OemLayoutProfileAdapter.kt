package io.github.julystar.musicapp.car.window

import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileHint

internal fun carLayoutHintFromOemState(
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
